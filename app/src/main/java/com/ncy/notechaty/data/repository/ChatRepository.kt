package com.ncy.notechaty.data.repository

import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.sync.event.ChatEvent
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    suspend fun getChat(chatId: String): Chat?
    suspend fun createChat(name: String): Chat
    suspend fun updateChat(chat: Chat)
    suspend fun deleteChat(chatId: String)

    suspend fun getMessages(chatId: String): List<Message>
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(chatId: String, messageId: String)

    suspend fun setChatAvatar(chatId: String, sourceFile: File)
    suspend fun setChatBackground(chatId: String, sourceFile: File)
    suspend fun getChatDir(chatId: String): File
    suspend fun getImagesDir(chatId: String): File
    suspend fun getVoiceDir(chatId: String): File

    suspend fun exportChat(chatId: String, outputFile: File)
    suspend fun exportAllChats(outputFile: File)
    suspend fun exportEveryProfile(outputFile: File)
    suspend fun importChat(zipFile: File)
    suspend fun scanForNewChats()

    /**
     * Apply an event authored by a remote peer (sync inbound). Idempotent: events whose
     * `eventId` already exists in the local log are dropped. Returns true if the event was
     * newly applied (and thus needs an ack), false if it was a duplicate.
     */
    suspend fun applyExternalEvent(event: ChatEvent): Boolean

    /**
     * Locate a content-addressed media blob by its SHA-256 hash. `mediaType` is `"voice"`
     * or `"image"`. Returns null if no matching file exists in any chat under the active
     * profile.
     */
    suspend fun mediaFileForHash(hash: String, mediaType: String): File?

    /**
     * Persist a freshly-downloaded media blob into the given chat. The file is named
     * `<hash>.<ext>` under the appropriate subdirectory (`voice/` or `images/`).
     */
    suspend fun saveMediaBlob(chatId: String, hash: String, mediaType: String, bytes: ByteArray)

    fun getBaseDir(): File
}
