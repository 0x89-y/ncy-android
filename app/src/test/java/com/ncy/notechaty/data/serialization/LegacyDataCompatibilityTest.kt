package com.ncy.notechaty.data.serialization

import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.sync.event.ChatEventCodec
import com.ncy.notechaty.data.sync.event.ChatEventOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyDataCompatibilityTest {
    @Test
    fun chatJsonNullCollectionsBecomeEmpty() {
        val data = ChatSerializer().deserialize(
            """
            {
              "version": 2,
              "chat": {"id":"chat","name":"Old","createdAt":1,"tags":null},
              "messages": [{
                "id":"message","chatId":"chat","createdAt":1,
                "type":"TEXT","content":null,"imageFiles":null,"tags":null,
                "checklistItems":null,"reactions":null
              }]
            }
            """.trimIndent()
        )

        val message = data.messages.single()
        assertTrue(data.chat.tags.isEmpty())
        assertTrue(message.content.isEmpty())
        assertTrue(message.imageFiles.isEmpty())
        assertTrue(message.tags.isEmpty())
        assertTrue(message.checklistItems.isEmpty())
        assertTrue(message.reactions.isEmpty())
    }

    @Test
    fun eventMissingNewMessageFieldsUsesKotlinDefaults() {
        val event = ChatEventCodec().decode(
            """
            {
              "v":1,"type":"MessageCreated","eventId":"event",
              "hlc":{"physicalMs":1,"logical":0,"nodeId":"node"},
              "originDevice":"node","chatId":"chat",
              "payload":{"id":"message","chatId":"chat","createdAt":1}
            }
            """.trimIndent()
        )
        val message = (event.op as ChatEventOp.MessageCreated).message

        assertEquals(MessageType.TEXT, message.type)
        assertTrue(message.content.isEmpty())
        assertTrue(message.reactions.isEmpty())
    }

    @Test
    fun genericFileMessageDecodesWithPortableMetadata() {
        val hash = "a".repeat(64)
        val event = ChatEventCodec().decode(
            """
            {
              "v":1,"type":"MessageCreated","eventId":"file-event",
              "hlc":{"physicalMs":1,"logical":0,"nodeId":"node"},
              "originDevice":"node","chatId":"chat",
              "payload":{
                "id":"message","chatId":"chat","createdAt":1,"type":"FILE",
                "fileAttachments":[{
                  "file":"files/$hash.bin","name":"archive.zip",
                  "mimeType":"application/zip","sizeBytes":42
                }]
              }
            }
            """.trimIndent()
        )
        val message = (event.op as ChatEventOp.MessageCreated).message

        assertEquals(MessageType.FILE, message.type)
        assertEquals("archive.zip", message.fileAttachments.single().name)
        assertEquals(42L, message.fileAttachments.single().sizeBytes)
    }
}
