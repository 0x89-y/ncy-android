package com.ncy.notechaty.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTimingTest {
    @Test
    fun `background re-lock follows the grace period with a one minute floor`() {
        assertEquals(AppLockCoordinator.MIN_RELOCK_MS, AppLockCoordinator.relockAfterMs(0))
        assertEquals(60_000L, AppLockCoordinator.relockAfterMs(1))
        assertEquals(15 * 60_000L, AppLockCoordinator.relockAfterMs(15))
    }

    @Test
    fun `an unlock counts only inside its grace window`() {
        val now = 10 * 60_000L
        assertTrue(ChatLockState.isWithinGrace(now - 4 * 60_000L, 5, now))
        assertFalse(ChatLockState.isWithinGrace(now - 5 * 60_000L, 5, now))
        // Never unlocked, and "Always" (no grace), both count as locked.
        assertFalse(ChatLockState.isWithinGrace(0L, 5, now))
        assertFalse(ChatLockState.isWithinGrace(now, 0, now))
    }
}
