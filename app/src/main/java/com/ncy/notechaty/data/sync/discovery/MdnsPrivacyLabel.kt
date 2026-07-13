package com.ncy.notechaty.data.sync.discovery

import java.util.Locale

/** LAN-visible DNS-SD label that never includes a model, hostname, or user label. */
object MdnsPrivacyLabel {
    fun fromDeviceId(deviceId: String): String {
        val short = deviceId
            .filter { it.isLetterOrDigit() }
            .take(8)
            .lowercase(Locale.ROOT)
            .ifEmpty { "device" }
        return "Ncy $short"
    }
}
