package com.ncy.notechaty.data.sync.protection

import com.ncy.notechaty.data.model.FileAttachment
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.sync.media.MediaHash
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProtectedMediaTest {
    @get:Rule val temp = TemporaryFolder()

    private val bytes = "a voice note".toByteArray()

    private fun chatWithVoice(): Pair<File, Message> {
        val chatDir = temp.newFolder("chat_c1")
        val voiceDir = File(chatDir, "voice").apply { mkdirs() }
        val plain = File(voiceDir, "x.part").apply { writeBytes(bytes) }
        val hash = MediaHash.ofFile(plain)
        plain.renameTo(File(voiceDir, "$hash.m4a"))
        return chatDir to Message(
            id = "m1",
            chatId = "c1",
            createdAt = 1L,
            voiceFile = "voice/$hash.m4a",
            fileAttachments = listOf(FileAttachment(file = "files/${"0".repeat(64)}.bin", name = "absent")),
        )
    }

    private fun open(key: ByteArray, file: File): ByteArray =
        ByteArrayOutputStream().also { SealedBlob.open(key, file.inputStream(), file.length(), it) }.toByteArray()

    @Test
    fun `sealing rewrites the reference, removes the plain copy and leaves absent media alone`() {
        val (chatDir, message) = chatWithVoice()
        val key = ByteArray(32) { 1 }
        val sealed = ProtectedMedia.rewrite(chatDir, message, key)

        assertNotEquals(message.voiceFile, sealed.voiceFile)
        val sealedFile = File(chatDir, sealed.voiceFile!!)
        assertTrue(SealedBlob.isSealed(sealedFile))
        assertEquals("named by the hash of the sealed bytes", MediaHash.ofFile(sealedFile), MediaHash.extractHash(sealed.voiceFile))
        assertFalse("no plain copy outlives protection", File(chatDir, message.voiceFile!!).exists())
        assertEquals(message.fileAttachments, sealed.fileAttachments)
        assertArrayEquals(bytes, open(key, sealedFile))

        val listed = ProtectedMedia.blobRefs(chatDir, sealed)
        assertEquals(sealedFile.length(), listed.single { it.mediaType == "voice" }.sizeBytes)
    }

    @Test
    fun `a PIN change re-seals and removing protection opens it back`() {
        val (chatDir, message) = chatWithVoice()
        val (oldKey, newKey) = ByteArray(32) { 1 } to ByteArray(32) { 2 }
        val underOld = ProtectedMedia.rewrite(chatDir, message, oldKey)
        val underNew = ProtectedMedia.rewrite(chatDir, underOld, newKey, oldKey)
        assertNotEquals(underOld.voiceFile, underNew.voiceFile)
        assertArrayEquals(bytes, open(newKey, File(chatDir, underNew.voiceFile!!)))

        val plain = ProtectedMedia.rewrite(chatDir, underNew, null, newKey)
        assertEquals("back under its original hash", message.voiceFile, plain.voiceFile)
        assertArrayEquals(bytes, File(chatDir, plain.voiceFile!!).readBytes())
    }
}
