package com.ncy.notechaty.data.serialization

import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.ChatMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ChatFileCacheTest {

    @get:Rule val folder = TemporaryFolder()

    private val serializer = ChatSerializer()

    private fun write(file: File, name: String) =
        serializer.writeToFile(file, ChatData(chat = ChatMetadata(id = "c", name = name, createdAt = 0L)))

    @Test
    fun `an unchanged file is read once`() {
        val file = File(folder.root, "chat.json").also { write(it, "Groceries") }
        val cache = ChatFileCache<String>()
        var reads = 0
        repeat(3) { cache.get(file) { reads++; serializer.readFromFile(file).chat.name } }
        assertEquals(1, reads)
    }

    @Test
    fun `a write through the serializer is noticed even at the same size and time`() {
        val file = File(folder.root, "chat.json").also { write(it, "Groceries") }
        val cache = ChatFileCache<String>()
        val read = { cache.get(file) { serializer.readFromFile(file).chat.name } }
        assertEquals("Groceries", read())
        val modified = file.lastModified()
        // Same length, and the clock put back: only the write count tells them apart.
        write(file, "Groceriez")
        file.setLastModified(modified)
        assertEquals("Groceriez", read())
    }

    @Test
    fun `a file replaced from elsewhere is noticed by its size`() {
        val file = File(folder.root, "chat.json").also { write(it, "Groceries") }
        val cache = ChatFileCache<String>()
        val read = { cache.get(file) { serializer.readFromFile(file).chat.name } }
        read()
        val other = File(folder.root, "other.json").also { write(it, "Shopping list") }
        file.writeText(other.readText())
        assertEquals("Shopping list", read())
    }

    @Test
    fun `a missing file is not cached`() {
        val file = File(folder.root, "chat.json")
        val cache = ChatFileCache<String>()
        assertNull(cache.get(file) { null })
        write(file, "Rebuilt")
        assertEquals("Rebuilt", cache.get(file) { serializer.readFromFile(file).chat.name })
    }
}
