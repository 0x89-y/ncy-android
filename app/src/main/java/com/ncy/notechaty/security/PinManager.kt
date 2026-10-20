package com.ncy.notechaty.security

import com.ncy.notechaty.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    /** Only used to re-write a legacy PIN hash after a successful unlock. */
    private val upgradeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun hasPin(): Boolean = settingsRepository.settings.value.protectionPinHash != null

    suspend fun setPin(pin: String) {
        val hashed = PinHasher.newHash(pin)
        val current = settingsRepository.settings.first()
        settingsRepository.updateSettings(
            current.copy(
                protectionPinHash = hashed.hash,
                protectionPinSalt = hashed.saltB64
            )
        )
    }

    fun verifyPin(pin: String): Boolean {
        val s = settingsRepository.settings.value
        val savedHash = s.protectionPinHash ?: return false
        val saltB64 = s.protectionPinSalt ?: return false
        if (PinAttempts.isLockedOut()) return false
        if (!PinHasher.verify(pin, savedHash, saltB64)) {
            PinAttempts.recordFailure()
            return false
        }
        PinAttempts.recordSuccess()
        // Stored under the old bare-SHA-256 scheme. We hold the correct plaintext
        // right now, which is the only moment the hash can be upgraded without
        // asking the user to re-enter it.
        if (PinHasher.needsRehash(savedHash)) {
            upgradeScope.launch { runCatching { setPin(pin) } }
        }
        return true
    }

    suspend fun clearPin() {
        val current = settingsRepository.settings.first()
        settingsRepository.updateSettings(
            current.copy(
                protectionPinHash = null,
                protectionPinSalt = null,
                protectionUseBiometric = false
            )
        )
    }

    suspend fun setUseBiometric(enabled: Boolean) {
        val current = settingsRepository.settings.first()
        settingsRepository.updateSettings(current.copy(protectionUseBiometric = enabled))
    }
}
