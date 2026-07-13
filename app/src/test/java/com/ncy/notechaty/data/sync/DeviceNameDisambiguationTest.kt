package com.ncy.notechaty.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceNameDisambiguationTest {
    @Test
    fun `duplicate names ignore case and surrounding whitespace`() {
        val duplicates = DeviceNameDisambiguation.duplicateNameKeys(
            listOf(
                "Pixel 9" to "first-device",
                " pixel 9 " to "second-device",
                "Laptop" to "third-device",
            )
        )

        assertTrue(DeviceNameDisambiguation.isDuplicate("PIXEL 9", duplicates))
        assertFalse(DeviceNameDisambiguation.isDuplicate("Laptop", duplicates))
    }

    @Test
    fun `fingerprint uses only the stable device id`() {
        assertEquals("a1b2c3d4", DeviceNameDisambiguation.shortFingerprint("A1B2-C3D4-E5F6"))
        assertEquals("unknown", DeviceNameDisambiguation.shortFingerprint("---"))
    }
}
