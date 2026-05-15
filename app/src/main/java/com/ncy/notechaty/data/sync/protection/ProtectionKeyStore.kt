package com.ncy.notechaty.data.sync.protection

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds derived AES-256 keys for protected chats in memory only, keyed by chatId.
 * Keys are derived via PBKDF2 from the user's PIN plus the chat's `protectionSalt`
 * at unlock time and cleared on lock — never persisted.
 *
 * This is the bridge between the UI (which has the live PIN at the moment of entry
 * and the salt from `ChatMetadata`) and the repository (which needs the raw 32-byte
 * key to encrypt/decrypt event payloads but should never see the PIN itself).
 */
@Singleton
class ProtectionKeyStore @Inject constructor() {

    private val keys = mutableMapOf<String, ByteArray>()

    /**
     * Derive and cache the key for [chatId] from [pin] + [saltB64]. Idempotent —
     * re-deriving with the same inputs replaces the cached value. Returns the key
     * for callers that want to use it immediately without a second lookup.
     */
    @Synchronized
    fun unlock(chatId: String, pin: String, saltB64: String): ByteArray {
        val key = ProtectionCrypto.deriveKey(pin, saltB64)
        keys[chatId] = key
        return key
    }

    @Synchronized
    fun keyFor(chatId: String): ByteArray? = keys[chatId]

    @Synchronized
    fun isUnlocked(chatId: String): Boolean = chatId in keys

    @Synchronized
    fun lock(chatId: String) {
        keys.remove(chatId)?.fill(0)
    }

    @Synchronized
    fun lockAll() {
        keys.values.forEach { it.fill(0) }
        keys.clear()
    }
}
