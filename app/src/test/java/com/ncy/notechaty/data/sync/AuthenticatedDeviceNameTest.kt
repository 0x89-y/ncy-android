package com.ncy.notechaty.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticatedDeviceNameTest {
    @Test
    fun `authenticated nonblank name replaces stale name`() {
        assertEquals("Renamed desktop", AuthenticatedDeviceName.refresh("Old name", "  Renamed desktop  "))
    }

    @Test
    fun `blank name is ignored and long names are bounded`() {
        assertEquals("Old name", AuthenticatedDeviceName.refresh("Old name", "   "))
        assertEquals(
            AuthenticatedDeviceName.MAX_LENGTH,
            AuthenticatedDeviceName.refresh("Old name", "x".repeat(500)).length,
        )
    }
}
