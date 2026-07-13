package com.ncy.notechaty.data.sync

/** Sanitizes mutable device-name metadata received after identity authentication. */
object AuthenticatedDeviceName {
    const val MAX_LENGTH = 80

    fun refresh(current: String, received: String): String = received
        .trim()
        .take(MAX_LENGTH)
        .takeIf { it.isNotEmpty() }
        ?: current
}
