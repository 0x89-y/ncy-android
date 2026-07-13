package com.ncy.notechaty.data.sync.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class MdnsPrivacyLabelTest {
    @Test
    fun labelUsesStableIdInsteadOfModelOrFriendlyName() {
        assertEquals(
            "Ncy a1b2c3d4",
            MdnsPrivacyLabel.fromDeviceId("A1B2C3D4-ffff-eeee-dddd-cccccccccccc"),
        )
        assertEquals("Ncy device", MdnsPrivacyLabel.fromDeviceId("---"))
    }
}
