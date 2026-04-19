package com.ncy.notechaty.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatLockState @Inject constructor() {

    private val unlockedAt = mutableMapOf<String, Long>()
    private var appUnlockedAt: Long = 0L

    @Synchronized
    fun unlock(chatId: String) {
        unlockedAt[chatId] = System.currentTimeMillis()
    }

    @Synchronized
    fun isUnlocked(chatId: String, gracePeriodMinutes: Int): Boolean {
        if (gracePeriodMinutes <= 0) {
            // Per-entry policy: one-shot token, consume on read.
            // App-wide unlock does not count here — every chat entry re-prompts.
            return unlockedAt.remove(chatId) != null
        }
        if (appUnlockedAt > 0L) return true
        val ts = unlockedAt[chatId] ?: return false
        val ok = System.currentTimeMillis() - ts < gracePeriodMinutes * 60_000L
        if (!ok) unlockedAt.remove(chatId)
        return ok
    }

    @Synchronized
    fun unlockApp() {
        appUnlockedAt = System.currentTimeMillis()
    }

    @Synchronized
    fun isAppUnlocked(): Boolean = appUnlockedAt > 0L

    @Synchronized
    fun lockAll() {
        unlockedAt.clear()
        appUnlockedAt = 0L
    }
}
