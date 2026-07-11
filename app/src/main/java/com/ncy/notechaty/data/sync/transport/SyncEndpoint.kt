package com.ncy.notechaty.data.sync.transport

/** Formatting helpers for pairing hosts, including IPv6 literals and zones. */
object SyncEndpoint {
    private fun rawHost(host: String): String = host.trim().removePrefix("[").removeSuffix("]")

    fun hostForUrl(host: String): String {
        val raw = rawHost(host)
        if (!raw.contains(':')) return raw
        val encodedZone = raw.replace("%25", "%").replace("%", "%25")
        return "[$encodedZone]"
    }

    fun webSocketUrl(host: String, port: Int, path: String): String =
        "ws://${hostForUrl(host)}:$port/${path.trimStart('/')}"

    fun display(host: String, port: Int): String = "${hostForUrl(host)}:$port"
}
