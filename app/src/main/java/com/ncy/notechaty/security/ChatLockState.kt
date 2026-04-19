package com.ncy.notechaty.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatLockState @Inject constructor() {

    private val unlocked = mutableSetOf<String>()

    @Synchronized
    fun unlock(chatId: String) {
        unlocked.add(chatId)
    }

    @Synchronized
    fun consume(chatId: String): Boolean {
        return unlocked.remove(chatId)
    }

    @Synchronized
    fun lockAll() {
        unlocked.clear()
    }
}
