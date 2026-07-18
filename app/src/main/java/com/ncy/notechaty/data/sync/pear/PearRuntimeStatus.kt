package com.ncy.notechaty.data.sync.pear

sealed class PearRuntimeStatus {
    data object Disabled : PearRuntimeStatus()
    data object Starting : PearRuntimeStatus()
    data class Ready(val publicKey: String) : PearRuntimeStatus()
    data class Failed(val reason: String) : PearRuntimeStatus()
}
