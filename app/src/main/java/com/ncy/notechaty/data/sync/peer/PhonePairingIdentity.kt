package com.ncy.notechaty.data.sync.peer

import android.content.Context
import android.util.Base64
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.DeviceIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Persistent server-side identity used when this phone hosts another phone. */
@Singleton
class PhonePairingIdentity @Inject constructor(
    @ApplicationContext context: Context,
    private val deviceIdentity: DeviceIdentity,
    private val settingsRepository: SettingsRepository,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val secret: String
        get() = prefs.getString(KEY_SECRET, null) ?: newSecret().also {
            prefs.edit().putString(KEY_SECRET, it).apply()
        }

    fun payload(host: String, port: Int): PairingPayload = PairingPayload(
        v = PairingPayload.WIRE_VERSION,
        deviceId = deviceIdentity.deviceId,
        name = settingsRepository.settings.value.deviceName,
        host = host,
        port = port,
        secret = secret,
        availability = PairingPayload.AVAILABILITY_FOREGROUND,
    )

    fun regenerateSecret() {
        prefs.edit().putString(KEY_SECRET, newSecret()).apply()
    }

    private fun newSecret(): String = ByteArray(32).also(SecureRandom()::nextBytes).let {
        Base64.encodeToString(it, Base64.NO_WRAP)
    }

    private companion object {
        const val PREFS = "ncy_phone_pairing"
        const val KEY_SECRET = "secret"
    }
}
