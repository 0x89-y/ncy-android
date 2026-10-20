package com.ncy.notechaty.data.sync.event

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.normalizedForCompatibility
import com.ncy.notechaty.data.sync.protection.ProtectionCrypto
import java.util.Base64

/**
 * Encodes and decodes [ChatEvent] as one JSON object per line (NDJSON). Format is portable
 * — keys are explicit so a non-Kotlin client can stream-parse without sharing types.
 *
 * Wire schema:
 * ```
 * {"v":1,"type":"<OpType>","eventId":"<uuid>","hlc":{"physicalMs":N,"logical":N,"nodeId":"..."},
 *  "originDevice":"<id>","chatId":"<id>","payload":{...}}
 * ```
 */
class ChatEventCodec(private val gson: Gson = Gson()) {

    fun encode(event: ChatEvent): String {
        val obj = JsonObject().apply {
            addProperty("v", WIRE_VERSION)
            addProperty("type", typeOf(event.op))
            addProperty("eventId", event.eventId)
            add("hlc", gson.toJsonTree(event.hlc))
            addProperty("originDevice", event.originDevice)
            addProperty("chatId", event.chatId)
            addProperty("profileId", event.profileId)
            addProperty("profileName", event.profileName)
            add("payload", payloadOf(event.op))
        }
        return obj.toString()
    }

    fun decode(line: String): ChatEvent {
        val obj = JsonParser.parseString(line).asJsonObject
        val type = obj["type"].asString
        val hlc = gson.fromJson(obj["hlc"], HlcTimestamp::class.java)
        val payload = obj["payload"]
        val op: ChatEventOp = when (type) {
            TYPE_MESSAGE_CREATED, TYPE_MESSAGE_EDITED -> {
                // Phase 3: detect the sealed envelope shape by the `enc` discriminator.
                if (payload.isJsonObject && payload.asJsonObject.has("enc")) {
                    val po = payload.asJsonObject
                    ChatEventOp.EncryptedMessage(
                        innerType = type,
                        nonce = Base64.getDecoder().decode(po["nonce"].asString),
                        ciphertext = Base64.getDecoder().decode(po["ct"].asString),
                        messageId = po["messageId"]
                            ?.takeIf { !it.isJsonNull }?.asString,
                        supersedes = po["supersedes"]
                            ?.takeIf { !it.isJsonNull }?.asString,
                        // Absent from envelopes that predate the list; empty is a real answer.
                        blobs = po["blobs"]?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { element ->
                            runCatching { gson.fromJson(element, SealedBlobRef::class.java) }.getOrNull()
                                ?.takeIf { it.hash.isNotEmpty() && it.mediaType.isNotEmpty() }
                        },
                    )
                } else if (type == TYPE_MESSAGE_CREATED) {
                    ChatEventOp.MessageCreated(
                        gson.fromJson(payload, Message::class.java).normalizedForCompatibility()
                    )
                } else {
                    ChatEventOp.MessageEdited(
                        gson.fromJson(payload, Message::class.java).normalizedForCompatibility()
                    )
                }
            }
            TYPE_MESSAGE_TOMBSTONED ->
                ChatEventOp.MessageTombstoned(payload.asJsonObject["messageId"].asString)
            TYPE_CHAT_METADATA_CHANGED ->
                ChatEventOp.ChatMetadataChanged(
                    gson.fromJson(payload, ChatMetadata::class.java).normalizedForCompatibility()
                )
            TYPE_CHAT_TOMBSTONED ->
                ChatEventOp.ChatTombstoned(payload.asJsonObject["chatId"].asString)
            else -> throw IllegalArgumentException("Unknown event type: $type")
        }
        return ChatEvent(
            eventId = obj["eventId"].asString,
            hlc = hlc,
            originDevice = obj["originDevice"].asString,
            chatId = obj["chatId"].asString,
            op = op,
            profileId = obj["profileId"]?.takeIf { !it.isJsonNull }?.asString ?: "",
            profileName = obj["profileName"]?.takeIf { !it.isJsonNull }?.asString ?: ""
        )
    }

    private fun typeOf(op: ChatEventOp): String = when (op) {
        is ChatEventOp.MessageCreated -> TYPE_MESSAGE_CREATED
        is ChatEventOp.MessageEdited -> TYPE_MESSAGE_EDITED
        is ChatEventOp.MessageTombstoned -> TYPE_MESSAGE_TOMBSTONED
        is ChatEventOp.ChatMetadataChanged -> TYPE_CHAT_METADATA_CHANGED
        is ChatEventOp.ChatTombstoned -> TYPE_CHAT_TOMBSTONED
        // Outer wire `type` mirrors the original op so phase-1/-2 peers still see a
        // recognisable label; the payload's `enc` key marks it as encrypted.
        is ChatEventOp.EncryptedMessage -> op.innerType
    }

    private fun payloadOf(op: ChatEventOp) = when (op) {
        is ChatEventOp.MessageCreated -> gson.toJsonTree(op.message)
        is ChatEventOp.MessageEdited -> gson.toJsonTree(op.message)
        is ChatEventOp.MessageTombstoned -> JsonObject().apply { addProperty("messageId", op.messageId) }
        is ChatEventOp.ChatMetadataChanged -> gson.toJsonTree(op.metadata)
        is ChatEventOp.ChatTombstoned -> JsonObject().apply { addProperty("chatId", op.chatId) }
        is ChatEventOp.EncryptedMessage -> JsonObject().apply {
            addProperty("v", ProtectionCrypto.ENVELOPE_VERSION)
            addProperty("enc", "AES-256-GCM")
            addProperty("nonce", Base64.getEncoder().encodeToString(op.nonce))
            addProperty("ct", Base64.getEncoder().encodeToString(op.ciphertext))
            op.messageId?.let { addProperty("messageId", it) }
            op.supersedes?.let { addProperty("supersedes", it) }
            op.blobs?.let { add("blobs", gson.toJsonTree(it)) }
        }
    }

    companion object {
        const val WIRE_VERSION = 1
        const val TYPE_MESSAGE_CREATED = "MessageCreated"
        const val TYPE_MESSAGE_EDITED = "MessageEdited"
        const val TYPE_MESSAGE_TOMBSTONED = "MessageTombstoned"
        const val TYPE_CHAT_METADATA_CHANGED = "ChatMetadataChanged"
        const val TYPE_CHAT_TOMBSTONED = "ChatTombstoned"
    }
}
