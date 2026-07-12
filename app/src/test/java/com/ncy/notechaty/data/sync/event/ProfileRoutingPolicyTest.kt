package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRoutingPolicyTest {
    @Test
    fun historicalUnprotectedPersonalIdsMapToActivePersonal() {
        val profiles = listOf(
            Profile(id = "old", name = "Personal", createdAt = 1),
            Profile(id = "active", name = "Personal", createdAt = 2),
        )
        assertEquals(
            "active",
            ProfileRoutingPolicy.chooseProfileId(profiles, "active", "third-generation", "Personal"),
        )
    }

    @Test
    fun protectedProfilesAreNeverMergedByName() {
        val profiles = listOf(
            Profile(id = "one", name = "Private", pinHash = "a", createdAt = 1),
            Profile(id = "two", name = "Private", pinHash = "b", createdAt = 2),
        )
        assertNull(ProfileRoutingPolicy.chooseProfileId(profiles, "one", "unknown", "Private"))
        assertEquals("two", ProfileRoutingPolicy.chooseProfileId(profiles, "one", "two", "Private"))
    }

    @Test
    fun aSingleProtectedProfileStillRequiresItsExactId() {
        val profiles = listOf(
            Profile(id = "private", name = "Private", pinHash = "hash", createdAt = 1),
        )
        assertNull(ProfileRoutingPolicy.chooseProfileId(profiles, "private", "unknown", "Private"))
        assertEquals(
            "private",
            ProfileRoutingPolicy.chooseProfileId(profiles, "private", "private", "Private"),
        )
    }
}
