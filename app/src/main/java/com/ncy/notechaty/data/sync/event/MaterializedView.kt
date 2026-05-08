package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message

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
    fun fold(events: List<ChatEvent>): ChatData? {
        if (events.isEmpty()) return null

        val ordered = events.sortedBy { it.hlc }

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
                        messages[op.message.id] = op.message
                    }
                }
                is ChatEventOp.MessageEdited -> {
                    if (op.message.id !in tombstonedMessages) {
                        messages[op.message.id] = op.message
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
}
