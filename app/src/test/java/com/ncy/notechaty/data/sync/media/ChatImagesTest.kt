package com.ncy.notechaty.data.sync.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ChatImagesTest {
    @get:Rule val tmp = TemporaryFolder()

    private val hash = "a".repeat(64)

    private fun chatDir(): File = tmp.newFolder("chat_1")

    @Test
    fun `a picture set on this device opens at its named path`() {
        val dir = chatDir()
        val file = File(dir, "avatar/$hash.jpg").apply { parentFile!!.mkdirs(); writeText("x") }
        assertEquals(file, ChatImages.resolve(dir, "avatar/$hash.jpg"))
    }

    @Test
    fun `a picture that arrived by sync is found under images`() {
        val dir = chatDir()
        val file = File(dir, "images/$hash.jpg").apply { parentFile!!.mkdirs(); writeText("x") }
        assertEquals(file, ChatImages.resolve(dir, "avatar/$hash.jpg"))
        assertEquals(file, ChatImages.resolve(dir, "background/$hash.jpg"))
    }

    @Test
    fun `a picture not on this device resolves to nothing`() {
        val dir = chatDir()
        assertNull(ChatImages.resolve(dir, "avatar/$hash.jpg"))
        assertNull(ChatImages.resolve(dir, null))
    }
}
