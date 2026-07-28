package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message
import java.nio.file.Files
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A deletion has to stand on its own, without the creation it deletes.
 *
 * The desktop now strips a deleted message's `MessageCreated` from its log, so a
 * device catching up receives the tombstone and nothing else — where it previously
 * received the message first and was then told to hide it. That makes the
 * tombstone-alone path the normal one for every fresh or re-paired device, not an
 * out-of-order edge case, so it is worth holding still.
 */
class TombstoneWithoutCreationTest {

    private fun event(id: String, time: Long, op: ChatEventOp) = ChatEvent(
        eventId = id,
        hlc = HlcTimestamp(time, 0, "desktop"),
        originDevice = "desktop",
        chatId = "chat",
        op = op,
        profileId = "profile",
        profileName = "Personal",
    )

    private val metadata =
        event("meta", 1, ChatEventOp.ChatMetadataChanged(ChatMetadata("chat", "Chat", createdAt = 1)))

    @Test
    fun `a tombstone for a message that was never seen still hides it`() {
        val root = Files.createTempDirectory("ncy-tombstone-alone").toFile()
        // MessageTombstoneGraveyard keys off the directory name.
        val chatDir = root.resolve("chat_chat").apply { mkdirs() }
        try {
            val tombstone = event("delete", 3, ChatEventOp.MessageTombstoned("m1"))
            MessageTombstoneGraveyard.archive(chatDir, tombstone)

            // Archived even though nothing here ever held the message.
            assertTrue(MessageTombstoneGraveyard.contains(chatDir, "m1"))

            // And a creation arriving afterwards — an older peer replaying history,
            // or a catch-up that has not been scrubbed — must not bring it back.
            val history = ChatEventHistory.read(chatDir) + metadata +
                event("create", 2, ChatEventOp.MessageCreated(Message("m1", "chat", createdAt = 2)))
            val chat = MaterializedView.fold(history)
            assertNull(
                "a deleted message came back",
                chat?.messages?.firstOrNull { it.id == "m1" },
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `the deletion survives a restart with no chat log at all`() {
        val root = Files.createTempDirectory("ncy-tombstone-restart").toFile()
        val chatDir = root.resolve("chat_chat").apply { mkdirs() }
        try {
            MessageTombstoneGraveyard.archive(
                chatDir,
                event("delete", 3, ChatEventOp.MessageTombstoned("m1")),
            )
            // Nothing in events.ndjson: the desktop scrubbed the creation, so the
            // graveyard is the only record this device has.
            assertTrue(ChatEventHistory.read(chatDir).any { it.op is ChatEventOp.MessageTombstoned })
            assertTrue(MessageTombstoneGraveyard.contains(chatDir, "m1"))
        } finally {
            root.deleteRecursively()
        }
    }
}
