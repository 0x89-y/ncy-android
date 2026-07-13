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
    fun temporaryTransportFailuresRemainRetryable() {
        assertFalse(SyncFailurePolicy.stopsAutomaticRetry("connection refused"))
        assertFalse(SyncFailurePolicy.stopsAutomaticRetry("timeout"))
        assertEquals("timeout", SyncFailurePolicy.userFacingReason("timeout"))
    }
}
