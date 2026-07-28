package com.ncy.notechaty.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * A backup zip is untrusted input. These pin the two properties extraction relies
 * on: entries cannot escape the sandbox, and they cannot expand without bound.
 */
class BackupArchiveTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun root() = temp.newFolder("import_temp")

    @Test
    fun `accepts the entry shapes the exporters actually produce`() {
        val root = root()
        for (name in listOf(
            "events.ndjson",
            "profiles.json",
            "chat_abc/events.ndjson",
            "profile_1/chat_2/blobs/deadbeef.jpg",
        )) {
            val resolved = BackupArchive.safeTarget(root, name)
            assertNotNull("expected $name to be accepted", resolved)
            assertTrue(resolved!!.canonicalPath.startsWith(root.canonicalPath))
        }
    }

    @Test
    fun `rejects traversal out of the sandbox`() {
        val root = root()
        for (name in listOf(
            "../escaped.txt",
            "../../shared_prefs/ncy_phone_pairing.xml",
            "chat_a/../../escaped.txt",
            "a/b/../../../escaped.txt",
            "..",
        )) {
            assertNull("expected $name to be rejected", BackupArchive.safeTarget(root, name))
        }
    }

    @Test
    fun `rejects backslash traversal and absolute paths`() {
        val root = root()
        for (name in listOf(
            "..\\escaped.txt",
            "..\\..\\shared_prefs\\ncy_device.xml",
            "/etc/passwd",
            "C:/Windows/System32/drivers/etc/hosts",
            "C:\\Windows\\hosts",
            "",
        )) {
            assertNull("expected '$name' to be rejected", BackupArchive.safeTarget(root, name))
        }
    }

    @Test
    fun `rejects a sibling directory sharing the sandbox name prefix`() {
        // The bug a plain string-prefix containment check would let through.
        val root = root()
        assertNull(BackupArchive.safeTarget(root, "../import_temp_evil/x.txt"))
    }

    @Test
    fun `copies content within the limit`() {
        val payload = ByteArray(4096) { (it % 251).toByte() }
        val out = ByteArrayOutputStream()
        val written = BackupArchive.copyAtMost(ByteArrayInputStream(payload), out, payload.size.toLong())
        assertEquals(payload.size.toLong(), written)
        assertTrue(payload.contentEquals(out.toByteArray()))
    }

    @Test
    fun `fails instead of truncating when the limit is exceeded`() {
        val payload = ByteArray(8192)
        assertThrows(IllegalStateException::class.java) {
            BackupArchive.copyAtMost(ByteArrayInputStream(payload), ByteArrayOutputStream(), 4096)
        }
    }

    @Test
    fun `fails when the budget is already exhausted`() {
        assertThrows(IllegalStateException::class.java) {
            BackupArchive.copyAtMost(ByteArrayInputStream(ByteArray(1)), ByteArrayOutputStream(), 0)
        }
    }
}
