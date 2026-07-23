package com.ncy.notechaty.data.serialization

import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.ChatMetadata
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class ChatSerializerTest {
    @Test
    fun readersNeverObserveTruncatedChatDuringConcurrentWrites() {
        val dir = Files.createTempDirectory("ncy-chat-serializer").toFile()
        val file = dir.resolve("chat.json")
        val serializer = ChatSerializer()
        serializer.writeToFile(file, data("initial"))
        val failure = AtomicReference<Throwable?>()

        val writer = thread {
            runCatching {
                repeat(500) { serializer.writeToFile(file, data("chat-$it")) }
            }.onFailure { failure.compareAndSet(null, it) }
        }
        val reader = thread {
            runCatching {
                while (writer.isAlive) {
                    serializer.readFromFile(file)
                }
            }.onFailure { failure.compareAndSet(null, it) }
        }

        writer.join()
        reader.join()
        failure.get()?.let { throw it }
        assertEquals("chat-499", serializer.readFromFile(file).chat.name)
        dir.deleteRecursively()
    }

    private fun data(name: String) = ChatData(
        chat = ChatMetadata(id = "chat-id", name = name, createdAt = 1L),
        messages = emptyList(),
    )
}
