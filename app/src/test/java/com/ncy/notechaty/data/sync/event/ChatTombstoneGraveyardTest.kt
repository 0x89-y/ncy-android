package com.ncy.notechaty.data.sync.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ChatTombstoneGraveyardTest {
    @Test
    fun `graveyard is exact persistent and explicitly purgeable`() {
        val root = Files.createTempDirectory("ncy-graveyard").toFile()
        val profile = root.resolve("profile_personal").apply { mkdirs() }
        val tombstone = ChatEvent(
            eventId = "delete",
            hlc = HlcTimestamp(2, 0, "phone"),
            originDevice = "phone",
            chatId = "chat",
            op = ChatEventOp.ChatTombstoned("chat"),
            profileId = "personal",
            profileName = "Personal",
        )
        try {
            assertTrue(ChatTombstoneGraveyard.archive(profile, tombstone))
            assertFalse(ChatTombstoneGraveyard.archive(profile, tombstone))
            assertTrue(ChatTombstoneGraveyard.contains(root, "chat"))
            assertEquals(listOf(tombstone), ChatTombstoneGraveyard.events(profile))
            val legacyDir = profile.resolve("chat_legacy").apply { mkdirs() }
            EventLog(legacyDir.resolve("events.ndjson")).append(
                tombstone.copy(
                    eventId = "legacy-delete",
                    chatId = "legacy",
                    op = ChatEventOp.ChatTombstoned("legacy"),
                )
            )
            assertEquals(2, ChatTombstoneGraveyard.purge(root))
            assertFalse(ChatTombstoneGraveyard.contains(root, "chat"))
            assertFalse(legacyDir.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
