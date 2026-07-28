package com.ncy.notechaty.data.sync.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live state of incoming media transfers, for the UI.
 *
 * Without this the only thing a message could say was "downloading…", which looked
 * identical whether bytes were arriving, the transfer had quietly died, or nothing
 * had ever started. Progress and failures are held here, keyed by blob hash, and the
 * transfer paths report into it.
 *
 * Deliberately not persisted: it describes transfers in this process, and a stale
 * "43%" restored from disk would be a lie. Anything durable — whether the bytes are
 * on disk, whether they were evicted — is answered by the repository.
 */
@Singleton
class MediaTransferState @Inject constructor() {

    data class Progress(val received: Long, val total: Long)

    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress.asStateFlow()

    private val _failures = MutableStateFlow<Map<String, String>>(emptyMap())
    val failures: StateFlow<Map<String, String>> = _failures.asStateFlow()

    /**
     * Bumped whenever media presence may have changed — an arrival, an eviction, a
     * failure.
     *
     * Whether a message holds its blob is answered by touching the disk, which
     * composition does once and then caches. Without a signal to invalidate on, a file
     * that arrives (or is removed) while its chat is open keeps rendering the old
     * answer until the screen is left and re-entered.
     */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun bump() = _version.update { it + 1 }

    /** Reported for a transfer the user stopped, so the UI can present a choice
     *  rather than an error. Matched verbatim by the message UI. */
    companion object {
        const val CANCELLED = "cancelled"
        const val EVICTED = "removed from this device"
    }

    fun started(hash: String, total: Long) {
        _failures.update { if (hash in it) it - hash else it }
        _progress.update { it + (hash to Progress(0, total)) }
        bump()
    }

    fun advanced(hash: String, received: Long, total: Long) {
        _progress.update { it + (hash to Progress(received, total)) }
    }

    fun finished(hash: String) {
        _progress.update { if (hash in it) it - hash else it }
        _failures.update { if (hash in it) it - hash else it }
        bump()
    }

    fun failed(hash: String, reason: String) {
        _progress.update { if (hash in it) it - hash else it }
        _failures.update { it + (hash to reason) }
        bump()
    }

    /** Forget both states — used when a retry starts, so it does not show as failed. */
    fun clear(hash: String) {
        _progress.update { if (hash in it) it - hash else it }
        _failures.update { if (hash in it) it - hash else it }
        bump()
    }

    private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        while (true) {
            val current = value
            val next = transform(current)
            if (current === next || compareAndSet(current, next)) return
        }
    }
}
