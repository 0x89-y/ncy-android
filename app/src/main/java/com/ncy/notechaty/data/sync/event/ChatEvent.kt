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

    /**
     * Phase 3: a `MessageCreated` or `MessageEdited` whose body is AES-GCM-sealed
     * under a key derived from the chat's PIN + [ChatMetadata.protectionSalt].
     * `innerType` records the original op type so the materializer can place a
     * typed-correct placeholder. `nonce` + `ciphertext` are the raw bytes (not
     * base64) for the round-trip with [ChatEventCodec].
     */
    data class EncryptedMessage(
        val innerType: String,
        val nonce: ByteArray,
        val ciphertext: ByteArray
    ) : ChatEventOp() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncryptedMessage) return false
            return innerType == other.innerType &&
                nonce.contentEquals(other.nonce) &&
                ciphertext.contentEquals(other.ciphertext)
        }
        override fun hashCode(): Int {
            var r = innerType.hashCode()
            r = 31 * r + nonce.contentHashCode()
            r = 31 * r + ciphertext.contentHashCode()
            return r
        }
    }
}
