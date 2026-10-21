package com.ncy.notechaty.data.sync.peer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PairingPayloadTest {
    @Test
    fun parsesDesktopPairingWireNamesAndIgnoresAPearKey() {
        // Desktops still include their Pear key; the phone pairs over the LAN regardless.
        val payload = PairingPayload.parse(
            """
            {
              "v": 1,
              "deviceId": "desktop-id",
              "name": "My Desktop",
              "host": "192.168.1.42",
              "port": 8765,
              "secret": "pairing-secret",
              "pearPublicKey": "${"a".repeat(64)}"
            }
            """.trimIndent(),
        )

        assertNotNull(payload)
        assertEquals("desktop-id", payload?.deviceId)
        assertEquals("pairing-secret", payload?.secret)
        assertEquals("192.168.1.42", payload?.host)
        assertEquals(8765, payload?.port)
    }

    @Test
    fun rejectsAnInvitationWithoutALocalAddress() {
        // A Pear-only invitation carried no LAN address; nothing could connect to it.
        val payload = PairingPayload.parse(
            """
            {
              "v": 1,
              "deviceId": "desktop-id",
              "name": "My Desktop",
              "host": "",
              "port": 0,
              "secret": "pairing-secret",
              "pearPublicKey": "${"a".repeat(64)}"
            }
            """.trimIndent(),
        )

        assertNull(payload)
    }
}
