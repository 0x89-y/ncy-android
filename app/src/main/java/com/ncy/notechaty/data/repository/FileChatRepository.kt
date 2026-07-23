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
import com.ncy.notechaty.data.sync.event.ChatEventHistory
import com.ncy.notechaty.data.sync.event.ChatEventCodec
import com.ncy.notechaty.data.sync.event.ChatEventOp
import com.ncy.notechaty.data.sync.event.EventLog
import com.ncy.notechaty.data.sync.event.ChatTombstoneGraveyard
import com.ncy.notechaty.data.sync.event.EventLogMigrator
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.event.MaterializedView
import com.ncy.notechaty.data.sync.event.MessageTombstoneGraveyard
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
import androidx.annotation.Keep
import android.util.Log

/**
 * Profile registry snapshot embedded in a full backup zip as `profiles.json`.
 * Profiles live in SharedPreferences, not on disk, so without this a restored
 * `profile_<id>/` directory tree would be orphaned (no entry in the switcher).
 * Carries the full [Profile] (name, icon, and PIN hash/salt) so a restore is
 * full-fidelity. Optional: absent in pre-manifest / single-chat backups.
 *
 * Public + `@Keep` because Gson reflects on it; private/minified classes are
 * unreliable to instantiate. The secondary no-arg constructor is belt-and-
 * suspenders so Gson never has to fall back to `Unsafe`-allocation.
 */
@Keep
data class BackupManifest(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String = "",
    /**
     * The device's app-wide protection PIN, so a fresh-install restore can
     * unlock protected chats (whose sealed content is encrypted under
     * `PBKDF2(that PIN, ChatMetadata.protectionSalt)`). Without this, the
     * salt+ciphertext survive but there's nothing to check the entered PIN
     * against, so `ChatViewModel.verifyPin` refuses every PIN. Both fields
     * are `null` on backups from devices with no app PIN set, and on legacy
     * backups from before this field existed.
     */
    val appPinHash: String? = null,
    val appPinSalt: String? = null
) {
    @Suppress("unused")
    constructor() : this(emptyList(), "", null, null)
}

