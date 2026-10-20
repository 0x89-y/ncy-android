package com.ncy.notechaty.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeIdTest {
    @Test
    fun `ids minted by android and desktop are accepted`() {
        assertTrue(SafeId.isValid("8e2bd4f3-2b13-4a4a-9b2c-a1b0d8e2f4a6"))
        assertTrue(SafeId.isValid("legacy_chat-1"))
    }

    @Test
    fun `anything that could leave its directory is rejected`() {
        listOf(
            null,
            "",
            "..",
            "/../../shared_prefs",
            "a/b",
            "a\\b",
            "x".repeat(129),
        ).forEach { assertFalse("accepted '$it'", SafeId.isValid(it)) }
    }
}
