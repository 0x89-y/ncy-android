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
        if (_appUnlockedAt.value > 0L) return true
        val ts = unlockedAt[chatId] ?: return false
        val ok = System.currentTimeMillis() - ts < gracePeriodMinutes * 60_000L
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

    /** Clear per-chat unlocks without touching the app-wide unlock. Used on profile switch. */
    @Synchronized
    fun clearChatUnlocks() {
        unlockedAt.clear()
        _timestamps.value = emptyMap()
    }
}
