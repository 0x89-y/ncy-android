package com.ncy.notechaty.data.sync.event

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Receipt sequence: the order this installation stored events in (protocol §10.9).
 *
 * An HLC records when an event was *written*, not when this device *got* it, so
 * catch-up by HLC silently skipped anything that arrived late with an old clock —
 * a phone that was offline for a day brings day-old notes, and every peer whose
 * watermark had moved past them never received them. Every newly stored event
 * gets the next receipt here instead, and catch-up and forwarding follow that.
 *
 * Append-only NDJSON (`{"s":1,"c":"<chat>","e":"<event>"}`) next to an epoch file,
 * the same layout ncy-core uses. A missing, unreadable or out-of-order log starts a
 * new epoch, which makes every peer fall back to HLC catch-up once instead of
 * trusting positions that no longer mean anything.
 */
class ReceiptLog(dir: File) {
    data class Receipt(
        @SerializedName("s") val seq: Long,
        @SerializedName("c") val chatId: String,
        @SerializedName("e") val eventId: String,
    )

    private val file = File(dir, LOG_FILE)
    private val epochFile = File(dir, EPOCH_FILE)
    private val gson = Gson()
    /** Ascending by seq, so a range is a binary search away. */
    private val entries = ArrayList<Receipt>()
    private val byEvent = HashMap<String, Long>()

    /** True when this open began a new epoch, so nothing stored is numbered yet. */
    val fresh: Boolean
    val epoch: String

    init {
        dir.mkdirs()
        val existing = runCatching { epochFile.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
        val loaded = existing?.let { readEntries() }
        if (existing != null && loaded != null) {
            epoch = existing
            loaded.forEach { add(it) }
            endTornLine()
            fresh = false
        } else {
            // Positions peers hold for any old epoch mean nothing now.
            epoch = UUID.randomUUID().toString()
            file.delete()
            epochFile.writeText(epoch)
            fresh = true
        }
    }

    @Synchronized
    fun latest(): Long = entries.lastOrNull()?.seq ?: 0L

    @Synchronized
    fun receiptOf(eventId: String): Long? = byEvent[eventId]

    /** Number [eventId] if it is new, returning its receipt either way. */
    @Synchronized
    fun record(chatId: String, eventId: String): Long {
        byEvent[eventId]?.let { return it }
        val receipt = Receipt(latest() + 1, chatId, eventId)
        FileOutputStream(file, /* append = */ true).use { out ->
            out.write(gson.toJson(receipt).toByteArray(Charsets.UTF_8))
            out.write('\n'.code)
            out.flush()
            out.fd.sync()
        }
        add(receipt)
        return receipt.seq
    }

    /** Every receipt numbered after [after], in order. */
    @Synchronized
    fun after(after: Long): List<Receipt> {
        var lo = 0
        var hi = entries.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (entries[mid].seq <= after) lo = mid + 1 else hi = mid
        }
        return entries.subList(lo, entries.size).toList()
    }

    private fun add(receipt: Receipt) {
        entries += receipt
        byEvent[receipt.eventId] = receipt.seq
    }

    /** Null when the log is unusable as a sequence (out of order or repeated). */
    private fun readEntries(): List<Receipt>? {
        if (!file.isFile) return emptyList()
        val out = ArrayList<Receipt>()
        file.useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                // A torn final line from a crash mid-append is the only expected damage.
                val receipt = runCatching { gson.fromJson(line, Receipt::class.java) }.getOrNull()
                    ?.takeIf { it.seq > 0 && !it.eventId.isNullOrEmpty() && !it.chatId.isNullOrEmpty() }
                    ?: continue
                if (out.isNotEmpty() && receipt.seq <= out.last().seq) return null
                out += receipt
            }
        }
        return out
    }

    /** End a torn final line, so the next record is not glued to the fragment and lost. */
    private fun endTornLine() {
        if (!file.isFile || file.length() == 0L) return
        val last = file.inputStream().use { input ->
            input.skip(file.length() - 1)
            input.read()
        }
        if (last != '\n'.code) FileOutputStream(file, true).use { it.write('\n'.code) }
    }

    companion object {
        const val LOG_FILE = "receipts.log"
        const val EPOCH_FILE = "receipts.epoch"
    }
}

/**
 * Process-wide access to the receipt log. [EventLog] has no injector — it is built
 * from a file wherever an event is written — so the log is reached statically,
 * the way [EventLog] writers need it. Not initialised (unit tests) means no-op.
 */
object Receipts {
    @Volatile private var dir: File? = null
    private var log: ReceiptLog? = null

    /** Call from Application.onCreate, before anything can write an event. */
    fun init(dir: File) {
        this.dir = dir
    }

    /** Opened lazily, off the main thread, by whoever first needs it. */
    @Synchronized
    fun log(): ReceiptLog? {
        log?.let { return it }
        val target = dir ?: return null
        return runCatching { ReceiptLog(target) }
            .onFailure { Log.w(TAG, "receipt log unavailable", it) }
            .getOrNull()
            ?.also { log = it }
    }

    /**
     * Number a newly stored event. A failure is logged, not raised: the event is
     * already durable, and the next startup numbers anything left without one.
     */
    fun record(event: ChatEvent): Long? = runCatching { log()?.record(event.chatId, event.eventId) }
        .onFailure { Log.w(TAG, "could not record receipt for ${event.eventId}", it) }
        .getOrNull()

    fun receiptOf(eventId: String): Long? = log()?.receiptOf(eventId)

    fun epoch(): String? = log()?.epoch

    private const val TAG = "Receipts"
}
