package com.ncy.notechaty.data.sync.discovery

/**
 * Holds untrusted mDNS endpoints in memory until the sync handshake authenticates
 * them. Rapid changes for one stable device ID are ignored to limit spoofing and
 * endpoint-flapping denial of service.
 */
class EndpointCandidateTracker(
    private val changeCooldownMs: Long = CHANGE_COOLDOWN_MS,
    private val candidateTtlMs: Long = CANDIDATE_TTL_MS,
) {
    data class Candidate(
        val host: String,
        val port: Int,
        val changedAt: Long,
        val observedAt: Long,
    )

    private val candidates = mutableMapOf<String, Candidate>()

    @Synchronized
    fun observe(
        deviceId: String,
        host: String,
        port: Int,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (host.isBlank() || port !in 1..65535) return false
        val current = candidates[deviceId]
        if (current != null) {
            if (current.host == host && current.port == port) {
                candidates[deviceId] = current.copy(
                    observedAt = now,
                )
                return false
            }
            if (now - current.changedAt < changeCooldownMs) return false
        }
        candidates[deviceId] = Candidate(host, port, changedAt = now, observedAt = now)
        return true
    }

    @Synchronized
    fun fresh(deviceId: String, now: Long = System.currentTimeMillis()): Candidate? {
        val candidate = candidates[deviceId] ?: return null
        if (now - candidate.observedAt > candidateTtlMs) {
            candidates.remove(deviceId)
            return null
        }
        return candidate
    }

    @Synchronized
    fun remove(deviceId: String) {
        candidates.remove(deviceId)
    }

    companion object {
        const val CHANGE_COOLDOWN_MS = 30_000L
        const val CANDIDATE_TTL_MS = 5 * 60_000L
    }
}
