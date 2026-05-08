package com.ncy.notechaty.data.sync.event

/**
 * Hybrid Logical Clock (Kulkarni et al.). Used to order events deterministically
 * across devices that may be offline. Comparison is (physicalMs, logical, nodeId).
 */
data class HlcTimestamp(
    val physicalMs: Long,
    val logical: Int,
    val nodeId: String
) : Comparable<HlcTimestamp> {

    override fun compareTo(other: HlcTimestamp): Int {
        if (physicalMs != other.physicalMs) return physicalMs.compareTo(other.physicalMs)
        if (logical != other.logical) return logical.compareTo(other.logical)
        return nodeId.compareTo(other.nodeId)
    }

    companion object {
        /** Generate the next local timestamp given the previous local clock and current wall-clock. */
        fun tick(previous: HlcTimestamp?, nowMs: Long, nodeId: String): HlcTimestamp {
            val prev = previous ?: HlcTimestamp(0L, 0, nodeId)
            return if (nowMs > prev.physicalMs) {
                HlcTimestamp(nowMs, 0, nodeId)
            } else {
                HlcTimestamp(prev.physicalMs, prev.logical + 1, nodeId)
            }
        }

        /** Merge a remote timestamp into the local clock and return the new local timestamp. */
        fun receive(
            local: HlcTimestamp?,
            remote: HlcTimestamp,
            nowMs: Long,
            nodeId: String
        ): HlcTimestamp {
            val l = local ?: HlcTimestamp(0L, 0, nodeId)
            val maxPhysical = maxOf(nowMs, l.physicalMs, remote.physicalMs)
            val logical = when (maxPhysical) {
                l.physicalMs -> if (maxPhysical == remote.physicalMs)
                    maxOf(l.logical, remote.logical) + 1 else l.logical + 1
                remote.physicalMs -> remote.logical + 1
                else -> 0
            }
            return HlcTimestamp(maxPhysical, logical, nodeId)
        }
    }
}
