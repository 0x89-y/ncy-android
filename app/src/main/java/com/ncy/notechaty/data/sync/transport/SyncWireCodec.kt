package com.ncy.notechaty.data.sync.transport

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ncy.notechaty.data.sync.event.ChatEventCodec
import com.ncy.notechaty.data.sync.event.HlcTimestamp

/**
 * Encodes and decodes [SyncWireMessage]s as JSON text frames. Symmetrical with the Python
 * reference server (see scripts/sync_test_server.py). Schema is stable from phase 1.
 */
class SyncWireCodec(
    private val gson: Gson = Gson(),
    private val eventCodec: ChatEventCodec = ChatEventCodec(gson)
) {

    fun encode(message: SyncWireMessage): String {
        val obj = JsonObject()
        when (message) {
            is SyncWireMessage.Challenge -> {
                obj.addProperty("type", T_CHALLENGE)
                obj.addProperty("nonce", message.nonce)
                if (message.transport.isNotEmpty()) obj.addProperty("transport", message.transport)
            }
            is SyncWireMessage.Hello -> {
                obj.addProperty("type", T_HELLO)
                obj.addProperty("deviceId", message.deviceId)
                if (message.deviceName.isNotBlank()) obj.addProperty("deviceName", message.deviceName)
                obj.addProperty("auth", message.auth)
                if (message.transport.isNotEmpty()) obj.addProperty("transport", message.transport)
                if (message.clientNonce.isNotEmpty()) obj.addProperty("clientNonce", message.clientNonce)
                if (message.sinceHlc != null) {
                    obj.add("sinceHlc", gson.toJsonTree(message.sinceHlc))
                } else {
                    obj.add("sinceHlc", null)
                }
            }
            is SyncWireMessage.Welcome -> {
                obj.addProperty("type", T_WELCOME)
                obj.addProperty("deviceId", message.deviceId)
                obj.addProperty("name", message.name)
            }
            is SyncWireMessage.Event -> {
                obj.addProperty("type", T_EVENT)
                obj.add("event", JsonParser.parseString(eventCodec.encode(message.event)))
            }
            is SyncWireMessage.Ack -> {
                obj.addProperty("type", T_ACK)
                obj.add("upToHlc", gson.toJsonTree(message.upToHlc))
            }
            is SyncWireMessage.Error -> {
                obj.addProperty("type", T_ERROR)
                obj.addProperty("code", message.code)
                obj.addProperty("message", message.message)
            }
            is SyncWireMessage.Secure -> {
                obj.addProperty("type", T_SECURE)
                obj.addProperty("seq", message.seq)
                obj.addProperty("data", message.data)
            }
            is SyncWireMessage.BlobRequest -> {
                obj.addProperty("type", T_BLOB_REQUEST)
                obj.addProperty("hash", message.hash)
                obj.addProperty("mediaType", message.mediaType)
            }
            is SyncWireMessage.BlobChunk -> {
                obj.addProperty("type", T_BLOB_CHUNK)
                obj.addProperty("hash", message.hash)
                obj.addProperty("mediaType", message.mediaType)
                obj.addProperty("offset", message.offset)
                obj.addProperty("totalSize", message.totalSize)
                obj.addProperty("data", message.data)
                obj.addProperty("isLast", message.isLast)
            }
            is SyncWireMessage.BlobMissing -> {
                obj.addProperty("type", T_BLOB_MISSING)
                obj.addProperty("hash", message.hash)
            }
        }
        return obj.toString()
    }

    fun decode(text: String): SyncWireMessage {
        val obj = JsonParser.parseString(text).asJsonObject
        return when (val type = obj["type"].asString) {
            T_CHALLENGE -> SyncWireMessage.Challenge(
                obj["nonce"].asString,
                obj["transport"]?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
            )
            T_HELLO -> SyncWireMessage.Hello(
                deviceId = obj["deviceId"].asString,
                auth = obj["auth"].asString,
                sinceHlc = obj["sinceHlc"]?.takeIf { !it.isJsonNull }?.let {
                    gson.fromJson(it, HlcTimestamp::class.java)
                },
                deviceName = obj["deviceName"]?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                transport = obj["transport"]?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
                clientNonce = obj["clientNonce"]?.takeIf { !it.isJsonNull }?.asString.orEmpty(),
            )
            T_WELCOME -> SyncWireMessage.Welcome(
                deviceId = obj["deviceId"].asString,
                name = obj["name"].asString
            )
            T_EVENT -> SyncWireMessage.Event(
                event = eventCodec.decode(obj["event"].toString())
            )
            T_ACK -> SyncWireMessage.Ack(
                upToHlc = gson.fromJson(obj["upToHlc"], HlcTimestamp::class.java)
            )
            T_ERROR -> SyncWireMessage.Error(
                code = obj["code"].asString,
                message = obj["message"].asString
            )
            T_SECURE -> SyncWireMessage.Secure(obj["seq"].asLong, obj["data"].asString)
            T_BLOB_REQUEST -> SyncWireMessage.BlobRequest(
                hash = obj["hash"].asString,
                mediaType = obj["mediaType"].asString
            )
            T_BLOB_CHUNK -> SyncWireMessage.BlobChunk(
                hash = obj["hash"].asString,
                mediaType = obj["mediaType"].asString,
                offset = obj["offset"].asInt,
                totalSize = obj["totalSize"].asInt,
                data = obj["data"].asString,
                isLast = obj["isLast"].asBoolean
            )
            T_BLOB_MISSING -> SyncWireMessage.BlobMissing(hash = obj["hash"].asString)
            else -> throw IllegalArgumentException("Unknown wire message type: $type")
        }
    }

    companion object {
        const val T_CHALLENGE = "Challenge"
        const val T_HELLO = "Hello"
        const val T_WELCOME = "Welcome"
        const val T_EVENT = "Event"
        const val T_ACK = "Ack"
        const val T_ERROR = "Error"
        const val T_SECURE = "Secure"
        const val T_BLOB_REQUEST = "BlobRequest"
        const val T_BLOB_CHUNK = "BlobChunk"
        const val T_BLOB_MISSING = "BlobMissing"
    }
}
