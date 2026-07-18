package com.ncy.notechaty.data.model

import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsPearSyncTest {
    @Test
    fun pearSyncIsOptIn() {
        assertFalse(AppSettings().pearSyncEnabled)
    }
}
