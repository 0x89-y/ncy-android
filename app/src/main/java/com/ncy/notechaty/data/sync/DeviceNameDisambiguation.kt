package com.ncy.notechaty.data.sync

import java.util.Locale

/** Presentation-only helpers for telling identically named sync devices apart. */
object DeviceNameDisambiguation {
    fun duplicateNameKeys(devices: List<Pair<String, String>>): Set<String> = devices
        .map { (name, _) -> nameKey(name) }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

    fun isDuplicate(name: String, duplicateNameKeys: Set<String>): Boolean =
        nameKey(name) in duplicateNameKeys

    fun shortFingerprint(deviceId: String): String = deviceId
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
        .take(8)
        .ifEmpty { "unknown" }

    private fun nameKey(name: String): String = name.trim().lowercase(Locale.ROOT)
}
