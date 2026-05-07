package com.ncy.notechaty.security

import com.ncy.notechaty.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

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
        return PinHasher.verify(pin, savedHash, saltB64)
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
