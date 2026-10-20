package com.ncy.notechaty.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatLockState @Inject constructor() {

    private val unlockedAt = mutableMapOf<String, Long>()

    private val _timestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val timestamps: StateFlow<Map<String, Long>> = _timestamps.asStateFlow()

    private val _appUnlockedAt = MutableStateFlow(0L)
    val appUnlockedAt: StateFlow<Long> = _appUnlockedAt.asStateFlow()

    @Synchronized
    fun unlock(chatId: String) {
        unlockedAt[chatId] = System.currentTimeMillis()
        _timestamps.value = unlockedAt.toMap()
    }

    @Synchronized
    fun isUnlocked(chatId: String, gracePeriodMinutes: Int): Boolean {
        if (gracePeriodMinutes <= 0) {
            // Per-entry policy: one-shot token, consume on read.
            // App-wide unlock does not count here — every chat entry re-prompts.
            val removed = unlockedAt.remove(chatId) != null
            if (removed) _timestamps.value = unlockedAt.toMap()
            return removed
        }
        val now = System.currentTimeMillis()
        // The app unlock counts for the same grace window as a chat's own unlock.
        // It used to count forever, so one PIN at start opened every protected chat
        // for as long as the process lived.
        if (isWithinGrace(_appUnlockedAt.value, gracePeriodMinutes, now)) return true
        val ts = unlockedAt[chatId] ?: return false
        val ok = isWithinGrace(ts, gracePeriodMinutes, now)
        if (!ok) {
            unlockedAt.remove(chatId)
            _timestamps.value = unlockedAt.toMap()
        }
        return ok
    }

    @Synchronized
    fun unlockApp() {
        _appUnlockedAt.value = System.currentTimeMillis()
    }

    @Synchronized
    fun isAppUnlocked(): Boolean = _appUnlockedAt.value > 0L

    @Synchronized
    fun lockAll() {
        unlockedAt.clear()
        _timestamps.value = emptyMap()
        _appUnlockedAt.value = 0L
    }

    companion object {
        fun isWithinGrace(unlockedAt: Long, gracePeriodMinutes: Int, now: Long): Boolean =
            unlockedAt > 0L && now - unlockedAt < gracePeriodMinutes * 60_000L
    }

    /** Clear per-chat unlocks without touching the app-wide unlock. Used on profile switch. */
    @Synchronized
    fun clearChatUnlocks() {
        unlockedAt.clear()
        _timestamps.value = emptyMap()
    }
}
