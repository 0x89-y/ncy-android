package com.ncy.notechaty.data.sync.transport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCatchupPolicyTest {
    @Test
    fun fullReplayIncludesEventsOriginallyAuthoredByReinstalledDevice() {
        assertTrue(SyncCatchupPolicy.shouldSend("phone", "phone", fullReplay = true))
    }

    @Test
    fun incrementalReplayStillPreventsOriginLoop() {
        assertFalse(SyncCatchupPolicy.shouldSend("phone", "phone", fullReplay = false))
        assertTrue(SyncCatchupPolicy.shouldSend("desktop", "phone", fullReplay = false))
    }

    @Test
    fun snapshotLiveOverlapIsDeduplicatedByEventIdNotWatermark() {
        val sent = mutableSetOf<String>()
        assertTrue(SyncCatchupPolicy.markForSend("old", "desktop", "phone", true, sent))
        assertFalse(SyncCatchupPolicy.markForSend("old", "desktop", "phone", false, sent))
        // A distinct queued event must still be sent even if its HLC would sort
        // below another snapshot event; event identity, not a scalar watermark,
        // decides handoff overlap.
        assertTrue(SyncCatchupPolicy.markForSend("late-append", "desktop", "phone", false, sent))
    }

}
