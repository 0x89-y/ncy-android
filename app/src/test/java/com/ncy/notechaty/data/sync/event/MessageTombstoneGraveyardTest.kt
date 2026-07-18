package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTombstoneGraveyardTest {
    private fun event(id: String, time: Long, op: ChatEventOp) = ChatEvent(
        eventId = id,
        hlc = HlcTimestamp(time, 0, "phone"),
        originDevice = "phone",
        chatId = "chat",
        op = op,
        profileId = "profile",
        profileName = "Personal",
    )

    @Test
    fun `compaction archives exact deletion and rejects stale history after restart`() {
        val root = Files.createTempDirectory("ncy-message-graveyard").toFile()
        val chatDir = root.resolve("chat_chat").apply { mkdirs() }
        try {
            val metadata = event(
                "meta",
                1,
                ChatEventOp.ChatMetadataChanged(ChatMetadata("chat", "Chat", createdAt = 1)),
            )
            val created = event(
                "create",
                2,
                ChatEventOp.MessageCreated(Message("message", "chat", createdAt = 2)),
            )
            val edits = (3L..1999L).map { time ->
                event(
                    "edit-$time",
                    time,
                    ChatEventOp.MessageEdited(Message("message", "chat", createdAt = 2)),
                )
            }
            val deleted = event("delete", 2000, ChatEventOp.MessageTombstoned("message"))
            EventLog(chatDir.resolve("events.ndjson")).replaceAll(
                listOf(metadata, created) + edits + deleted,
            )

            EventLogCompactor.compactChatDir(chatDir)

            val active = EventLog(chatDir.resolve("events.ndjson")).readAll()
            assertFalse(active.any { it.op is ChatEventOp.MessageTombstoned })
            assertFalse(active.any {
                (it.op as? ChatEventOp.MessageCreated)?.message?.id == "message" ||
                    (it.op as? ChatEventOp.MessageEdited)?.message?.id == "message"
            })
            assertEquals(listOf(deleted), MessageTombstoneGraveyard.events(chatDir))

            val reopened = ChatEventHistory.read(chatDir)
            val stale = event(
                "stale-restored-edit",
                3000,
                ChatEventOp.MessageEdited(Message("message", "chat", createdAt = 2)),
            )
            assertTrue(MaterializedView.fold(reopened + stale)!!.messages.isEmpty())
            assertTrue(MessageTombstoneGraveyard.contains(chatDir, "message"))
            assertFalse(MessageTombstoneGraveyard.contains(chatDir, "other"))
            assertEquals(1, MessageTombstoneGraveyard.purge(root))
            assertFalse(MessageTombstoneGraveyard.contains(chatDir, "message"))
        } finally {
            root.deleteRecursively()
        }
    }
}
