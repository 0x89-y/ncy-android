package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.BlockType
import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.model.RichTextBlock
import com.ncy.notechaty.data.model.RichTextSpan

/**
 * Pure fold from an event log to a [ChatData]. Processes events in HLC order so the result
 * is deterministic regardless of the order events were appended.
 *
 * Rules:
 *  - ChatMetadataChanged: whole-record LWW (later HLC wins; achieved naturally by ordering).
 *  - MessageCreated / MessageEdited: applied unless the message id is tombstoned.
 *  - MessageTombstoned: removes the message and ignores all later ops on that id.
 *  - ChatTombstoned: yields `null` (chat should not exist on disk).
 */
object MaterializedView {

    /** Fold the events into a ChatData, or null if the chat is tombstoned / has no metadata. */
    fun fold(
        events: List<ChatEvent>,
        trustedDecryptedEventIds: Set<String> = emptySet(),
    ): ChatData? {
        if (events.isEmpty()) return null

        val ordered = events.sortedBy { it.hlc }
        // Safety invariant shared with Core: if the final metadata is protected,
        // never materialize a plaintext body from an older/concurrent event.
        val finalProtected = ordered
            .lastOrNull { it.op is ChatEventOp.ChatMetadataChanged }
            ?.let { (it.op as ChatEventOp.ChatMetadataChanged).metadata.isProtected }
            ?: false

        var metadata: ChatMetadata? = null
        val messages = LinkedHashMap<String, Message>()
        val tombstonedMessages = HashSet<String>()
        var chatTombstoned = false

        for (event in ordered) {
            when (val op = event.op) {
                is ChatEventOp.ChatTombstoned -> chatTombstoned = true
                is ChatEventOp.ChatMetadataChanged -> metadata = op.metadata
                is ChatEventOp.MessageTombstoned -> {
                    tombstonedMessages.add(op.messageId)
                    messages.remove(op.messageId)
                }
                is ChatEventOp.MessageCreated -> {
                    if (op.message.id !in tombstonedMessages) {
                        messages[op.message.id] = if (
                            finalProtected && event.eventId !in trustedDecryptedEventIds
                        ) {
                            lockedPlaceholder(op.message.id, event)
                        } else op.message
                    }
                }
                is ChatEventOp.MessageEdited -> {
                    if (op.message.id !in tombstonedMessages) {
                        messages[op.message.id] = if (
                            finalProtected && event.eventId !in trustedDecryptedEventIds
                        ) {
                            lockedPlaceholder(op.message.id, event)
                        } else op.message
                    }
                }
                is ChatEventOp.EncryptedMessage -> {
                    // Phase 3: the original message id lives inside the encrypted payload,
                    // so without a key we synthesize a placeholder keyed by eventId. The
                    // decrypted view (FileChatRepository.getMessages with cached key)
                    // re-materializes from raw events and replaces this with the real message.
                    val placeholderId = op.messageId ?: "locked_${event.eventId}"
                    if (placeholderId !in tombstonedMessages) {
                        messages[placeholderId] = lockedPlaceholder(placeholderId, event)
                    }
                }
            }
        }

        if (chatTombstoned || metadata == null) return null

        return ChatData(
            chat = metadata,
            messages = messages.values.sortedBy { it.createdAt }
        )
    }

    /** Synthetic message stand-in for an [ChatEventOp.EncryptedMessage]. The UI renders
     *  the `[locked]` glyph distinctively; the message's `createdAt` is the event's
     *  physical HLC so locked bubbles still sort into the right place in the timeline. */
    private fun lockedPlaceholder(id: String, event: ChatEvent): Message = Message(
        id = id,
        chatId = event.chatId,
        content = listOf(
            RichTextBlock(
                type = BlockType.PARAGRAPH,
                spans = listOf(RichTextSpan(text = "🔒 Protected message"))
            )
        ),
        createdAt = event.hlc.physicalMs,
        type = MessageType.TEXT
    )
}
