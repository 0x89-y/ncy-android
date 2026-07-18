package com.ncy.notechaty.data.sync.pear

import android.content.Context
import android.util.Log
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import to.holepunch.bare.kit.IPC
import to.holepunch.bare.kit.Worklet

/**
 * Owns the experimental Bare worklet in Ncy Dev.
 *
 * This first stage is deliberately network-free: it proves native loading, IPC, and Android
 * lifecycle behavior before Hyperswarm is bundled. Construction has no side effects; Bare's
 * native library is loaded only after the user explicitly enables Pear and [start] is called.
 */
@Singleton
class PearRuntimeController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var worklet: Worklet? = null
    private var ipc: IPC? = null
    private var incoming = ""
    private val listeners = CopyOnWriteArraySet<PearRuntimeListener>()
    private var allowedPeers: Set<String> = emptySet()
    private val _status = MutableStateFlow<PearRuntimeStatus>(PearRuntimeStatus.Disabled)
    val status: StateFlow<PearRuntimeStatus> = _status.asStateFlow()

    @Synchronized
    fun start() {
        if (worklet != null) return
        _status.value = PearRuntimeStatus.Starting

        runCatching {
            val nextWorklet = Worklet(Worklet.Options().memoryLimit(MEMORY_LIMIT_BYTES))
            val nextIpc = IPC(nextWorklet)
            nextIpc.readable {
                while (true) {
                    val data = nextIpc.read() ?: break
                    receive(data.toUtf8String())
                }
            }
            val bundle = context.assets.open(WORKLET_ASSET).use { it.readBytes() }
            nextWorklet.start("/ncy-pear.bundle", ByteBuffer.wrap(bundle), emptyArray())
            worklet = nextWorklet
            ipc = nextIpc
            val seed = identitySeed()
            command("start", "seed" to seed)
            sendAllowedPeers()
        }.onFailure { error ->
            Log.e(TAG, "Could not start Bare worklet", error)
            stop()
            _status.value = PearRuntimeStatus.Failed(error.message ?: error.javaClass.simpleName)
        }
    }

    @Synchronized
    fun stop() {
        val activeIpc = ipc
        val activeWorklet = worklet
        ipc = null
        worklet = null
        incoming = ""
        runCatching { activeIpc?.close() }
        runCatching { activeWorklet?.terminate() }
        _status.value = PearRuntimeStatus.Disabled
    }

    @Synchronized
    fun allowPeers(publicKeys: Set<String>) {
        allowedPeers = publicKeys
        sendAllowedPeers()
    }

    fun connect(publicKey: String) = command("connect", "publicKey" to publicKey)

    fun send(connectionId: String, text: String): Boolean {
        if (text.length > MAX_FRAME_CHARS || ipc == null) return false
        command(
            "send",
            "id" to connectionId,
            "data" to Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP),
        )
        return true
    }

    fun close(connectionId: String) = command("close", "id" to connectionId)

    fun addListener(listener: PearRuntimeListener) { listeners += listener }
    fun removeListener(listener: PearRuntimeListener) { listeners -= listener }

    @Synchronized
    private fun receive(chunk: String) {
        incoming += chunk
        if (incoming.length > MAX_IPC_CHARS) {
            incoming = ""
            _status.value = PearRuntimeStatus.Failed("Pear IPC response exceeded limit")
            return
        }
        while (true) {
            val newline = incoming.indexOf('\n')
            if (newline < 0) return
            val line = incoming.substring(0, newline)
            incoming = incoming.substring(newline + 1)
            if (line.isBlank()) continue
            Log.i(TAG, "Bare IPC: $line")
            runCatching { JsonParser.parseString(line).asJsonObject }.onSuccess { message ->
                when (message["type"]?.asString) {
                    "ready" -> _status.value = PearRuntimeStatus.Ready(message["publicKey"].asString)
                    "connected" -> listeners.forEach {
                        it.onConnected(
                            message["id"].asString,
                            message["publicKey"].asString,
                            message["client"]?.asBoolean == true,
                        )
                    }
                    "frame" -> {
                        val text = Base64.decode(message["data"].asString, Base64.NO_WRAP)
                            .toString(StandardCharsets.UTF_8)
                        listeners.forEach { it.onFrame(message["id"].asString, text) }
                    }
                    "disconnected" -> listeners.forEach {
                        it.onDisconnected(message["id"].asString, message["publicKey"].asString)
                    }
                    "connection-error" -> listeners.forEach {
                        it.onConnectionError(message["message"]?.asString ?: "Pear connection failed")
                    }
                    "error", "fatal" -> _status.value = PearRuntimeStatus.Failed(
                        message["message"]?.asString ?: "Unknown Pear error"
                    )
                }
            }
        }
    }

    @Synchronized
    private fun sendAllowedPeers() {
        val target = ipc ?: return
        val message = JsonObject().apply {
            addProperty("type", "allow")
            add("publicKeys", JsonArray().also { array -> allowedPeers.forEach(array::add) })
        }
        write(target, message.toString() + '\n')
    }

    @Synchronized
    private fun command(type: String, vararg fields: Pair<String, String>) {
        val target = ipc ?: return
        val message = JsonObject().apply {
            addProperty("type", type)
            fields.forEach { (key, value) -> addProperty(key, value) }
        }
        write(target, message.toString() + '\n')
    }

    private fun write(target: IPC, message: String) {
        target.write(ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))) { error ->
            if (error != null) {
                Log.e(TAG, "Could not write to Bare IPC", error)
                _status.value = PearRuntimeStatus.Failed(error.message ?: "Pear IPC write failed")
            }
        }
    }

    private fun identitySeed(): String {
        val prefs = context.getSharedPreferences(IDENTITY_PREFS, Context.MODE_PRIVATE)
        prefs.getString(IDENTITY_SEED, null)?.let { return it }
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP).also {
            check(prefs.edit().putString(IDENTITY_SEED, it).commit()) {
                "Could not persist Pear identity"
            }
        }
    }

    private fun ByteBuffer.toUtf8String(): String {
        val bytes = ByteArray(remaining())
        get(bytes)
        return bytes.toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val TAG = "NcyPear"
        const val MEMORY_LIMIT_BYTES = 24 * 1024 * 1024
        const val MAX_IPC_CHARS = 2 * 1024 * 1024
        const val MAX_FRAME_CHARS = 1400 * 1024
        const val WORKLET_ASSET = "ncy-pear.bundle"
        const val IDENTITY_PREFS = "ncy_pear_identity"
        const val IDENTITY_SEED = "seed"
    }
}
