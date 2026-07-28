package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.RichTextBlock
import com.ncy.notechaty.data.model.RichTextSpan
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deleting a message has to take its text with it.
 *
 * The compactor already dropped a tombstoned message's create/edit history, but only
 * for logs past its 2,000-event threshold — a size gate meant to avoid pointless
 * rewrite I/O. An ordinary chat never came close, so deleted notes stayed fully
 * readable in `events.ndjson` indefinitely.
 */
class EventLogCompactorScrubTest {

    private fun event(id: String, time: Long, op: ChatEventOp) = ChatEvent(
        eventId = id,
        hlc = HlcTimestamp(time, 0, "phone"),
        originDevice = "phone",
        chatId = "chat",
        op = op,
        profileId = "profile",
        profileName = "Personal",
    )

    private fun message(id: String, text: String, createdAt: Long) = Message(
        id = id,
        chatId = "chat",
        createdAt = createdAt,
        content = listOf(RichTextBlock(spans = listOf(RichTextSpan(text = text)))),
    )

    @Test
    fun `a small chat still loses the body of a deleted message`() {
        val root = Files.createTempDirectory("ncy-scrub-small").toFile()
        val chatDir = root.resolve("chat_chat").apply { mkdirs() }
        try {
            val log = EventLog(chatDir.resolve("events.ndjson"))
            log.append(
                event("create-secret", 1, ChatEventOp.MessageCreated(message("m1", "launch codes", 1)))
            )
            log.append(
                event("create-kept", 2, ChatEventOp.MessageCreated(message("m2", "groceries", 2)))
            )
            log.append(event("delete", 3, ChatEventOp.MessageTombstoned("m1")))

            val raw = chatDir.resolve("events.ndjson").readText()
            assertTrue("precondition: the body is on disk", raw.contains("launch codes"))

            EventLogCompactor.compactChatDir(chatDir)

            val after = chatDir.resolve("events.ndjson").readText()
            assertFalse(
                "deleted message body still recoverable from a small chat's log",
                after.contains("launch codes"),
            )
            assertTrue("scrub removed the wrong message", after.contains("groceries"))
            // The deletion itself survives, so peers still converge on it.
            assertTrue(
                MessageTombstoneGraveyard.events(chatDir)
                    .any { (it.op as? ChatEventOp.MessageTombstoned)?.messageId == "m1" },
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `a chat with no deletions is left alone`() {
        val root = Files.createTempDirectory("ncy-scrub-noop").toFile()
        val chatDir = root.resolve("chat_chat").apply { mkdirs() }
        try {
            val events = chatDir.resolve("events.ndjson")
            val log = EventLog(events)
            log.append(event("create", 1, ChatEventOp.MessageCreated(message("m1", "groceries", 1))))
            val before = events.readText()

            EventLogCompactor.compactChatDir(chatDir)

            // Untouched: rewriting a log with nothing to scrub is the I/O the
            // threshold exists to avoid.
            assertTrue(events.readText() == before)
        } finally {
            root.deleteRecursively()
        }
    }
}
