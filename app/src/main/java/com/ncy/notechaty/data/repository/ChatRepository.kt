package com.ncy.notechaty.data.repository

import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.Message
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

    fun getBaseDir(): File
}
