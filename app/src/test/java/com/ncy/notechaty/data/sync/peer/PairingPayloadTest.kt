package com.ncy.notechaty.data.sync.peer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingPayloadTest {
    @Test
    fun oldPayloadDefaultsToAlwaysAvailable() {
        val payload = PairingPayload.parse(
            """{"v":1,"deviceId":"peer","name":"Desktop","host":"192.168.1.2","port":8765,"secret":"secret"}"""
        )!!
        assertNull(payload.availability)
        assertFalse(payload.toPeer(1L).foregroundOnly)
        assertFalse(payload.toPeer(1L).disabled)
    }

    @Test
    fun foregroundCapabilitySurvivesPairingImport() {
        val payload = PairingPayload(
            v = 1,
            deviceId = "phone",
            name = "Phone",
            host = "192.168.1.3",
            port = 8765,
            secret = "secret",
            availability = PairingPayload.AVAILABILITY_FOREGROUND,
        )
        assertEquals(PairingPayload.AVAILABILITY_FOREGROUND, PairingPayload.parse(payload.toJson())?.availability)
        assertTrue(payload.toPeer(1L).foregroundOnly)
    }

    @Test
    fun optionalPearKeySurvivesPairingImport() {
        val key = "ab".repeat(32)
        val payload = PairingPayload(
            v = 1,
            deviceId = "phone",
            name = "Phone",
            host = "192.168.1.3",
            port = 8765,
            secret = "secret",
            pearPublicKey = key,
        )
        val parsed = PairingPayload.parse(payload.toJson())!!
        assertEquals(key, parsed.pearPublicKey)
        assertEquals(key, parsed.toPeer(1L).pearPublicKey)
    }

    @Test
    fun malformedPearKeyIsRejected() {
        assertNull(
            PairingPayload.parse(
                """{"v":1,"deviceId":"peer","name":"Desktop","host":"192.168.1.2","port":8765,"secret":"secret","pearPublicKey":"not-a-key"}"""
            )
        )
    }
}
