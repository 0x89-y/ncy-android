package com.ncy.notechaty.data.repository

import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.ProfileChat
import com.ncy.notechaty.data.sync.event.ChatEvent
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Outcome of a [ChatRepository.collectUnreferencedMedia] pass. */
data class MediaCleanupReport(
    val scanned: Int = 0,
    val deleted: Int = 0,
    val reclaimedBytes: Long = 0,
    /** Size of all stored media before any deletion. */
    val totalBytes: Long = 0,
    /** Chats whose events could not be read; non-empty means nothing was deleted. */
    val lockedChats: List<String> = emptyList(),
)

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    suspend fun getChat(chatId: String): Chat?
    suspend fun createChat(name: String): Chat
    suspend fun updateChat(chat: Chat)
    suspend fun deleteChat(chatId: String)

    /**
     * Chats of every profile, kept current while the chat list shows all profiles
     * (empty otherwise). Locked profiles are the caller's to filter out.
     */
    fun observeAllProfileChats(): Flow<List<ProfileChat>>

    /**
     * Ticks after any change to stored chats or messages, here or from a peer. The
     * chat list only re-emits when a chat's summary changes, so starring a message
     * or setting a reminder would otherwise go unnoticed.
     */
    fun observeChanges(): Flow<Long>

    /** Messages of a chat in [profileId], whichever profile is active. */
    suspend fun getMessagesIn(profileId: String, chatId: String): List<Message>

    /**
     * Only the starred messages and those with a reminder, past or upcoming. Cheap to
     * call for every chat after every change: unchanged chats are not read again.
     */
    suspend fun getSavedMessagesIn(profileId: String, chatId: String): List<Message>

    /**
     * Delete a profile and its chats on every paired device: each chat gets a
     * deletion record that sync carries, and the profile cannot come back on its own.
     */
    suspend fun deleteProfileEverywhere(profileId: String)

    suspend fun getMessages(chatId: String): List<Message>
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(chatId: String, messageId: String)

    suspend fun setChatAvatar(chatId: String, sourceFile: File)
    suspend fun setChatBackground(chatId: String, sourceFile: File)
    suspend fun getChatDir(chatId: String): File
    suspend fun getImagesDir(chatId: String): File
    suspend fun getVoiceDir(chatId: String): File
    suspend fun getFilesDir(chatId: String): File

    suspend fun exportChat(chatId: String, outputFile: File)
    suspend fun exportAllChats(outputFile: File)
    suspend fun exportEveryProfile(outputFile: File)
    suspend fun importChat(zipFile: File)
    /** Dangerous: forget exact whole-chat deletion records, allowing stale resurrection. */
    suspend fun purgeChatTombstones(): Int
    suspend fun scanForNewChats()

    /**
     * Apply an event authored by a remote peer (sync inbound). Idempotent: events whose
     * `eventId` already exists in the local log are dropped. Returns true if the event was
     * newly applied (and thus needs an ack), false if it was a duplicate.
     */
    suspend fun applyExternalEvent(event: ChatEvent): Boolean

    /** Map a remote historical profile stamp onto this installation's canonical profile. */
    fun canonicalizeInboundEvent(event: ChatEvent): ChatEvent

    /**
     * Phase 3: like [getMessages] but decrypts any `EncryptedMessage` ops on the fly
     * using the in-memory key cached by `ProtectionKeyStore`. Falls back to placeholders
     * for events the caller doesn't have the key for. Used by the UI when a protected
     * chat is currently unlocked.
     */
    suspend fun getMessagesDecrypted(chatId: String): List<Message>

    /**
     * Phase 3 migration: rewrite any plaintext `MessageCreated`/`MessageEdited` events
     * in a protected chat as encrypted ones, in place (same eventId/HLC). Requires the
     * chat's key to already be cached. Idempotent — already-encrypted events are left
     * untouched. No-op if no key is cached or there's nothing plaintext to convert.
     */
    suspend fun migrateProtectedChat(chatId: String)

    /**
     * Move every protected chat, in every profile, from the key of [oldPin] to the
     * key of [newPin] — messages and attachments — or, with [newPin] null, open
     * them back to plain and unprotect the chats. The PIN is what each chat key is
     * derived from, so changing it without this leaves every protected chat
     * unreadable. Returns how many chats changed.
     */
    suspend fun rekeyProtectedChats(oldPin: String, newPin: String?): Int

    /**
     * Locate a content-addressed media blob by its SHA-256 hash. `mediaType` is `"voice"`,
     * `"image"`, or `"file"`. Returns null if no matching file exists in any chat under the active
     * profile. When [profileId] is null, all local profiles are searched so a peer
     * can request media for a background profile.
     */
    suspend fun mediaFileForHash(hash: String, mediaType: String, profileId: String? = null): File?

    /**
     * Drop this device's copy of a blob while keeping the message.
     *
     * Deleting the file alone is not enough: it is still referenced by a stored event,
     * so the sync layer would notice it missing and fetch it straight back. A marker
     * beside where the blob lived is what makes the choice stick — automatic fetching
     * skips it until the user asks again. Local by design, so an always-on archive
     * peer can hold everything while a phone holds a subset. Returns bytes freed.
     */
    suspend fun evictMedia(hash: String, mediaType: String, profileId: String? = null): Long

    /** True when this device dropped the blob on purpose and should not re-fetch it. */
    suspend fun isMediaEvicted(hash: String, mediaType: String, profileId: String? = null): Boolean

    /** Forget an eviction so automatic fetching may bring the blob back. */
    suspend fun clearMediaEviction(hash: String, mediaType: String, profileId: String? = null)

    /**
     * Delete media no stored event refers to any more, returning the outcome.
     *
     * Nothing has ever removed a blob, so deleting a message leaves its attachment on
     * disk forever — unbounded growth, and a privacy surprise since "deleted" media is
     * still readable. Pass `dryRun` to measure without touching anything.
     *
     * Media lives per chat here, so a chat's blobs are referenced only by that chat's
     * events. A protected chat whose events cannot be read is reported rather than
     * swept: its references are sealed inside the ciphertext, and treating them as
     * absent would delete every attachment it owns.
     */
    suspend fun collectUnreferencedMedia(dryRun: Boolean): MediaCleanupReport

    /**
     * Delete messages past their chat's retention window, returning how many went.
     *
     * Ordinary tombstones, so the deletion syncs exactly like a manual one and every
     * paired device converges. Any device may run this; a peer that already tombstoned
     * the same message absorbs the duplicate. A locked protected chat is skipped: its
     * bodies are placeholders, so `isStarred` cannot be trusted and sweeping blind
     * would delete exactly the messages the rule promises to keep.
     */
    suspend fun applyChatRetention(): Int

    /**
     * Persist a freshly-downloaded media blob into the given chat. The file is named
     * `<hash>.<ext>` under the appropriate subdirectory (`voice/`, `images/`, or `files/`).
     */
    /** Move/copy a verified temporary blob into its canonical content-addressed path. */
    suspend fun saveMediaBlobFromFile(
        chatId: String,
        hash: String,
        mediaType: String,
        sourceFile: File,
        profileId: String? = null,
    )

    fun getBaseDir(): File
}
