package com.ncy.notechaty.data.sync.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointCandidateTrackerTest {
    @Test
    fun rapidEndpointFlappingDoesNotReplaceFirstCandidate() {
        val tracker = EndpointCandidateTracker(changeCooldownMs = 30_000, candidateTtlMs = 300_000)
        assertTrue(tracker.observe("peer", "10.0.0.2", 8765, now = 1_000))
        assertFalse(tracker.observe("peer", "10.0.0.99", 9999, now = 30_999))
        assertEquals("10.0.0.2", tracker.fresh("peer", now = 31_000)?.host)
    }

    @Test
    fun candidateChangesAfterCooldownAndExpires() {
        val tracker = EndpointCandidateTracker(changeCooldownMs = 30_000, candidateTtlMs = 300_000)
        assertTrue(tracker.observe("peer", "10.0.0.2", 8765, now = 1_000))
        assertTrue(tracker.observe("peer", "10.0.0.3", 8766, now = 31_000))
        assertEquals(8766, tracker.fresh("peer", now = 31_000)?.port)
        assertNull(tracker.fresh("peer", now = 331_001))
    }

    @Test
    fun duplicateRefreshDoesNotExtendChangeCooldown() {
        val tracker = EndpointCandidateTracker(changeCooldownMs = 30_000, candidateTtlMs = 300_000)
        assertTrue(tracker.observe("peer", "10.0.0.2", 8765, now = 1_000))
        assertFalse(tracker.observe("peer", "10.0.0.2", 8765, now = 30_999))
        assertTrue(tracker.observe("peer", "10.0.0.3", 8765, now = 31_000))
    }
}
