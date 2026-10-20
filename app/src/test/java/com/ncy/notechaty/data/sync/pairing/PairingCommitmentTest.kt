package com.ncy.notechaty.data.sync.pairing

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class PairingCommitmentTest {
    /** Shared with ncy-core's `pair_crypto` tests, so both ends provably agree. */
    @Test
    fun `commitment matches the cross-platform vector`() {
        val key = Base64.getDecoder().decode(
            "BAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A="
        )
        assertEquals(
            "F/bMjAEKar4bTgeyp3eRau0GkouVAh5X77dbhVN7b3U=",
            PairingClient.commitToPubKey(key),
        )
    }
}
