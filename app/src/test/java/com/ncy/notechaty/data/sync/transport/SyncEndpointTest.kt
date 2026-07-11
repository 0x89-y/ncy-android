package com.ncy.notechaty.data.sync.transport

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncEndpointTest {
    @Test
    fun formatsIpv4AndDnsHostsNormally() {
        assertEquals("ws://192.168.1.2:8765/sync", SyncEndpoint.webSocketUrl("192.168.1.2", 8765, "sync"))
        assertEquals("desktop.local:8765", SyncEndpoint.display("desktop.local", 8765))
    }

    @Test
    fun bracketsIpv6AndEncodesScopeForUrls() {
        assertEquals("ws://[2001:db8::2]:8765/sync", SyncEndpoint.webSocketUrl("2001:db8::2", 8765, "/sync"))
        assertEquals("ws://[fe80::1%25wlan0]:8765/pair", SyncEndpoint.webSocketUrl("fe80::1%wlan0", 8765, "pair"))
        assertEquals("[2001:db8::2]:8765", SyncEndpoint.display("[2001:db8::2]", 8765))
    }
}
