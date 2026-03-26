package com.ncy.notechaty.data.repository

import android.content.Context
import com.ncy.notechaty.data.model.*
import com.ncy.notechaty.data.serialization.ChatSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    @ApplicationContext private val context: Context
) : ChatRepository {

    private val serializer = ChatSerializer()
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())

    private val rootDir: File
        get() {
            val dir = File(context.filesDir, "NoteChaty")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    override fun getBaseDir(): File = rootDir

    override fun observeChats(): Flow<List<Chat>> = _chats.asStateFlow()

    override suspend fun getChat(chatId: String): Chat? = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_$chatId")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext null
        val data = serializer.readFromFile(chatFile)
        data.toChat(chatId, data.messages)
    }

    override suspend fun createChat(name: String): Chat = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val chatDir = File(rootDir, "chat_$id")
        chatDir.mkdirs()
        File(chatDir, "images").mkdirs()
        File(chatDir, "voice").mkdirs()

        val chatData = ChatData(
            version = 2,
            chat = ChatMetadata(
                id = id,
                name = name,
                createdAt = System.currentTimeMillis()
            )
        )
        serializer.writeToFile(File(chatDir, "chat.json"), chatData)

        val chat = Chat(id = id, name = name, createdAt = chatData.chat.createdAt)
        refreshChats()
        chat
    }

    override suspend fun updateChat(chat: Chat): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_${chat.id}")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext

        val data = serializer.readFromFile(chatFile)
        val updatedData = data.copy(
            chat = data.chat.copy(
                name = chat.name,
                avatarFile = chat.avatarFile,
                backgroundFile = chat.backgroundFile,
                isPinned = chat.isPinned,
                bubbleColor = chat.bubbleColor,
                bubbleGradient = chat.bubbleGradient,
                tags = chat.tags
            )
        )
        serializer.writeToFile(chatFile, updatedData)
        refreshChats()
    }

    override suspend fun deleteChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_$chatId")
        chatDir.deleteRecursively()
        refreshChats()
    }

    override suspend fun getMessages(chatId: String): List<Message> = withContext(Dispatchers.IO) {
        val chatFile = File(File(rootDir, "chat_$chatId"), "chat.json")
        if (!chatFile.exists()) return@withContext emptyList()
        serializer.readFromFile(chatFile).messages
    }

    override suspend fun addMessage(message: Message): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_${message.chatId}")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext

        val data = serializer.readFromFile(chatFile)
        val updatedData = data.copy(messages = data.messages + message)
        serializer.writeToFile(chatFile, updatedData)
        refreshChats()
    }

    override suspend fun updateMessage(message: Message): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_${message.chatId}")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext

        val data = serializer.readFromFile(chatFile)
        val updatedMessages = data.messages.map {
            if (it.id == message.id) message else it
        }
        serializer.writeToFile(chatFile, data.copy(messages = updatedMessages))
        refreshChats()
    }

    override suspend fun deleteMessage(chatId: String, messageId: String): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_$chatId")
        val chatFile = File(chatDir, "chat.json")
        if (!chatFile.exists()) return@withContext

        val data = serializer.readFromFile(chatFile)
        val updatedMessages = data.messages.filter { it.id != messageId }
        serializer.writeToFile(chatFile, data.copy(messages = updatedMessages))
        refreshChats()
    }

    override suspend fun setChatAvatar(chatId: String, sourceFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_$chatId")
        val avatarFile = File(chatDir, "avatar.jpg")
        sourceFile.copyTo(avatarFile, overwrite = true)

        val chatFile = File(chatDir, "chat.json")
        val data = serializer.readFromFile(chatFile)
        serializer.writeToFile(chatFile, data.copy(chat = data.chat.copy(avatarFile = "avatar.jpg")))
        refreshChats()
    }

    override suspend fun setChatBackground(chatId: String, sourceFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_$chatId")
        val bgFile = File(chatDir, "background.jpg")
        sourceFile.copyTo(bgFile, overwrite = true)

        val chatFile = File(chatDir, "chat.json")
        val data = serializer.readFromFile(chatFile)
        serializer.writeToFile(chatFile, data.copy(chat = data.chat.copy(backgroundFile = "background.jpg")))
        refreshChats()
    }

    override suspend fun getChatDir(chatId: String): File = File(rootDir, "chat_$chatId")

    override suspend fun getImagesDir(chatId: String): File {
        val dir = File(File(rootDir, "chat_$chatId"), "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override suspend fun getVoiceDir(chatId: String): File {
        val dir = File(File(rootDir, "chat_$chatId"), "voice")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override suspend fun exportChat(chatId: String, outputFile: File): Unit = withContext(Dispatchers.IO) {
        val chatDir = File(rootDir, "chat_$chatId")
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
            rootDir.listFiles()
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

    override suspend fun importChat(zipFile: File): Unit = withContext(Dispatchers.IO) {
        val tempDir = File(rootDir, "import_temp_${System.currentTimeMillis()}")
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
                val targetDir = File(rootDir, "chat_$newId")

                tempDir.renameTo(targetDir)

                val updatedData = data.copy(
                    chat = data.chat.copy(id = newId),
                    messages = data.messages.map { it.copy(chatId = newId) }
                )
                serializer.writeToFile(File(targetDir, "chat.json"), updatedData)
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
        val chats = rootDir.listFiles()
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
                    MessageType.VOICE -> "\uD83C\uDFA4 Voice message"
                    MessageType.IMAGE -> "\uD83D\uDDBC Image"
                    MessageType.TEXT -> it.plainTextPreview()
                }
            },
            isPinned = chat.isPinned,
            bubbleColor = chat.bubbleColor,
            bubbleGradient = chat.bubbleGradient,
            tags = chat.tags
        )
    }
}