@Singleton
class FileChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val chatLockState: ChatLockState,
    private val deviceIdentity: DeviceIdentity,
    private val eventBus: ChatEventBus,
    private val keyStore: ProtectionKeyStore,
    private val settingsRepository: SettingsRepository
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
        scope.launch {
            consolidateEquivalentProfiles()
            refreshChats()
        }
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
            protectionSalt = nextSalt,
            remoteMessagesOnLeft = chat.remoteMessagesOnLeft,
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
        if (tombstoneOnly.isNotEmpty()) {
            ChatTombstoneGraveyard.archive(profileDir, tombstoneOnly.last())
            chatDir.deleteRecursively()
        }

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
        mediaType: String,
        profileId: String?,
    ): File? = withContext(Dispatchers.IO) {
        val subdirs = mediaSearchSubdirs(mediaType) ?: return@withContext null
        val ext = mediaExt(mediaType) ?: return@withContext null
        val needle = "$hash.$ext"
        mediaProfileDirs(profileId)
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { it.isDirectory && it.name.startsWith("chat_") }
            .mapNotNull { chatDir ->
                subdirs.asSequence()
                    .map { sub -> File(File(chatDir, sub), needle) }
                    .firstOrNull { it.isFile }
            }
            .firstOrNull()
    }

    override suspend fun saveMediaBlobFromFile(
        chatId: String,
        hash: String,
        mediaType: String,
        sourceFile: File,
        profileId: String?,
    ): Unit = withContext(Dispatchers.IO) {
        val ext = mediaExt(mediaType) ?: run {
            sourceFile.delete()
            return@withContext
        }
        val subdir = mediaWriteSubdir(mediaType) ?: run {
            sourceFile.delete()
            return@withContext
        }
        val targetProfile = safeProfileId(profileId)?.let { File(rootDir, "profile_$it") } ?: profileDir
        val dir = File(File(targetProfile, "chat_$chatId"), subdir).apply { mkdirs() }
        val target = File(dir, "$hash.$ext")
        if (target.exists()) {
            sourceFile.delete()
        } else if (!sourceFile.renameTo(target)) {
            sourceFile.copyTo(target, overwrite = false)
            sourceFile.delete()
        }
    }

    private fun mediaExt(mediaType: String): String? = when (mediaType) {
        "voice" -> "m4a"
        "image" -> "jpg"
        "file" -> "bin"
        else -> null
    }

    /** Where to look when *searching* by hash. Images can live under `images/` (message
     *  attachments), `avatar/` (chat avatars), or `background/` (chat wallpapers). */
    private fun mediaSearchSubdirs(mediaType: String): List<String>? = when (mediaType) {
        "voice" -> listOf("voice")
        "image" -> listOf("images", "avatar", "background")
        "file" -> listOf("files")
        else -> null
    }

    /** Where to *write* a freshly-downloaded blob. Single canonical dir per media type. */
    private fun mediaWriteSubdir(mediaType: String): String? = when (mediaType) {
        "voice" -> "voice"
        "image" -> "images"
        "file" -> "files"
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

    override suspend fun getFilesDir(chatId: String): File {
        val dir = File(File(profileDir, "chat_$chatId"), "files")
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
            File(profileDir, ChatTombstoneGraveyard.FILE_NAME).takeIf { it.isFile }?.let { graveyard ->
                zos.putNextEntry(ZipEntry(ChatTombstoneGraveyard.FILE_NAME))
                graveyard.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
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
            // Profile registry first, so a restore can rebuild the switcher and
            // place chats under the right (named) profile. Also the app-wide
            // protection PIN — protected chats' encryption is `PBKDF2(pin, salt)`
            // and the phone verifies entered PINs against this app-wide hash,
            // so without it a fresh-install restore can't unlock anything even
            // though the salt+ciphertext are intact.
            val appSettings = settingsRepository.settings.value
            val manifest = BackupManifest(
                profiles = profileRepository.profiles.value,
                activeProfileId = profileRepository.activeProfileId.value,
                appPinHash = appSettings.protectionPinHash,
                appPinSalt = appSettings.protectionPinSalt
            )
            zos.putNextEntry(ZipEntry("profiles.json"))
            zos.write(gson.toJson(manifest).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            File(rootDir, ChatTombstoneGraveyard.FILE_NAME).takeIf { it.isFile }?.let { graveyard ->
                zos.putNextEntry(ZipEntry(ChatTombstoneGraveyard.FILE_NAME))
                graveyard.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            rootDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("profile_") }
                ?.forEach { pDir ->
                    File(pDir, ChatTombstoneGraveyard.FILE_NAME).takeIf { it.isFile }?.let { graveyard ->
                        zos.putNextEntry(ZipEntry("${pDir.name}/${ChatTombstoneGraveyard.FILE_NAME}"))
                        graveyard.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
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

            // Profile registry from a full backup (absent in legacy / single-chat
            // zips). Restored chats keep their original `profile_<id>` directory,
            // so registering these makes them visible in the switcher again.
            val manifestFile = File(tempDir, "profiles.json")
            val manifest: BackupManifest? = if (manifestFile.exists()) {
                val raw = try {
                    manifestFile.readText()
                } catch (e: Exception) {
                    Log.w("FileChatRepo", "import: profiles.json unreadable", e)
                    null
                }
                if (raw == null) null else try {
                    val m = gson.fromJson(raw, BackupManifest::class.java)
                    Log.i(
                        "FileChatRepo",
                        "import: manifest parsed — profiles=${m?.profiles?.size ?: 0}, active=${m?.activeProfileId}"
                    )
                    m
                } catch (e: Exception) {
                    Log.w("FileChatRepo", "import: profiles.json parse failed; raw len=${raw.length}", e)
                    null
                }
            } else {
                Log.i("FileChatRepo", "import: no profiles.json in backup")
                null
            }
            val restoredProfileIds = HashSet<String>()

            val graveyardFiles = tempDir.walkTopDown()
                .filter { it.isFile && it.name == ChatTombstoneGraveyard.FILE_NAME }
                .toList()
            val messageGraveyardFiles = tempDir.walkTopDown()
                .filter { it.isFile && it.name == MessageTombstoneGraveyard.FILE_NAME }
                .toList()
            val importNow = System.currentTimeMillis()
            graveyardFiles.forEach { file ->
                val events = EventLog(file, eventCodec).readAll()
                if (events.any {
                        it.op !is ChatEventOp.ChatTombstoned ||
                            !HlcTimestamp.isAcceptableRemote(it.hlc, importNow)
                    }
                ) throw IllegalArgumentException("Backup contains an invalid tombstone graveyard")
                val relParts = file.relativeTo(tempDir).path.replace("\\", "/").split("/")
                val profileSeg = relParts.firstOrNull { it.startsWith("profile_") }
                val targetParent = if (profileSeg != null) {
                    restoredProfileIds += profileSeg.removePrefix("profile_")
                    File(rootDir, profileSeg).also { it.mkdirs() }
                } else profileDir
                ChatTombstoneGraveyard.merge(targetParent, events)
            }
            messageGraveyardFiles.forEach { file ->
                val events = MessageTombstoneGraveyard.events(requireNotNull(file.parentFile))
                if (events.any {
                        it.op !is ChatEventOp.MessageTombstoned ||
                            !HlcTimestamp.isAcceptableRemote(it.hlc, importNow)
                    }
                ) throw IllegalArgumentException("Backup contains an invalid message tombstone graveyard")
            }

            // Locate every chat directory, including tombstone-only histories that
            // intentionally have no chat.json. Single-chat backups place files at
            // the root; exportAllChats nests under chat_<id>/; exportEveryProfile
            // under profile_<id>/chat_<id>/.
            val chatDirs = tempDir.walkTopDown()
                .filter { dir ->
                    dir.isDirectory &&
                        (File(dir, "chat.json").isFile || File(dir, "events.ndjson").isFile)
                }
                .toList()

            if (chatDirs.isEmpty() && graveyardFiles.isEmpty() && messageGraveyardFiles.isEmpty()) {
                throw IllegalArgumentException("No chats found in backup")
            }

            // Validate every event log before modifying any destination directory.
            // Restore is allowed to replay old events, but it must not bypass the
            // same remote-clock policy enforced by live sync.
            chatDirs.forEach { chatDir ->
                val eventsFile = File(chatDir, "events.ndjson")
                if (eventsFile.exists()) {
                    val invalid = EventLog(eventsFile, eventCodec).readAll().firstOrNull {
                        !HlcTimestamp.isAcceptableRemote(it.hlc, importNow)
                    }
                    if (invalid != null) {
                        throw IllegalArgumentException(
                            "Backup contains an invalid or future-skewed event clock"
                        )
                    }
                }
            }

            for (srcChatDir in chatDirs) {
                val chatFile = File(srcChatDir, "chat.json")
                val data = chatFile.takeIf { it.isFile }?.let(serializer::readFromFile)
                val backupEventsFile = File(srcChatDir, "events.ndjson")
                val firstEvent = backupEventsFile.takeIf { it.isFile }
                    ?.let { EventLog(it, eventCodec).readAll().firstOrNull() }
                // **Preserve the original chatId.** Rekeying to a fresh UUID
                // would silently duplicate the whole chat on any peer that
                // still holds it under the old id: `applyExternalEventLocked`
                // dedups per-chat-directory, so a peer re-streaming events
                // with the original chatId lands in a *different* dir on this
                // device and every event gets appended fresh. Same-id restore
                // keeps eventId dedup working and syncs merge cleanly.
                val chatId = data?.chat?.id ?: firstEvent?.chatId
                    ?: throw IllegalArgumentException("Backup chat has no stable chat ID")

                // Restore into the profile the chat came from. exportEveryProfile
                // records a `profile_<id>/` path segment; honor it so a multi-profile
                // backup doesn't collapse every chat into the active profile. Single-
                // chat / exportAllChats backups carry no profile info → active profile.
                val relParts = srcChatDir.relativeTo(tempDir).path
                    .replace("\\", "/").split("/")
                val profileSeg = relParts.firstOrNull { it.startsWith("profile_") }
                val targetParent = if (profileSeg != null) {
                    restoredProfileIds += profileSeg.removePrefix("profile_")
                    File(rootDir, profileSeg).also { it.mkdirs() }
                } else {
                    profileDir
                }

                if (ChatTombstoneGraveyard.contains(rootDir, chatId)) continue

                val targetDir = File(targetParent, "chat_$chatId")
                if (!targetDir.exists()) {
                    // Fresh restore: copy the chat dir verbatim (events + media).
                    // eventIds and chatIds already match what any peer holds, so
                    // subsequent syncs converge via `eventId`-based dedup.
                    targetDir.mkdirs()
                    srcChatDir.listFiles()?.forEach { src ->
                        src.copyRecursively(File(targetDir, src.name), overwrite = true)
                    }
                    val eventsFile = File(targetDir, "events.ndjson")
                    if (eventsFile.exists()) {
                        val rebuilt = MaterializedView.fold(ChatEventHistory.read(targetDir, eventCodec))
                        if (rebuilt != null) {
                            serializer.writeToFile(File(targetDir, "chat.json"), rebuilt)
                        } else {
                            // Nothing to show, but the event log is still source of
                            // truth. In particular, deleting a tombstone-only directory
                            // here would let a stale peer resurrect the chat later.
                            File(targetDir, "chat.json").delete()
                            targetDir.listFiles()?.forEach { child ->
                                if (child.name != "events.ndjson" &&
                                    child.name != MessageTombstoneGraveyard.FILE_NAME
                                ) child.deleteRecursively()
                            }
                        }
                    } else {
                        // Legacy chat.json-only backup: keep the id, synthesize log.
                        serializer.writeToFile(
                            File(targetDir, "chat.json"),
                            requireNotNull(data) { "Legacy backup chat is missing chat.json" },
                        )
                        EventLogMigrator.migrateChatIfNeeded(targetDir, deviceIdentity.deviceId)
                    }
                } else if (backupEventsFile.exists()) {
                    // Target already present on this device — MERGE by eventId
                    // union rather than clobbering. Never destroys local edits;
                    // safe to re-import the same zip repeatedly (idempotent).
                    val existingLog = EventLog(File(targetDir, "events.ndjson"), eventCodec)
                    val prior = existingLog.readAll()
                    val seen = prior.mapTo(HashSet()) { it.eventId }
                    val incoming = EventLog(backupEventsFile, eventCodec).readAll()
                    MessageTombstoneGraveyard.merge(
                        targetDir,
                        MessageTombstoneGraveyard.events(srcChatDir) +
                            incoming.filter { it.op is ChatEventOp.MessageTombstoned },
                    )
                    val added = incoming.filter {
                        it.eventId !in seen && it.op !is ChatEventOp.MessageTombstoned &&
                            !isSuppressedByMessageGraveyard(targetDir, it)
                    }
                    if (added.isNotEmpty()) {
                        existingLog.replaceAll(prior + added)
                    }
                    // Copy any media the target doesn't already have.
                    srcChatDir.listFiles()?.forEach { src ->
                        if (src.isDirectory) {
                            val destDir = File(targetDir, src.name).apply { mkdirs() }
                            src.listFiles()?.forEach { f ->
                                val destFile = File(destDir, f.name)
                                if (!destFile.exists()) f.copyTo(destFile, overwrite = false)
                            }
                        }
                    }
                    val rebuilt = MaterializedView.fold(ChatEventHistory.read(targetDir, eventCodec))
                    if (rebuilt != null) {
                        serializer.writeToFile(File(targetDir, "chat.json"), rebuilt)
                    } else {
                        File(targetDir, "chat.json").delete()
                    }
                    Log.i(
                        "FileChatRepo",
                        "import: merged $chatId — added ${added.size} of ${incoming.size} events",
                    )
                }
            }

            // Rebuild the profile registry so restored chats aren't orphaned.
            // Manifest profiles (full fidelity: name, icon, PIN) come first;
            // any restored directory not covered by a manifest (legacy backup)
            // gets a placeholder profile so its chats are still reachable.
            manifest?.profiles?.let { profileRepository.registerImported(it) }
            for (pid in restoredProfileIds) {
                val name = manifest?.profiles?.firstOrNull { it.id == pid }?.name
                    ?: "Profile ${pid.take(6)}"
                profileRepository.ensureProfileExists(pid, name)
            }
            // Land the user back on the profile they backed up from, if it's now
            // present (setActive ignores unknown ids and is a no-op otherwise).
            val manifestActive = manifest?.activeProfileId?.takeIf { it.isNotEmpty() }
            manifestActive?.let { profileRepository.setActive(it) }

            // Restore the app-wide protection PIN so protected chats actually
            // unlock after a fresh-install restore. `verifyPin` in ChatViewModel
            // checks entered PINs against this hash before deriving the AES key
            // from `PBKDF2(pin, chatSalt)` — without a matching hash, every PIN
            // is rejected. Only restore when the target device has no PIN of
            // its own; that way a fresh install picks it up, but importing onto
            // a working device with its own PIN doesn't silently overwrite it.
            val manifestHash = manifest?.appPinHash
            val manifestSalt = manifest?.appPinSalt
            if (
                !manifestHash.isNullOrEmpty() &&
                !manifestSalt.isNullOrEmpty() &&
                settingsRepository.settings.value.protectionPinHash.isNullOrEmpty()
            ) {
                val current = settingsRepository.settings.value
                settingsRepository.updateSettings(
                    current.copy(
                        protectionPinHash = manifestHash,
                        protectionPinSalt = manifestSalt
                    )
                )
                Log.i("FileChatRepo", "import: restored app-wide protection PIN from manifest")
            }

            // Sweep a fresh-install default profile left behind by the restore:
            // when the manifest carries profiles, any pre-existing profile not in
            // it that has no chats is just a confusing duplicate (the empty
            // "Personal" the app created on first launch). Only runs for
            // manifest-rich backups, so legacy single-chat imports stay untouched.
            if ((manifest?.profiles?.size ?: 0) > 0) {
                val manifestIds = manifest!!.profiles.mapTo(HashSet()) { it.id }
                val activeId = profileRepository.activeProfileId.value
                val orphans = profileRepository.profiles.value.filter { p ->
                    p.id !in manifestIds && p.id != activeId && !profileDirHasChats(p.id)
                }
                for (o in orphans) {
                    Log.i("FileChatRepo", "import: dropping empty pre-existing profile ${o.id} (${o.name})")
                    profileRepository.removeOrphanProfile(o.id)
                }
            }

            // A backup created after several reinstall generations can legitimately
            // contain multiple equal-name profile IDs. Repair them immediately so
            // the user never has to restart or wait for another sync connection.
            consolidateEquivalentProfiles()

            Log.i(
                "FileChatRepo",
                "import: done — chats=${chatDirs.size}, profileDirs=${restoredProfileIds.size}," +
                    " manifestProfiles=${manifest?.profiles?.size ?: 0}," +
                    " activeAfter=${profileRepository.activeProfileId.value}," +
                    " profilesAfter=${profileRepository.profiles.value.size}"
            )
        } finally {
            if (tempDir.exists()) tempDir.deleteRecursively()
        }
        refreshChats()
    }

    /** Does this profile's directory contain any `chat_*` subdir? */
    private fun profileDirHasChats(profileId: String): Boolean {
        val dir = File(rootDir, "profile_$profileId")
        if (!dir.exists()) return false
        return dir.listFiles()?.any { it.isDirectory && it.name.startsWith("chat_") } ?: false
    }


    override suspend fun purgeChatTombstones(): Int = withContext(Dispatchers.IO) {
        ChatTombstoneGraveyard.purge(rootDir) + MessageTombstoneGraveyard.purge(rootDir)
    }

    override suspend fun scanForNewChats(): Unit = withContext(Dispatchers.IO) {
        refreshChats()
    }

    suspend fun refreshChats() = withContext(Dispatchers.IO) {
        // Capture the profile once. A profile switch can happen while files are being
        // decoded; an older refresh must never publish its result into the new profile.
        val profileId = profileRepository.activeProfileId.value
        val directory = if (profileId.isEmpty()) rootDir else File(rootDir, "profile_$profileId")
        directory.mkdirs()
        val chatDirectories = directory.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?: emptyList()
        Log.i(
            "FileChatRepo",
            "refresh start active=${profileId.take(8)} profiles=${profileRepository.profiles.value.size} " +
                "profileDirs=${rootDir.listFiles()?.count {
                    it.isDirectory && it.name.startsWith("profile_")
                } ?: 0} chatDirs=${chatDirectories.size}",
        )
        val chats = chatDirectories
            ?.mapNotNull { dir ->
                val chatFile = File(dir, "chat.json")
                try {
                    val data = loadOrRepairChat(dir, chatFile) ?: return@mapNotNull null
                    data.toChat(data.chat.id, data.messages)
                } catch (e: Exception) {
                    Log.w(
                        "FileChatRepo",
                        "Could not load ${dir.name}; preserving its event log for recovery",
                        e,
                    )
                    null
                }
            }
            ?.sortedByDescending { it.lastMessageAt ?: it.createdAt }
            ?: emptyList()

        if (profileRepository.activeProfileId.value == profileId) {
            _chats.value = chats
            Log.i(
                "FileChatRepo",
                "refresh publish active=${profileId.take(8)} chats=${chats.size} " +
                    "archived=${chats.count { it.isArchived }}",
            )
        } else {
            Log.i(
                "FileChatRepo",
                "refresh discarded active changed ${profileId.take(8)} -> " +
                    profileRepository.activeProfileId.value.take(8),
            )
        }
    }

    /**
     * `chat.json` is only a derived cache; `events.ndjson` remains the source of truth.
     * Recover a missing/corrupt cache during every explicit scan so an already-acked
     * chat cannot disappear permanently from the UI and require a peer to resend it.
     */
    private fun loadOrRepairChat(chatDir: File, chatFile: File): ChatData? {
        if (chatFile.exists()) {
            try {
                return serializer.readFromFile(chatFile)
            } catch (error: Exception) {
                Log.w("FileChatRepo", "Repairing unreadable ${chatDir.name}/chat.json", error)
            }
        }

        val eventsFile = File(chatDir, "events.ndjson")
        if (!eventsFile.exists()) return null
        val rebuilt = MaterializedView.fold(ChatEventHistory.read(chatDir, eventCodec)) ?: return null
        serializer.writeToFile(chatFile, rebuilt)
        return rebuilt
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
        applyExternalEventLocked(canonicalizeInboundEvent(event))
    }

    override fun canonicalizeInboundEvent(event: ChatEvent): ChatEvent {
        val id = safeProfileId(event.profileId) ?: return event
        val profile = profileRepository.resolveSyncedProfile(id, event.profileName)
        return event.copy(profileId = profile.id, profileName = profile.name)
    }

    @Synchronized
    private fun applyExternalEventLocked(event: ChatEvent): Boolean {
        if (ChatTombstoneGraveyard.contains(rootDir, event.chatId)) return false
        val targetProfileDir = profileDirForInbound(event)
        consolidateChatCopies(event.chatId, targetProfileDir)
        val chatDir = File(targetProfileDir, "chat_${event.chatId}")
        chatDir.mkdirs()
        val chatFile = File(chatDir, "chat.json")
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)

        val priorEvents = ChatEventHistory.read(chatDir, eventCodec)
        if (priorEvents.any { it.eventId == event.eventId }) {
            // Idempotency — already applied. The caller still acks it, but `false`
            // prevents duplicate UI work and duplicate media fetches during handoff.
            return false
        }

        if (isSuppressedByMessageGraveyard(chatDir, event)) return false
        val persisted = if (event.op is ChatEventOp.MessageTombstoned) {
            MessageTombstoneGraveyard.archive(chatDir, event)
        } else {
            eventLog.append(event)
            true
        }
        if (!persisted) return false
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

    /** Merge reinstall-generated copies of one logical chat before applying an event. */
    private fun consolidateChatCopies(chatId: String, targetProfileDir: File) {
        val targetDir = File(targetProfileDir, "chat_$chatId")
        val targetProfile = targetProfileDir.name
            .takeIf { it.startsWith("profile_") }
            ?.removePrefix("profile_")
            ?.let(profileRepository::profileById)
        val parents = buildList {
            add(rootDir) // legacy pre-profile layout
            rootDir.listFiles()
                ?.filterTo(this) { it.isDirectory && it.name.startsWith("profile_") }
        }
        val sources = parents.map { File(it, "chat_$chatId") }
            .filter { it.exists() && it.absolutePath != targetDir.absolutePath }
            .filter { source ->
                if (source.parentFile == rootDir) return@filter true
                val sourceProfile = source.parentFile?.name
                    ?.removePrefix("profile_")
                    ?.let(profileRepository::profileById)
                    ?: return@filter false
                targetProfile != null &&
                    !targetProfile.isProtected && !sourceProfile.isProtected &&
                    targetProfile.name.trim().equals(sourceProfile.name.trim(), ignoreCase = true)
            }
        if (sources.isEmpty()) return

        targetDir.mkdirs()
        val targetLog = EventLog(File(targetDir, "events.ndjson"), eventCodec)
        val combined = targetLog.readAll().toMutableList()
        val seen = combined.mapTo(HashSet()) { it.eventId }
        sources.forEach { source ->
            MessageTombstoneGraveyard.merge(
                targetDir,
                MessageTombstoneGraveyard.events(source) +
                    EventLog(File(source, "events.ndjson"), eventCodec).readAll()
                        .filter { it.op is ChatEventOp.MessageTombstoned },
            )
            val sourceLog = EventLog(File(source, "events.ndjson"), eventCodec)
            sourceLog.readAll().forEach {
                if (it.op !is ChatEventOp.MessageTombstoned &&
                    !isSuppressedByMessageGraveyard(targetDir, it) && seen.add(it.eventId)
                ) combined += it
            }
            source.walkTopDown().filter { it.isFile }.forEach { file ->
                if (file.name == "events.ndjson" || file.name == "chat.json" ||
                    file.name == MessageTombstoneGraveyard.FILE_NAME
                ) return@forEach
                val destination = File(targetDir, file.relativeTo(source).path)
                if (!destination.exists()) {
                    destination.parentFile?.mkdirs()
                    file.copyTo(destination, overwrite = false)
                }
            }
        }
        if (combined.isNotEmpty()) targetLog.replaceAll(combined.sortedBy { it.hlc })
        sources.forEach { source ->
            val sourceProfileId = source.parentFile?.name
                ?.takeIf { it.startsWith("profile_") }
                ?.removePrefix("profile_")
            source.deleteRecursively()
            if (sourceProfileId != null && !profileDirHasChats(sourceProfileId)) {
                profileRepository.removeOrphanProfile(sourceProfileId)
            }
        }
        Log.i("FileChatRepo", "consolidated ${sources.size} duplicate copies of chat $chatId")
    }

    /**
     * Repair profile multiplication caused by older clients treating every historical
     * reinstall UUID as a new profile. Only unprotected, equal-name profiles are safe
     * to coalesce; PIN-protected profiles always retain their separate identity.
     */
    @Synchronized
    private fun consolidateEquivalentProfiles() {
        val activeId = profileRepository.activeProfileId.value
        profileRepository.profiles.value
            .filterNot { it.isProtected }
            .groupBy { it.name.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .forEach { equivalents ->
                val target = equivalents.firstOrNull { it.id == activeId }
                    ?: equivalents.minWith(compareBy<com.ncy.notechaty.data.model.Profile> { it.createdAt }.thenBy { it.id })
                val chatIds = equivalents.asSequence()
                    .flatMap { profile ->
                        File(rootDir, "profile_${profile.id}").listFiles()
                            ?.asSequence()
                            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                            ?.map { it.name.removePrefix("chat_") }
                            ?: emptySequence()
                    }
                    .toSet()
                val targetProfileDir = File(rootDir, "profile_${target.id}").also { it.mkdirs() }
                chatIds.forEach { chatId ->
                    consolidateChatCopies(chatId, targetProfileDir)
                    val chatDir = File(targetProfileDir, "chat_$chatId")
                    val events = ChatEventHistory.read(chatDir, eventCodec)
                    val chatFile = File(chatDir, "chat.json")
                    MaterializedView.fold(events)?.let { serializer.writeToFile(chatFile, it) }
                        ?: chatFile.delete()
                }
                equivalents.filter { it.id != target.id }.forEach { source ->
                    if (!profileDirHasChats(source.id)) profileRepository.removeOrphanProfile(source.id)
                }
            }
    }

    /** Route a stamped event independently of the profile currently open in the UI. */
    private fun profileDirForInbound(event: ChatEvent): File {
        val id = safeProfileId(event.profileId) ?: return profileDir
        profileRepository.ensureProfileExists(
            id,
            event.profileName.ifBlank { "Synced profile ${id.take(8)}" },
        )
        return File(rootDir, "profile_$id").also { it.mkdirs() }
    }

    private fun safeProfileId(id: String?): String? = id?.takeIf { value ->
        value.isNotEmpty() && value.length <= 128 &&
            value.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    private fun mediaProfileDirs(profileId: String?): Sequence<File> {
        val id = safeProfileId(profileId)
        if (id != null) return sequenceOf(File(rootDir, "profile_$id"))
        val profiles = rootDir.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && it.name.startsWith("profile_") }
            ?: emptySequence()
        return sequenceOf(rootDir) + profiles
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

        val priorEvents = ChatEventHistory.read(chatDir, eventCodec)
        // Phase 3: if this chat is protected and we have a cached key, encrypt the
        // payload before persisting. Skipped silently if the chat has no salt or no
        // unlocked key in memory — those cases happen before the user has finished
        // setting up protection, and the message simply persists in plaintext.
        val effectiveOp = encryptIfProtected(chatId, op)
        // Profile sync: stamp which profile this chat lives in so a paired desktop can
        // group by profile. The mutated chat is always in the active profile (the
        // repository is active-profile-scoped).
        val activeProfileId = profileRepository.activeProfileId.value
        val activeProfileName = profileRepository.activeProfile()?.name ?: ""
        val newEvent = ChatEvent(
            eventId = UUID.randomUUID().toString(),
            hlc = HlcTimestamp.tick(
                previous = priorEvents.maxByOrNull { it.hlc }?.hlc,
                nowMs = System.currentTimeMillis(),
                nodeId = deviceIdentity.deviceId
            ),
            originDevice = deviceIdentity.deviceId,
            chatId = chatId,
            op = effectiveOp,
            profileId = activeProfileId,
            profileName = activeProfileName
        )
        if (isSuppressedByMessageGraveyard(chatDir, newEvent)) {
            return MaterializedView.fold(priorEvents)
        }
        if (newEvent.op is ChatEventOp.MessageTombstoned) {
            MessageTombstoneGraveyard.archive(chatDir, newEvent)
        } else {
            eventLog.append(newEvent)
        }

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
            ciphertext = sealed.ciphertext,
            messageId = message.id,
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
        val trustedDecrypted = HashSet<String>()
        val events = ChatEventHistory.read(chatDir, eventCodec).map { original ->
            val transformed = decryptOpIfPossible(original, key)
            if (original.op is ChatEventOp.EncryptedMessage &&
                transformed.op !is ChatEventOp.EncryptedMessage
            ) {
                trustedDecrypted += original.eventId
            }
            transformed
        }
        MaterializedView.fold(events, trustedDecrypted)?.messages ?: emptyList()
    }

    override suspend fun migrateProtectedChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        val key = keyStore.keyFor(chatId) ?: return@withContext
        val chatDir = File(profileDir, "chat_$chatId")
        val chatFile = File(chatDir, "chat.json")
        val eventLog = EventLog(File(chatDir, "events.ndjson"), eventCodec)
        if (!eventLog.exists()) return@withContext

        val compactTombstones = MessageTombstoneGraveyard.events(chatDir)
        val tombstonedIds = compactTombstones.mapNotNullTo(HashSet()) {
            (it.op as? ChatEventOp.MessageTombstoned)?.messageId
        }
        val events = eventLog.readAll()
        val passthrough = ArrayList<ChatEvent>(events.size)
        val plaintextMsgs = ArrayList<ChatEvent>()
        for (ev in events) {
            when (ev.op) {
                is ChatEventOp.MessageCreated ->
                    if (ev.op.message.id !in tombstonedIds) plaintextMsgs += ev
                is ChatEventOp.MessageEdited ->
                    if (ev.op.message.id !in tombstonedIds) plaintextMsgs += ev
                // Tombstones, metadata, and already-encrypted ops pass through —
                // re-running migration is therefore idempotent (no plaintext left).
                else -> passthrough += ev
            }
        }
        if (plaintextMsgs.isEmpty()) return@withContext

        // Re-issue each plaintext message as a *fresh* sealed event: a new eventId
        // and an HLC ticked past everything, so it actually propagates. Peers dedupe
        // by eventId and would otherwise never replace the plaintext they already
        // synced; the `supersedes` pointer names the exact stale plaintext event for
        // them to drop. The old plaintext events are not retained — the sealed ones
        // carry the same content (same messageId, higher HLC) so the fold replaces
        // them locally too.
        var maxHlc: HlcTimestamp? = events.maxByOrNull { it.hlc }?.hlc
        val reissued = ArrayList<ChatEvent>(plaintextMsgs.size)
        for (ev in plaintextMsgs) {
            val (message, innerType) = when (val op = ev.op) {
                is ChatEventOp.MessageCreated -> op.message to ChatEventCodec.TYPE_MESSAGE_CREATED
                is ChatEventOp.MessageEdited -> op.message to ChatEventCodec.TYPE_MESSAGE_EDITED
                else -> continue
            }
            val plaintext = gson.toJson(message).toByteArray(Charsets.UTF_8)
            val sealed = ProtectionCrypto.seal(key, plaintext)
            val hlc = HlcTimestamp.tick(
                previous = maxHlc,
                nowMs = System.currentTimeMillis(),
                nodeId = deviceIdentity.deviceId
            )
            maxHlc = hlc
            reissued += ChatEvent(
                eventId = UUID.randomUUID().toString(),
                hlc = hlc,
                originDevice = deviceIdentity.deviceId,
                chatId = chatId,
                op = ChatEventOp.EncryptedMessage(
                    innerType = innerType,
                    nonce = sealed.nonce,
                    ciphertext = sealed.ciphertext,
                    messageId = message.id,
                    supersedes = ev.eventId
                ),
                profileId = ev.profileId,
                profileName = ev.profileName
            )
        }

        val migrated = passthrough + reissued
        eventLog.replaceAll(migrated)
        val materialized = MaterializedView.fold(migrated + compactTombstones)
        if (materialized != null) serializer.writeToFile(chatFile, materialized)

        // Live peers get the sealed replacements immediately; reconnect catchup
        // re-streams them too (their HLC is past every peer's watermark).
        reissued.forEach { eventBus.publish(it) }
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
                .normalizedForCompatibility()
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

    private fun isSuppressedByMessageGraveyard(chatDir: File, event: ChatEvent): Boolean {
        val messageId = when (val op = event.op) {
            is ChatEventOp.MessageCreated -> op.message.id
            is ChatEventOp.MessageEdited -> op.message.id
            is ChatEventOp.MessageTombstoned -> op.messageId
            is ChatEventOp.EncryptedMessage -> op.messageId
            else -> null
        } ?: return false
        return MessageTombstoneGraveyard.contains(chatDir, messageId)
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
                    MessageType.FILE -> "📎 ${it.plainTextPreview()}"
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
            protectionSalt = chat.protectionSalt,
            remoteMessagesOnLeft = chat.remoteMessagesOnLeft,
        )
    }
}
