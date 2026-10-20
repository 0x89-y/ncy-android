package com.ncy.notechaty.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFailurePolicyTest {
    @Test
    fun revokedAndRejectedCredentialsStopRetriesAndRequestRepair() {
        listOf(
            "pairing_revoked: pair again",
            "bad_auth: HMAC mismatch",
            "auth_failed: Invalid pairing secret",
        ).forEach { reason ->
            assertTrue(SyncFailurePolicy.stopsAutomaticRetry(reason))
            assertEquals(SyncFailurePolicy.REPAIR_MESSAGE, SyncFailurePolicy.userFacingReason(reason))
        }
    }

    @Test
    fun plaintextRejectionsKeepRetryingBecauseAnyoneCanForgeThem() {
        listOf(
            "bad_auth: HMAC mismatch",
            "auth_failed: Invalid pairing secret",
            "pairing_rejected: Pairing rejected on host",
            "identity_collision: spoofed",
        ).forEach { reason ->
            assertFalse(reason, SyncFailurePolicy.stopsAutomaticRetry(reason, authenticated = false))
            assertEquals(
                SyncFailurePolicy.UNVERIFIED_REJECTION_MESSAGE,
                SyncFailurePolicy.userFacingReason(reason, authenticated = false),
            )
        }
        assertEquals("timeout", SyncFailurePolicy.userFacingReason("timeout", authenticated = false))
    }

    @Test
    fun temporaryTransportFailuresRemainRetryable() {
        assertFalse(SyncFailurePolicy.stopsAutomaticRetry("connection refused"))
        assertFalse(SyncFailurePolicy.stopsAutomaticRetry("timeout"))
        assertEquals("timeout", SyncFailurePolicy.userFacingReason("timeout"))
    }

    @Test
    fun hostPairingRejectionStopsAutomaticRetries() {
        val reason = "pairing_rejected: Pairing rejected on host"
        assertTrue(SyncFailurePolicy.stopsAutomaticRetry(reason))
        assertEquals(SyncFailurePolicy.REJECTED_MESSAGE, SyncFailurePolicy.userFacingReason(reason))
    }
}
