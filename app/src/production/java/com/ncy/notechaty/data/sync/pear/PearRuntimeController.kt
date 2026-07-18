package com.ncy.notechaty.data.sync.pear

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Production placeholder until the experimental Pear transport graduates from Ncy Dev. */
@Singleton
class PearRuntimeController @Inject constructor() {
    private val _status = MutableStateFlow<PearRuntimeStatus>(PearRuntimeStatus.Disabled)
    val status: StateFlow<PearRuntimeStatus> = _status

    fun start() = Unit
    fun stop() = Unit
    fun allowPeers(publicKeys: Set<String>) = Unit
    fun connect(publicKey: String) = Unit
    fun send(connectionId: String, text: String): Boolean = false
    fun close(connectionId: String) = Unit
    fun addListener(listener: PearRuntimeListener) = Unit
    fun removeListener(listener: PearRuntimeListener) = Unit
}
