package com.ncy.notechaty.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileLockState @Inject constructor() {

    private val unlockedAt = mutableMapOf<String, Long>()

    private val _timestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val timestamps: StateFlow<Map<String, Long>> = _timestamps.asStateFlow()

    @Synchronized
    fun unlock(profileId: String) {
        unlockedAt[profileId] = System.currentTimeMillis()
        _timestamps.value = unlockedAt.toMap()
    }

    @Synchronized
    fun isUnlocked(profileId: String, gracePeriodMinutes: Int): Boolean {
        if (gracePeriodMinutes <= 0) {
            // One-shot: every switch re-prompts.
            val removed = unlockedAt.remove(profileId) != null
            if (removed) _timestamps.value = unlockedAt.toMap()
            return removed
        }
        val ts = unlockedAt[profileId] ?: return false
        val ok = System.currentTimeMillis() - ts < gracePeriodMinutes * 60_000L
        if (!ok) {
            unlockedAt.remove(profileId)
            _timestamps.value = unlockedAt.toMap()
        }
        return ok
    }

    @Synchronized
    fun lockAll() {
        unlockedAt.clear()
        _timestamps.value = emptyMap()
    }
}
