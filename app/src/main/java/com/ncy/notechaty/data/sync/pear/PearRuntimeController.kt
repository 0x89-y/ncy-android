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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import to.holepunch.bare.kit.IPC
import to.holepunch.bare.kit.Worklet

/**
 * Owns the opt-in Bare/Hyperswarm worklet. Construction has no side effects; Bare's native
 * library is loaded only after the user explicitly enables Pear and [start] is called.
 */
@Singleton
class PearRuntimeController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var worklet: Worklet? = null
    private var ipc: IPC? = null
    private var incoming = ""
    private var pendingSeed: String? = null
    private val pendingWrites = ArrayDeque<ByteBuffer>()
    private val listeners = CopyOnWriteArraySet<PearRuntimeListener>()
    /** IPC callbacks arrive on Android's main thread. Keep ordered protocol work off it. */
    private val listenerExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "NcyPear-events").apply { isDaemon = true }
    }
    private var allowedPeers: Set<String> = emptySet()
    /** Set from the worker's `backpressure` signal; read by senders — see [isCongested]. */
    private val congested = AtomicBoolean(false)
    /** Frames handed to the worker that it has not yet confirmed are on the wire. */
    private val inFlight = AtomicInteger(0)
    private val _status = MutableStateFlow<PearRuntimeStatus>(PearRuntimeStatus.Disabled)
    val status: StateFlow<PearRuntimeStatus> = _status.asStateFlow()

    @Synchronized
    fun start() {
        if (worklet != null) return
        Log.i(TAG, "Starting Pear worklet")
        _status.value = PearRuntimeStatus.Starting

        runCatching {
            val nextWorklet = Worklet(Worklet.Options().memoryLimit(MEMORY_LIMIT_BYTES))
            Log.i(TAG, "Bare worklet created")
            val bundle = context.assets.open(WORKLET_ASSET).use { it.readBytes() }
            worklet = nextWorklet
            pendingSeed = identitySeed()
            nextWorklet.start("/ncy-pear.bundle", ByteBuffer.wrap(bundle), emptyArray())
            Log.i(TAG, "Pear bundle started; waiting for worker boot")
            val nextIpc = IPC(nextWorklet)
            ipc = nextIpc
            readNext(nextIpc)
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
        pendingSeed = null
        pendingWrites.clear()
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
    fun cancelConnect(publicKey: String) = command("cancel-connect", "publicKey" to publicKey)

    fun send(connectionId: String, text: String): Boolean {
        if (text.length > MAX_FRAME_CHARS || ipc == null) return false
        inFlight.incrementAndGet()
        command(
            "send",
            "id" to connectionId,
            "data" to Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP),
        )
        return true
    }

    fun close(connectionId: String) = command("close", "id" to connectionId)

    /**
     * True while the worker has reported that its command queue is backed up.
     *
     * The IPC channel to the worker cannot apply normal stream backpressure, so the
     * worker signals congestion explicitly and senders throttle at the source.
     * Without it a blob is enqueued in full before any of it reaches the wire, which
     * buries the connection and stalls every other frame behind it.
     */
    fun isCongested(): Boolean = congested.get() || inFlight.get() >= MAX_FRAMES_IN_FLIGHT

    /** Reset after a disconnect so a lost ack cannot wedge the window shut. */
    private fun resetFlowControl() {
        inFlight.set(0)
        congested.set(false)
    }

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
            listenerExecutor.execute { handleLine(line) }
        }
    }

    private fun handleLine(line: String) {
        runCatching { JsonParser.parseString(line).asJsonObject }.onSuccess { message ->
            when (message["type"]?.asString) {
                "booted" -> {
                    val seed = synchronized(this) {
                        pendingSeed?.also { pendingSeed = null }
                    } ?: return@onSuccess
                    Log.i(TAG, "Pear worker booted; starting Hyperswarm")
                    command("start", "seed" to seed)
                    sendAllowedPeers()
                }
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
                    val connectionId = message["id"].asString
                    listeners.forEach { it.onFrame(connectionId, text) }
                }
                "sent" -> if (inFlight.get() > 0) inFlight.decrementAndGet()
                "log" -> Log.w(TAG, "worker: ${message["message"]?.asString}")
                "backpressure" -> congested.set(message["active"]?.asBoolean == true)
                "disconnected" -> {
                    resetFlowControl()
                    listeners.forEach {
                        it.onDisconnected(message["id"].asString, message["publicKey"].asString)
                    }
                }
                "connection-error", "command-error" -> {
                    // A send that failed will never be acked; release its credit or
                    // the window stays shut for the life of the connection.
                    if (message["command"]?.asString == "send" && inFlight.get() > 0) {
                        inFlight.decrementAndGet()
                    }
                    listeners.forEach {
                        it.onConnectionError(
                            message["publicKey"]?.takeUnless { value -> value.isJsonNull }?.asString,
                            message["id"]?.takeUnless { value -> value.isJsonNull }?.asString,
                            message["message"]?.asString ?: "Pear connection failed",
                        )
                    }
                }
                "error", "fatal" -> _status.value = PearRuntimeStatus.Failed(
                    message["message"]?.asString ?: "Unknown Pear error"
                )
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
        synchronized(this) {
            pendingWrites.addLast(ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8)))
            drainWrites(target)
        }
    }

    /** Bare Kit returns a negative errno while the pipe is backpressured. Its v2.3.0
     * async helper treats that errno as a ByteBuffer position, so queue and retry via
     * the writable notification instead. */
    @Synchronized
    private fun drainWrites(target: IPC) {
        if (ipc !== target) return
        runCatching {
            while (pendingWrites.isNotEmpty()) {
                val buffer = pendingWrites.first()
                // Capture the size *before* the call. Bare Kit caps a write at 64 KiB
                // but drains the buffer to remaining()==0 regardless, so comparing
                // `written` against the post-write remaining always looked like a
                // complete write — silently discarding the tail of every frame larger
                // than 64 KiB. A blob chunk is ~155 KB once base64 has been applied
                // twice, so most of it never reached the worker and the peer received
                // a JSON line with no terminating newline.
                val start = buffer.position()
                val pending = buffer.remaining()
                val written = target.write(buffer)
                if (written <= 0) {
                    // Backpressure (negative errno) — leave it queued and retry.
                    buffer.position(start)
                    target.writable { drainWrites(target) }
                    return
                }
                if (written >= pending) {
                    pendingWrites.removeFirst()
                } else {
                    // Partial write: reposition explicitly rather than trusting where
                    // Bare Kit left it, and wait to be told the pipe is writable again.
                    Log.d(TAG, "Bare IPC partial write $written/$pending; requeueing tail")
                    buffer.position(start + written)
                    target.writable { drainWrites(target) }
                    return
                }
            }
            target.writable(null)
        }.onFailure { error ->
            pendingWrites.clear()
            Log.e(TAG, "Could not write to Bare IPC", error)
            _status.value = PearRuntimeStatus.Failed(error.message ?: "Pear IPC write failed")
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

    private fun readNext(target: IPC) {
        target.read { data, error ->
            if (error != null) {
                Log.e(TAG, "Could not read Bare IPC", error)
                _status.value = PearRuntimeStatus.Failed(error.message ?: "Pear IPC read failed")
                return@read
            }
            if (data != null) receive(data.toUtf8String())
            if (ipc === target) readNext(target)
        }
    }

    private companion object {
        const val TAG = "NcyPear"
        const val MEMORY_LIMIT_BYTES = 24 * 1024 * 1024

        /**
         * Frames the worker may owe an ack for before senders must wait.
         *
         * The worklet runs under [MEMORY_LIMIT_BYTES], and one blob chunk is ~155 KB
         * of IPC JSON — held as a UTF-16 JS string, ~310 KB. Handing over a whole
         * multi-megabyte blob at once therefore exhausts the worklet's heap and kills
         * it, which the peer sees as a connection reset and zero bytes delivered.
         * Four frames is ~1.2 MB in flight: enough to keep the link busy, nowhere
         * near the ceiling.
         */
        const val MAX_FRAMES_IN_FLIGHT = 4
        const val MAX_IPC_CHARS = 2 * 1024 * 1024
        const val MAX_FRAME_CHARS = 1400 * 1024
        const val WORKLET_ASSET = "ncy-pear.bundle"
        const val IDENTITY_PREFS = "ncy_pear_identity"
        const val IDENTITY_SEED = "seed"
    }
}
