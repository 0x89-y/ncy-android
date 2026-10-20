package com.ncy.notechaty.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAttemptsTest {
    @Test
    fun `the first few wrong PINs cost nothing`() {
        (0..PinAttempts.FREE_ATTEMPTS).forEach {
            assertEquals("after $it failures", 0L, PinAttempts.lockoutMsAfter(it))
        }
    }

    @Test
    fun `each further wrong PIN doubles the wait up to the ceiling`() {
        val first = PinAttempts.FREE_ATTEMPTS + 1
        assertEquals(PinAttempts.FIRST_LOCKOUT_MS, PinAttempts.lockoutMsAfter(first))
        assertEquals(PinAttempts.FIRST_LOCKOUT_MS * 2, PinAttempts.lockoutMsAfter(first + 1))
        assertEquals(PinAttempts.FIRST_LOCKOUT_MS * 4, PinAttempts.lockoutMsAfter(first + 2))
        assertEquals(PinAttempts.MAX_LOCKOUT_MS, PinAttempts.lockoutMsAfter(first + 50))
        assertEquals(PinAttempts.MAX_LOCKOUT_MS, PinAttempts.lockoutMsAfter(Int.MAX_VALUE))
    }
}
