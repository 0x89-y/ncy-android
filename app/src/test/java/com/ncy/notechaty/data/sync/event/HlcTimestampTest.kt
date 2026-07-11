package com.ncy.notechaty.data.sync.event

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlcTimestampTest {
    @Test
    fun acceptsBoundaryButRejectsFarFuture() {
        val now = 1_000_000L
        assertTrue(HlcTimestamp.isAcceptableRemote(
            HlcTimestamp(now + HlcTimestamp.MAX_FUTURE_SKEW_MS, 0, "peer"), now
        ))
        assertFalse(HlcTimestamp.isAcceptableRemote(
            HlcTimestamp(now + HlcTimestamp.MAX_FUTURE_SKEW_MS + 1, 0, "peer"), now
        ))
    }

    @Test
    fun rejectsMalformedRemoteClockParts() {
        assertFalse(HlcTimestamp.isAcceptableRemote(HlcTimestamp(-1, 0, "peer"), 0))
        assertFalse(HlcTimestamp.isAcceptableRemote(HlcTimestamp(0, -1, "peer"), 0))
        assertFalse(HlcTimestamp.isAcceptableRemote(HlcTimestamp(0, 0, ""), 0))
    }
}
