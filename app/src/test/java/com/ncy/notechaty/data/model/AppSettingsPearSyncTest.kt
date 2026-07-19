package com.ncy.notechaty.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsPearSyncTest {
    @Test
    fun pearSyncIsOptIn() {
        assertEquals(SyncTransportMode.LOCAL_ONLY, AppSettings().syncTransportMode)
        assertFalse(AppSettings().syncTransportMode.usesPear)
    }

    @Test
    fun onlyLocalModeAvoidsPearRuntime() {
        assertFalse(SyncTransportMode.LOCAL_ONLY.usesPear)
        assertTrue(SyncTransportMode.entries.filterNot { it == SyncTransportMode.LOCAL_ONLY }.all { it.usesPear })
    }
}
