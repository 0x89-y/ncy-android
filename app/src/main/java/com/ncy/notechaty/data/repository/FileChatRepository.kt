package com.ncy.notechaty.data.repository

import android.content.Context
import com.ncy.notechaty.data.model.*
import com.ncy.notechaty.data.serialization.ChatSerializer
import com.ncy.notechaty.data.sync.ChatEventBus
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventCodec
import com.ncy.notechaty.data.sync.event.ChatEventOp
import com.ncy.notechaty.data.sync.event.EventLog
import com.ncy.notechaty.data.sync.event.EventLogMigrator
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.event.MaterializedView
import com.ncy.notechaty.security.ChatLockState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val chatLockState: ChatLockState,
    private val deviceIdentity: DeviceIdentity,
    private val eventBus: ChatEventBus
) : ChatRepository {

    private val serializer = ChatSerializer()
    private val eventCodec = ChatEventCodec()
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val rootDir: File
        get() = profileRepository.rootDir()

    private val profileDir: File
        get() {
            val id = profileRepository.activeProfileId.value
            val dir = if (id.isEmpty()) rootDir else File(rootDir, "profile_$id")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    init {
        // Initial load + react to profile switches.
        scope.launch { refreshChats() }
        scope.launch {
            profileRepository.activeProfileId
                .drop(1) // initial value already handled above
                .collect {
                    chatLockState.clearChatUnlocks()
                    _chats.value = emptyList()
                    refreshChats()
                }
        }
    }

    override fun getBaseDir(): File = profileDir

    override fun observeChats(): Flow<List<Chat>> = _chats.asStateFlow()

    override suspend fun getChat(chatId: String): Chat? = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext null
        val data = serializer.readFromFile(chatFile)
        data.toChat(chatId, data.messages)
    }

    override suspend fun createChat(name: String): Chat = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val chatDir = File(profileDir, "chat_$id")
        chatDir.mkdirs()
        File(chatDir, "images").mkdirs()
        File(chatDir, "voice").mkdirs()

        val metadata = ChatMetadata(
            id = id,
            name = name,
            createdAt = System.currentTimeMillis()
        )
        appendAndMaterialize(id, ChatEventOp.ChatMetadataChanged(metadata))

        val chat = Chat(id = id, name = name, createdAt = metadata.createdAt)
        refreshChats()
        chat
    }

    override suspend fun updateChat(chat: Chat): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_${chat.id}")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext

        val existing = serializer.readFromFile(chatFile)
        val updated = ChatMetadata(
            id = chat.id,
            name = chat.name,
            avatarFile = chat.avatarFile,
            backgroundFile = chat.backgroundFile,
            createdAt = existing.chat.createdAt,
            isPinned = chat.isPinned,
            bubbleColor = chat.bubbleColor,
            bubbleGradient = chat.bubbleGradient,
            isArchived = chat.isArchived,
            tags = chat.tags,
            isProtected = chat.isProtected,
            hideName = chat.hideName
        )
        appendAndMaterialize(chat.id, ChatEventOp.ChatMetadataChanged(updated))
        refreshChats()
    }

    override suspend fun deleteChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        // Phase 0: physical delete. Phase 2 will move to a graveyard so peers can learn the tombstone.
        val chatDir = File(profileDir, "chat_$chatId")
        chatDir.deleteRecursively()
        refreshChats()
    }

    override suspend fun getMessages(chatId: String): List<Message> = withContext(Dispatchers.IO) {
        val chatFile = File(File(profileDir, "chat_$chatId"), "chat.json")
        if (!chatFile.exists()) return@withContext emptyList()
        serializer.readFromFile(chatFile).messages
    }

    override suspend fun addMessage(message: Message): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_${message.chatId}")
        if (!chatDir.exists()) return@withContext
        appendAndMaterialize(message.chatId, ChatEventOp.MessageCreated(message))
        refreshChats()
    }

    override suspend fun updateMessage(message: Message): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_${message.chatId}")
        if (!chatDir.exists()) return@withContext
        appendAndMaterialize(message.chatId, ChatEventOp.MessageEdited(message))
        refreshChats()
    }

    override suspend fun deleteMessage(chatId: String, messageId: String): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        if (!chatDir.exists()) return@withContext
        appendAndMaterialize(chatId, ChatEventOp.MessageTombstoned(messageId))
        refreshChats()
    }

    override suspend fun setChatAvatar(chatId: String, sourceFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        val avatarFile = File(chatDir, "avatar.jpg")
        sourceFile.copyTo(avatarFile, overwrite = true)

        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext
        val existing = serializer.readFromFile(chatFile)
        appendAndMaterialize(
            chatId,
            ChatEventOp.ChatMetadataChanged(existing.chat.copy(avatarFile = "avatar.jpg"))
        )
        refreshChats()
    }

    override suspend fun setChatBackground(chatId: String, sourceFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        val bgFile = File(chatDir, "background.jpg")
        sourceFile.copyTo(bgFile, overwrite = true)

        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext
        val existing = serializer.readFromFile(chatFile)
        appendAndMaterialize(
            chatId,
            ChatEventOp.ChatMetadataChanged(existing.chat.copy(backgroundFile = "background.jpg"))
        )
        refreshChats()
    }

    override suspend fun mediaFileForHash(
        hash: String,
        mediaType: String
    ): File? = withContext(Dispatchers.IO) {
        val (subdir, ext) = mediaPathBits(mediaType) ?: return@withContext null
        val needle = "$hash.$ext"
        profileDir.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.mapNotNull { chatDir ->
                val candidate = File(File(chatDir, subdir), needle)
                if (candidate.isFile) candidate else null
            }
            ?.firstOrNull()
    }

    override suspend fun saveMediaBlob(
        chatId: String,
        hash: String,
        mediaType: String,
        bytes: ByteArray
    ): Unit = withContext(Dispatchers.IO) {
        val (subdir, ext) = mediaPathBits(mediaType) ?: return@withContext
        val dir = File(File(profileDir, "chat_$chatId"), subdir).apply { mkdirs() }
        val target = File(dir, "$hash.$ext")
        if (target.isFile && target.length() == bytes.size.toLong()) return@withContext
        val tmp = File(dir, ".incoming_${UUID.randomUUID()}.$ext")
        tmp.writeBytes(bytes)
        if (target.exists()) tmp.delete()
        else if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = false)
            tmp.delete()
        }
    }

    private fun mediaPathBits(mediaType: String): Pair<String, String>? = when (mediaType) {
        "voice" -> "voice" to "m4a"
        "image" -> "images" to "jpg"
        else -> null
    }

    override suspend fun getChatDir(chatId: String): File = File(profileDir, "chat_$chatId")

    override suspend fun getImagesDir(chatId: String): File {
        val dir = File(File(profileDir, "chat_$chatId"), "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override suspend fun getVoiceDir(chatId: String): File {
        val dir = File(File(profileDir, "chat_$chatId"), "voice")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override suspend fun exportChat(chatId: String, outputFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        if (!chatDir.exists()) return@withContext

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            chatDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(chatDir).path.replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    override suspend fun exportAllChats(outputFile: File): Unit = withContext(Dispatchers.IO) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            profileDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                ?.forEach { chatDir ->
                    chatDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val entryName = "${chatDir.name}/${file.relativeTo(chatDir).path.replace("\\", "/")}"
                            zos.putNextEntry(ZipEntry(entryName))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
        }
    }

    override suspend fun exportEveryProfile(outputFile: File): Unit = withContext(Dispatchers.IO) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            rootDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("profile_") }
                ?.forEach { pDir ->
                    pDir.listFiles()
                        ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                        ?.forEach { chatDir ->
                            chatDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    val entryName = "${pDir.name}/${chatDir.name}/${file.relativeTo(chatDir).path.replace("\\", "/")}"
                                    zos.putNextEntry(ZipEntry(entryName))
                                    file.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                }
        }
    }

    override suspend fun importChat(zipFile: File): Unit = withContext(Dispatchers.IO) {
        val tempDir = File(profileDir, "import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(tempDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val chatFile = File(tempDir, "chat.json")
            if (chatFile.exists()) {
                val data = serializer.readFromFile(chatFile)
                val newId = UUID.randomUUID().toString()
                val targetDir = File(profileDir, "chat_$newId")

                tempDir.renameTo(targetDir)

                val updatedData = data.copy(
                    chat = data.chat.copy(id = newId),
                    messages = data.messages.map { it.copy(chatId = newId) }
                )
                serializer.writeToFile(File(targetDir, "chat.json"), updatedData)

                // Drop any events.ndjson that came in the zip — it references the old UUID.
                // The migrator will synthesize a fresh log keyed to the new UUID.
                File(targetDir, "events.ndjson").delete()
                EventLogMigrator.migrateChatIfNeeded(targetDir, deviceIdentity.deviceId)
            }
        } finally {
            if (tempDir.exists()) tempDir.deleteRecursively()
        }
        refreshChats()
    }

    override suspend fun scanForNewChats(): Unit = withContext(Dispatchers.IO) {
        refreshChats()
    }

    suspend fun refreshChats() = withContext(Dispatchers.IO) {
        val chats = profileDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.mapNotNull { dir ->
                val chatFile = File(dir, "chat.json")
                if (!chatFile.exists()) return@mapNotNull null
                try {
                    val data = serializer.readFromFile(chatFile)
                    data.toChat(data.chat.id, data.messages)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.lastMessageAt ?: it.createdAt }
            ?: emptyList()

        _chats.value = chats
    }

    /**
     * Apply an event authored by a remote peer. Idempotent — re-applying the same `eventId`
     * is a no-op. Does *not* call HLC.tick (the event already carries its own HLC); future
     * local writes that read the chat's prior events will naturally absorb the remote
     * physicalMs via [HlcTimestamp.tick]'s `max` rule.
     *
     * Returns true if the event was newly applied (caller should ack); false if duplicate.
     */
    override suspend fun applyExternalEvent(event: ChatEvent): Boolean = withContext(Dispatchers.IO) {
        applyExternalEventLocked(event)
    }

    @Synchronized
    private fun applyExternalEventLocked(event: ChatEvent): Boolean {
        val chatDir = File(profileDir, "chat_${event.chatId}")
        chatDir.mkdirs()
        val chatFile = File(chatDir, "chat.json")
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)

        val priorEvents = eventLog.readAll()
        if (priorEvents.any { it.eventId == event.eventId }) {
            // Idempotency — already applied. Still considered a "received" event; the caller
            // should ack so the peer can advance its lastSentHlc and not resend forever.
            return true
        }

        eventLog.append(event)
        val materialized = MaterializedView.fold(priorEvents + event)
        if (materialized != null) {
            serializer.writeToFile(chatFile, materialized)
        } else {
            chatFile.delete()
        }
        eventBus.publish(event)
        scope.launch { refreshChats() }
        return true
    }

    /**
     * Append a new event to the chat's log and re-materialize chat.json from the full log.
     * The event log is the source of truth; chat.json is a derived view. Synchronized so
     * concurrent mutations don't generate colliding HLCs (same physicalMs+logical+nodeId).
     */
    @Synchronized
    private fun appendAndMaterialize(chatId: String, op: ChatEventOp): ChatData? {
        val chatDir = File(profileDir, "chat_$chatId")
        chatDir.mkdirs()
        val chatFile = File(chatDir, "chat.json")
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)

        val priorEvents = eventLog.readAll()
        val newEvent = ChatEvent(
            eventId = UUID.randomUUID().toString(),
            hlc = HlcTimestamp.tick(
                previous = priorEvents.maxByOrNull { it.hlc }?.hlc,
                nowMs = System.currentTimeMillis(),
                nodeId = deviceIdentity.deviceId
            ),
            originDevice = deviceIdentity.deviceId,
            chatId = chatId,
            op = op
        )
        eventLog.append(newEvent)

        val materialized = MaterializedView.fold(priorEvents + newEvent)
        if (materialized != null) {
            serializer.writeToFile(chatFile, materialized)
        } else {
            chatFile.delete()
        }
        eventBus.publish(newEvent)
        return materialized
    }

    private fun ChatData.toChat(id: String, messages: List<Message>): Chat {
        val lastMsg = messages.maxByOrNull { it.createdAt }
        return Chat(
            id = id,
            name = chat.name,
            avatarFile = chat.avatarFile,
            backgroundFile = chat.backgroundFile,
            createdAt = chat.createdAt,
            lastMessageAt = lastMsg?.createdAt,
            lastMessagePreview = lastMsg?.let {
                when (it.type) {
                    MessageType.VOICE -> "🎤 Voice message"
                    MessageType.IMAGE -> "🖼 Image"
                    MessageType.CHECKLIST -> "☑ ${it.plainTextPreview()}"
                    MessageType.TEXT -> it.plainTextPreview()
                }
            },
            isPinned = chat.isPinned,
            bubbleColor = chat.bubbleColor,
            bubbleGradient = chat.bubbleGradient,
            isArchived = chat.isArchived,
            tags = chat.tags,
            isProtected = chat.isProtected,
            hideName = chat.hideName
        )
    }
}
