package com.ncy.notechaty.data.sync.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirrors `automatic_fetch_stops_at_the_backfill_cutoff` in
 * `ncy-core/src/sync/blob.rs`. The cutoff never travels on the wire — each device
 * decides for itself — so the rule is duplicated and both copies need holding still.
 */
class MediaBackfillTest {

    @Test
    fun `history is listed rather than fetched`() {
        val joined = 1_000L
        assertFalse("history was fetched", MediaBackfill.wantsAutomaticFetch(999, joined))
        assertTrue(
            "the join instant is not history",
            MediaBackfill.wantsAutomaticFetch(1_000, joined),
        )
        assertTrue(MediaBackfill.wantsAutomaticFetch(1_001, joined))
    }

    @Test
    fun `an unstamped device still fetches everything`() {
        // Installations that predate the cutoff have already backfilled; switching
        // them over would only strand transfers already in flight.
        for (authored in listOf(0L, 1L, Long.MAX_VALUE)) {
            assertTrue(MediaBackfill.wantsAutomaticFetch(authored, 0L))
            assertTrue(MediaBackfill.wantsAutomaticFetch(authored, -1L))
        }
    }
}
