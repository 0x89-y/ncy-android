package com.ncy.notechaty.data.repository

import android.content.Context
import com.ncy.notechaty.data.model.*
import com.ncy.notechaty.data.serialization.ChatSerializer
import com.google.gson.Gson
import com.ncy.notechaty.data.sync.ChatEventBus
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.protection.ProtectionCrypto
import com.ncy.notechaty.data.sync.protection.ProtectionKeyStore
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
    private val eventBus: ChatEventBus,
    private val keyStore: ProtectionKeyStore
) : ChatRepository {

    private val serializer = ChatSerializer()
    private val eventCodec = ChatEventCodec()
    private val gson = Gson()
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
        // Phase 3: when a chat first becomes protected, generate a fresh salt so the
        // PIN can derive a stable per-chat encryption key. Carry forward any existing
        // salt unchanged; never re-generate (that would orphan already-encrypted events).
        val nextSalt = when {
            chat.isProtected && existing.chat.protectionSalt == null ->
                ProtectionCrypto.generateSalt()
            else -> existing.chat.protectionSalt
        }
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
            hideName = chat.hideName,
            protectionSalt = nextSalt
        )
        appendAndMaterialize(chat.id, ChatEventOp.ChatMetadataChanged(updated))
        refreshChats()
    }

    override suspend fun deleteChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        if (!chatDir.exists()) return@withContext

        // Emit a ChatTombstoned so paired devices learn the deletion. This appends the
        // event (HLC ticked past all prior events), drops chat.json via materialize
        // returning null, and publishes onto the bus so SyncManager live-forwards it
        // to connected peers.
        appendAndMaterialize(chatId, ChatEventOp.ChatTombstoned(chatId))

        // Privacy + disk: keep nothing on disk but the tombstone itself. Deleting the
        // media subdirs + collapsing events.ndjson to just the tombstone means the
        // chat's content is gone locally, while offline peers still pick the deletion
        // up via the catchup snapshot (which scans events.ndjson per chat dir).
        chatDir.listFiles()?.forEach { f ->
            if (f.name != "events.ndjson") f.deleteRecursively()
        }
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)
        val tombstoneOnly = eventLog.readAll().filter { it.op is ChatEventOp.ChatTombstoned }
        if (tombstoneOnly.isNotEmpty()) eventLog.replaceAll(tombstoneOnly)

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
        val target = ingestChatImage(chatDir, sourceFile, "avatar")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext
        val existing = serializer.readFromFile(chatFile)
        appendAndMaterialize(
            chatId,
            ChatEventOp.ChatMetadataChanged(existing.chat.copy(avatarFile = "avatar/${target.name}"))
        )
        refreshChats()
    }

    override suspend fun setChatBackground(chatId: String, sourceFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        val target = ingestChatImage(chatDir, sourceFile, "background")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext
        val existing = serializer.readFromFile(chatFile)
        appendAndMaterialize(
            chatId,
            ChatEventOp.ChatMetadataChanged(existing.chat.copy(backgroundFile = "background/${target.name}"))
        )
        refreshChats()
    }

    /**
     * Copy [sourceFile] into `<chatDir>/<subdir>/<sha256>.jpg`, dedup'd by hash. Used by
     * `setChatAvatar` and `setChatBackground` so both produce content-addressed files
     * that paired desktops can fetch via the existing blob protocol.
     */
    private fun ingestChatImage(chatDir: File, sourceFile: File, subdir: String): File {
        val dir = File(chatDir, subdir).apply { mkdirs() }
        val temp = File(dir, "incoming_${UUID.randomUUID()}.jpg")
        sourceFile.copyTo(temp, overwrite = true)
        val hash = MediaHash.ofFile(temp)
        val target = File(dir, "$hash.jpg")
        if (target.exists()) {
            temp.delete()
        } else if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = false)
            temp.delete()
        }
        return target
    }

    override suspend fun mediaFileForHash(
        hash: String,
        mediaType: String
    ): File? = withContext(Dispatchers.IO) {
        val subdirs = mediaSearchSubdirs(mediaType) ?: return@withContext null
        val ext = mediaExt(mediaType) ?: return@withContext null
        val needle = "$hash.$ext"
        profileDir.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.mapNotNull { chatDir ->
                subdirs.asSequence()
                    .map { sub -> File(File(chatDir, sub), needle) }
                    .firstOrNull { it.isFile }
            }
            ?.firstOrNull()
    }

    override suspend fun saveMediaBlob(
        chatId: String,
        hash: String,
        mediaType: String,
        bytes: ByteArray
    ): Unit = withContext(Dispatchers.IO) {
        val ext = mediaExt(mediaType) ?: return@withContext
        // Receive-side: save into the canonical write dir (voice/ or images/). Avatars
        // arriving from a peer also land in images/ — Avatar.svelte locally looks up by
        // hash, not by literal path, so the on-disk location doesn't matter to renderers.
        val subdir = mediaWriteSubdir(mediaType) ?: return@withContext
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

    private fun mediaExt(mediaType: String): String? = when (mediaType) {
        "voice" -> "m4a"
        "image" -> "jpg"
        else -> null
    }

    /** Where to look when *searching* by hash. Images can live under `images/` (message
     *  attachments), `avatar/` (chat avatars), or `background/` (chat wallpapers). */
    private fun mediaSearchSubdirs(mediaType: String): List<String>? = when (mediaType) {
        "voice" -> listOf("voice")
        "image" -> listOf("images", "avatar", "background")
        else -> null
    }

    /** Where to *write* a freshly-downloaded blob. Single canonical dir per media type. */
    private fun mediaWriteSubdir(mediaType: String): String? = when (mediaType) {
        "voice" -> "voice"
        "image" -> "images"
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

            // Locate every chat.json in the extracted tree. Single-chat backups
            // place it at the root; exportAllChats nests it under chat_<id>/;
            // exportEveryProfile under profile_<name>/chat_<id>/.
            val chatFiles = tempDir.walkTopDown()
                .filter { it.isFile && it.name == "chat.json" }
                .toList()

            if (chatFiles.isEmpty()) {
                throw IllegalArgumentException("No chats found in backup")
            }

            for (chatFile in chatFiles) {
                val srcChatDir = chatFile.parentFile ?: continue
                val data = serializer.readFromFile(chatFile)
                val newId = UUID.randomUUID().toString()
                val targetDir = File(profileDir, "chat_$newId")
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                // Copy chat contents (media/, voice/, etc.) into the new chat dir.
                srcChatDir.listFiles()?.forEach { src ->
                    src.copyRecursively(File(targetDir, src.name), overwrite = true)
                }

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
        // Phase 3: if this chat is protected and we have a cached key, encrypt the
        // payload before persisting. Skipped silently if the chat has no salt or no
        // unlocked key in memory — those cases happen before the user has finished
        // setting up protection, and the message simply persists in plaintext.
        val effectiveOp = encryptIfProtected(chatId, op)
        val newEvent = ChatEvent(
            eventId = UUID.randomUUID().toString(),
            hlc = HlcTimestamp.tick(
                previous = priorEvents.maxByOrNull { it.hlc }?.hlc,
                nowMs = System.currentTimeMillis(),
                nodeId = deviceIdentity.deviceId
            ),
            originDevice = deviceIdentity.deviceId,
            chatId = chatId,
            op = effectiveOp
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

    private fun encryptIfProtected(chatId: String, op: ChatEventOp): ChatEventOp {
        val (message, innerType) = when (op) {
            is ChatEventOp.MessageCreated -> op.message to ChatEventCodec.TYPE_MESSAGE_CREATED
            is ChatEventOp.MessageEdited -> op.message to ChatEventCodec.TYPE_MESSAGE_EDITED
            else -> return op
        }
        val metaFile = File(File(profileDir, "chat_$chatId"), "chat.json")
        if (!metaFile.exists()) return op
        val meta = try {
            serializer.readFromFile(metaFile).chat
        } catch (_: Exception) {
            return op
        }
        if (!meta.isProtected) return op
        meta.protectionSalt ?: return op
        val key = keyStore.keyFor(chatId) ?: return op

        val plaintext = gson.toJson(message).toByteArray(Charsets.UTF_8)
        val sealed = ProtectionCrypto.seal(key, plaintext)
        return ChatEventOp.EncryptedMessage(
            innerType = innerType,
            nonce = sealed.nonce,
            ciphertext = sealed.ciphertext
        )
    }

    /**
     * Phase 3 decrypt path: read raw events, replace any `EncryptedMessage` op we have
     * the key for with the decrypted `MessageCreated`/`MessageEdited`, then materialize.
     * Falls back to placeholders when no key is cached. Used by the UI when a chat is
     * unlocked — the on-disk `chat.json` always carries placeholders (so a stolen
     * device can't read protected content), and decryption happens transiently in
     * memory on each call.
     */
    override suspend fun getMessagesDecrypted(chatId: String): List<Message> = withContext(Dispatchers.IO) {
        val chatDir = File(profileDir, "chat_$chatId")
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)
        if (!eventLog.exists()) {
            // No event log yet — fall back to the materialized view (covers legacy
            // chats that never went through appendAndMaterialize after phase 0).
            val chatFile = File(chatDir, "chat.json")
            if (!chatFile.exists()) return@withContext emptyList()
            return@withContext serializer.readFromFile(chatFile).messages
        }
        val key = keyStore.keyFor(chatId)
        val events = eventLog.readAll().map { decryptOpIfPossible(it, key) }
        MaterializedView.fold(events)?.messages ?: emptyList()
    }

    override suspend fun migrateProtectedChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        val key = keyStore.keyFor(chatId) ?: return@withContext
        val chatDir = File(profileDir, "chat_$chatId")
        val chatFile = File(chatDir, "chat.json")
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)
        if (!eventLog.exists()) return@withContext

        val events = eventLog.readAll()
        var changed = false
        val migrated = events.map { ev ->
            val (message, innerType) = when (val op = ev.op) {
                is ChatEventOp.MessageCreated -> op.message to ChatEventCodec.TYPE_MESSAGE_CREATED
                is ChatEventOp.MessageEdited -> op.message to ChatEventCodec.TYPE_MESSAGE_EDITED
                // Tombstones, metadata, and already-encrypted ops stay verbatim —
                // re-running migration is therefore idempotent.
                else -> return@map ev
            }
            val plaintext = gson.toJson(message).toByteArray(Charsets.UTF_8)
            val sealed = ProtectionCrypto.seal(key, plaintext)
            changed = true
            ev.copy(
                op = ChatEventOp.EncryptedMessage(innerType, sealed.nonce, sealed.ciphertext)
            )
        }
        if (!changed) return@withContext

        // Same eventIds/HLCs preserved, so this only secures the *local* at-rest copy.
        // Peers that already synced the plaintext keep it (idempotent dedup by eventId)
        // until that chat is reset on their side — documented limitation.
        eventLog.replaceAll(migrated)
        val materialized = MaterializedView.fold(migrated)
        if (materialized != null) serializer.writeToFile(chatFile, materialized)
    }

    private fun decryptOpIfPossible(event: ChatEvent, key: ByteArray?): ChatEvent {
        val op = event.op
        if (op !is ChatEventOp.EncryptedMessage) return event
        if (key == null) return event
        val plaintext = ProtectionCrypto.open(
            key,
            ProtectionCrypto.Sealed(op.nonce, op.ciphertext)
        ) ?: return event
        val message = try {
            gson.fromJson(String(plaintext, Charsets.UTF_8), Message::class.java)
        } catch (_: Exception) {
            return event
        }
        val newOp = when (op.innerType) {
            ChatEventCodec.TYPE_MESSAGE_CREATED -> ChatEventOp.MessageCreated(message)
            ChatEventCodec.TYPE_MESSAGE_EDITED -> ChatEventOp.MessageEdited(message)
            else -> return event
        }
        return event.copy(op = newOp)
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
            hideName = chat.hideName,
            protectionSalt = chat.protectionSalt
        )
    }
}
