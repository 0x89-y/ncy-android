package com.ncy.notechaty.data.sync.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportCryptoTest {
    private val secret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
    private val serverNonce = "ICEiIyQlJicoKSorLC0uLw=="
    private val clientNonce = "QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl8="

    @Test
    fun matchesCoreDeterministicVector() {
        val client = TransportCrypto.forClient(secret, serverNonce, clientNonce)
        val envelope = client.seal(SyncWireMessage.BlobMissing("vector"))
        assertEquals(0L, envelope.seq)
        assertEquals(
            "dejB55qFviInI2E10OD4jTuRg15XT+qacEiOVdbrF9Uy5moz2MZDSkgNR2UyTMcu3rHI7pG+",
            envelope.data,
        )
    }

    @Test
    fun roundTripRejectsReplay() {
        val client = TransportCrypto.forClient(secret, serverNonce, clientNonce)
        val server = TransportCrypto.forServer(secret, serverNonce, clientNonce)
        val envelope = client.seal(SyncWireMessage.BlobMissing("abc"))
        assertEquals(SyncWireMessage.BlobMissing("abc"), server.open(envelope))
        assertTrue(runCatching { server.open(envelope) }.isFailure)
    }
}
