package com.ncy.notechaty.data.sync.media

/**
 * Whether media on an event should be fetched without the user asking for it.
 *
 * A deliberate mirror of the desktop's `wants_automatic_fetch` in
 * `ncy-core/src/sync/blob.rs`. Both ends decide this independently — the cutoff is
 * installation-local and never travels on the wire — so the rule is duplicated
 * rather than shared. Keep the two in step.
 */
object MediaBackfill {

    /**
     * @param eventMs when the event was authored (its HLC's physical time).
     * @param cutoffMs when this device joined the mesh; zero disables the cutoff.
     */
    fun wantsAutomaticFetch(eventMs: Long, cutoffMs: Long): Boolean =
        cutoffMs <= 0L || eventMs >= cutoffMs
}
