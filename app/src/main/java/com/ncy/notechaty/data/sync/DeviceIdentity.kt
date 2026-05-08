package com.ncy.notechaty.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stable per-install device id. Used as the HLC nodeId and the originDevice on every event.
 * Generated lazily on first access and persisted to SharedPreferences.
 *
 * Phase 0 only stores the id. Phase 1+ will add a long-lived keypair for pairing.
 */
@Singleton
class DeviceIdentity @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) existing
        else {
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            generated
        }
    }

    private companion object {
        const val PREFS = "ncy_device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
