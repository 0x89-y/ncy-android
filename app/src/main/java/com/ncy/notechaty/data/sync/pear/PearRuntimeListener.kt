package com.ncy.notechaty.data.sync.pear

/** Complete text-frame events emitted by the optional Pear worker. */
interface PearRuntimeListener {
    fun onConnected(connectionId: String, publicKey: String, outgoing: Boolean) = Unit
    fun onFrame(connectionId: String, text: String) = Unit
    fun onDisconnected(connectionId: String, publicKey: String) = Unit
    fun onConnectionError(message: String) = Unit
}
