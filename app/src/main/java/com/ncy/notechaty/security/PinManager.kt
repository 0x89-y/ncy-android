package com.ncy.notechaty.security

import com.ncy.notechaty.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    fun hasPin(): Boolean = settingsRepository.settings.value.protectionPinHash != null

    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = encode(salt)
        val hash = hash(pin, salt)
        val current = settingsRepository.settings.first()
        settingsRepository.updateSettings(
            current.copy(
                protectionPinHash = hash,
                protectionPinSalt = saltB64
            )
        )
    }

    fun verifyPin(pin: String): Boolean {
        val s = settingsRepository.settings.value
        val savedHash = s.protectionPinHash ?: return false
        val saltB64 = s.protectionPinSalt ?: return false
        val salt = decode(saltB64) ?: return false
        return hash(pin, salt) == savedHash
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

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return encode(md.digest())
    }

    private fun encode(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decode(s: String): ByteArray? = try {
        android.util.Base64.decode(s, android.util.Base64.NO_WRAP)
    } catch (_: IllegalArgumentException) {
        null
    }
}
