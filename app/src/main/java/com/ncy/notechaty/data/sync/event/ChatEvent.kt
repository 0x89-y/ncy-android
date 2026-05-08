package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message

/**
 * A single mutation to a chat. The append-only log of these events is the source of truth;
 * `chat.json` is a materialized view derived from the log.
 */
data class ChatEvent(
    val eventId: String,
    val hlc: HlcTimestamp,
    val originDevice: String,
    val chatId: String,
    val op: ChatEventOp
)

sealed class ChatEventOp {
    data class MessageCreated(val message: Message) : ChatEventOp()
    data class MessageEdited(val message: Message) : ChatEventOp()
    data class MessageTombstoned(val messageId: String) : ChatEventOp()
    data class ChatMetadataChanged(val metadata: ChatMetadata) : ChatEventOp()
    data class ChatTombstoned(val chatId: String) : ChatEventOp()
}
