package com.ncy.notechaty.data.sync.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncLimitsPartialClaimTest {
    @get:Rule val temp = TemporaryFolder()

    private val hash = "a".repeat(64)

    @Test
    fun `one hash resolves to one partial, which only one transfer may own`() {
        val first = SyncLimits.blobPartialFile(temp.root, hash, "image")
        val second = SyncLimits.blobPartialFile(temp.root, hash, "image")
        assertEquals(first, second)

        assertTrue(SyncLimits.claimPartial(first))
        assertFalse(SyncLimits.claimPartial(second))

        SyncLimits.releasePartial(first)
        assertTrue(SyncLimits.claimPartial(second))
        SyncLimits.releasePartial(second)
    }

    @Test
    fun `the same hash under another media type is a separate partial`() {
        val image = SyncLimits.blobPartialFile(temp.root, hash, "image")
        val file = SyncLimits.blobPartialFile(temp.root, hash, "file")

        assertTrue(SyncLimits.claimPartial(image))
        assertTrue(SyncLimits.claimPartial(file))

        SyncLimits.releasePartial(image)
        SyncLimits.releasePartial(file)
    }
}
