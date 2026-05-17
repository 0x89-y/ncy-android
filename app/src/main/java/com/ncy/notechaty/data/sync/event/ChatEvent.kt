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
    val op: ChatEventOp,
    /**
     * Phase: profile sync. Which phone profile this chat belongs to, and its display
     * name, so a paired desktop can group chats by profile. Default `""` means
     * "unknown / pre-profile-sync" — receivers bucket those under a single default
     * profile. Stamped on author and backfilled from the on-disk profile dir at
     * snapshot time for legacy events.
     */
    val profileId: String = "",
    val profileName: String = ""
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
        val ciphertext: ByteArray,
        /**
         * Protected-chat catch-up: the `eventId` of a *plaintext* message event this
         * sealed event replaces. Set when a chat is protected after it had already
         * synced in plaintext to a peer — the peer drops the superseded plaintext
         * from its at-rest log on receipt. Null for normally-authored encrypted
         * messages (nothing to supersede). Travels in the cleartext envelope so a
         * locked peer can act on it without the key.
         */
        val supersedes: String? = null
    ) : ChatEventOp() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EncryptedMessage) return false
            return innerType == other.innerType &&
                nonce.contentEquals(other.nonce) &&
                ciphertext.contentEquals(other.ciphertext) &&
                supersedes == other.supersedes
        }
        override fun hashCode(): Int {
            var r = innerType.hashCode()
            r = 31 * r + nonce.contentHashCode()
            r = 31 * r + ciphertext.contentHashCode()
            r = 31 * r + (supersedes?.hashCode() ?: 0)
            return r
        }
    }
}
