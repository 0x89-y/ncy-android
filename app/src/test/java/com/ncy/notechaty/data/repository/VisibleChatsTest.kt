package com.ncy.notechaty.data.repository

import com.ncy.notechaty.data.model.Profile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What "All chats" may show from a protected profile. */
class VisibleChatsTest {

    private val now = 1_000_000_000L
    private val open = Profile(id = "open", name = "Personal")
    private val locked = Profile(id = "locked", name = "Work", pinHash = "hash")

    @Test
    fun `an unprotected profile is always shown`() {
        assertTrue(VisibleChats.isProfileOpen(open, "other", emptyMap(), 0, now))
    }

    @Test
    fun `a protected profile is shown while it is the active one`() {
        assertTrue(VisibleChats.isProfileOpen(locked, "locked", emptyMap(), 0, now))
    }

    @Test
    fun `a protected profile never unlocked is hidden`() {
        assertFalse(VisibleChats.isProfileOpen(locked, "open", emptyMap(), 5, now))
    }

    @Test
    fun `a protected profile is shown inside its grace period and hidden after it`() {
        val unlocked = mapOf("locked" to now - 4 * 60_000L)
        assertTrue(VisibleChats.isProfileOpen(locked, "open", unlocked, 5, now))
        assertFalse(VisibleChats.isProfileOpen(locked, "open", unlocked, 3, now))
    }

    @Test
    fun `with no grace period an earlier unlock does not keep it shown`() {
        val unlocked = mapOf("locked" to now - 1_000L)
        assertFalse(VisibleChats.isProfileOpen(locked, "open", unlocked, 0, now))
    }
}
