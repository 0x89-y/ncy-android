package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterializedViewConflictTest {
    private fun event(
        id: String,
        time: Long,
        node: String,
        op: ChatEventOp,
        profileName: String = node,
    ) = ChatEvent(
        eventId = id,
        hlc = HlcTimestamp(time, 0, node),
        originDevice = node,
        chatId = "chat",
        op = op,
        profileId = "profile",
        profileName = profileName,
    )

    private fun metadata(protected: Boolean = false) = ChatMetadata(
        id = "chat",
        name = "Chat",
        createdAt = 1,
        isProtected = protected,
        protectionSalt = if (protected) "salt" else null,
    )

    private fun message(starred: Boolean) = Message(
        id = "message",
        chatId = "chat",
        createdAt = 1,
        isStarred = starred,
    )

    @Test
    fun threeDeviceEditsConvergeIndependentOfArrivalOrder() {
        val base = event("meta", 1, "a", ChatEventOp.ChatMetadataChanged(metadata()))
        val editB = event("edit-b", 2, "b", ChatEventOp.MessageEdited(message(false)))
        val editC = event("edit-c", 2, "c", ChatEventOp.MessageEdited(message(true)))

        listOf(
            listOf(base, editB, editC),
            listOf(editC, base, editB),
            listOf(editB, editC, base),
        ).forEach { order ->
            val result = MaterializedView.fold(order)!!
            assertEquals(1, result.messages.size)
            assertTrue(result.messages.single().isStarred)
        }
    }

    @Test
    fun deleteWinsOverLaterEditFromThirdDevice() {
        val result = MaterializedView.fold(
            listOf(
                event("meta", 1, "a", ChatEventOp.ChatMetadataChanged(metadata())),
                event("delete", 2, "b", ChatEventOp.MessageTombstoned("message")),
                event("edit", 3, "c", ChatEventOp.MessageEdited(message(true))),
            )
        )!!
        assertTrue(result.messages.isEmpty())
    }

    @Test
    fun fullReplayTombstoneRemovesChatRestoredFromStaleBackup() {
        val staleBackup = listOf(
            event("meta", 1, "phone", ChatEventOp.ChatMetadataChanged(metadata())),
            event("message", 2, "phone", ChatEventOp.MessageCreated(message(false))),
        )
        val desktopTombstone = event(
            "delete",
            3,
            "desktop",
            ChatEventOp.ChatTombstoned("chat"),
        )
        assertNull(MaterializedView.fold(staleBackup + desktopTombstone))
    }

    @Test
    fun latestProtectionMetadataHidesConcurrentPlaintext() {
        val result = MaterializedView.fold(
            listOf(
                event("meta-a", 1, "a", ChatEventOp.ChatMetadataChanged(metadata())),
                event("message-b", 2, "b", ChatEventOp.MessageCreated(message(false))),
                event("meta-c", 3, "c", ChatEventOp.ChatMetadataChanged(metadata(true))),
            )
        )!!
        assertTrue(result.chat.isProtected)
        assertEquals("message", result.messages.single().id)
        assertFalse(result.messages.single().isStarred)
        assertEquals("🔒 Protected message", result.messages.single().content.single().spans.single().text)
    }

    @Test
    fun trustedInMemoryDecryptionRendersWhileRawPlaintextStaysLocked() {
        val meta = event("meta", 1, "a", ChatEventOp.ChatMetadataChanged(metadata(true)))
        val decrypted = event("encrypted-event", 2, "b", ChatEventOp.MessageCreated(message(true)))

        val locked = MaterializedView.fold(listOf(meta, decrypted))!!
        assertEquals("🔒 Protected message", locked.messages.single().content.single().spans.single().text)

        val unlocked = MaterializedView.fold(
            listOf(meta, decrypted),
            trustedDecryptedEventIds = setOf("encrypted-event"),
        )!!
        assertTrue(unlocked.messages.single().isStarred)
        assertTrue(unlocked.messages.single().content.isEmpty())
    }

    @Test
    fun lockedEditsShareIdentityAndObeyTombstones() {
        fun encrypted(id: String, time: Long, node: String) = event(
            id,
            time,
            node,
            ChatEventOp.EncryptedMessage(
                innerType = ChatEventCodec.TYPE_MESSAGE_EDITED,
                nonce = byteArrayOf(),
                ciphertext = byteArrayOf(),
                messageId = "message",
            ),
        )
        val base = event("meta", 1, "a", ChatEventOp.ChatMetadataChanged(metadata(true)))
        val edited = MaterializedView.fold(
            listOf(base, encrypted("enc-b", 2, "b"), encrypted("enc-c", 3, "c"))
        )!!
        assertEquals(listOf("message"), edited.messages.map { it.id })

        val deleted = MaterializedView.fold(
            listOf(
                base,
                encrypted("enc-b", 2, "b"),
                event("delete", 3, "c", ChatEventOp.MessageTombstoned("message")),
            )
        )!!
        assertTrue(deleted.messages.isEmpty())
    }

    @Test
    fun encryptedMessageIdentitySurvivesWireAndCompaction() {
        val encrypted = event(
            "encrypted",
            2,
            "b",
            ChatEventOp.EncryptedMessage(
                innerType = ChatEventCodec.TYPE_MESSAGE_EDITED,
                nonce = byteArrayOf(1),
                ciphertext = byteArrayOf(2),
                messageId = "message",
            ),
        )
        val decoded = ChatEventCodec().decode(ChatEventCodec().encode(encrypted))
        assertEquals("message", (decoded.op as ChatEventOp.EncryptedMessage).messageId)

        val compacted = EventLogCompactor.compact(
            listOf(
                event("meta", 1, "a", ChatEventOp.ChatMetadataChanged(metadata(true))),
                encrypted,
                encrypted.copy(eventId = "newer", hlc = HlcTimestamp(3, 0, "c")),
            )
        )
        assertEquals(2, compacted.size)
        assertEquals("newer", compacted.last().eventId)
    }
}
