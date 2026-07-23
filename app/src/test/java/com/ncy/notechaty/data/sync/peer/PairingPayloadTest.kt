package com.ncy.notechaty.data.sync.peer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PairingPayloadTest {
    @Test
    fun parsesDesktopPairingWireNames() {
        val publicKey = "a".repeat(64)
        val payload = PairingPayload.parse(
            """
            {
              "v": 1,
              "deviceId": "desktop-id",
              "name": "My Desktop",
              "host": "",
              "port": 0,
              "secret": "pairing-secret",
              "pearPublicKey": "$publicKey"
            }
            """.trimIndent(),
        )

        assertNotNull(payload)
        assertEquals("desktop-id", payload?.deviceId)
        assertEquals("pairing-secret", payload?.secret)
        assertEquals(publicKey, payload?.pearPublicKey)
    }
}
