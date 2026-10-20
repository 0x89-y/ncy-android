package com.ncy.notechaty.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Wrong-PIN backoff shared by every PIN prompt (app lock, protected chats, profiles).
 *
 * A 4-digit PIN is 10^4 guesses, and nothing used to slow someone holding the
 * phone from working through them. The first few mistakes are free; after that
 * each one doubles the wait, up to [MAX_LOCKOUT_MS]. Persisted, so killing the app
 * does not reset it, and enforced inside the verifiers rather than the prompts, so
 * leaving and reopening a screen does not either.
 */
object PinAttempts {
    const val FREE_ATTEMPTS = 4
    const val FIRST_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 30 * 60_000L

    private const val PREFS = "ncy_pin_attempts"
    private const val KEY_FAILURES = "failures"
    private const val KEY_LOCKED_UNTIL = "locked_until"

    private var prefs: SharedPreferences? = null

    /** Call once from Application.onCreate, before any PIN can be checked. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Lockout after the [failures]th consecutive wrong PIN. */
    fun lockoutMsAfter(failures: Int): Long {
        if (failures <= FREE_ATTEMPTS) return 0L
        val doublings = (failures - FREE_ATTEMPTS - 1).coerceAtMost(16)
        return (FIRST_LOCKOUT_MS shl doublings).coerceAtMost(MAX_LOCKOUT_MS)
    }

    @Synchronized
    fun remainingLockoutMs(now: Long = System.currentTimeMillis()): Long {
        val until = prefs?.getLong(KEY_LOCKED_UNTIL, 0L) ?: return 0L
        // Clamped: a clock set backwards would otherwise stretch the wait indefinitely.
        return (until - now).coerceIn(0L, MAX_LOCKOUT_MS)
    }

    fun isLockedOut(): Boolean = remainingLockoutMs() > 0L

    @Synchronized
    fun recordFailure(now: Long = System.currentTimeMillis()) {
        val p = prefs ?: return
        val failures = p.getInt(KEY_FAILURES, 0) + 1
        p.edit()
            .putInt(KEY_FAILURES, failures)
            .putLong(KEY_LOCKED_UNTIL, now + lockoutMsAfter(failures))
            .apply()
    }

    @Synchronized
    fun recordSuccess() {
        prefs?.edit()?.clear()?.apply()
    }
}

/** The PIN field's helper line: the lockout wins over a plain "wrong PIN". */
fun pinSupportingText(wrongPin: Boolean, lockoutSeconds: Int): (@Composable () -> Unit)? = when {
    lockoutSeconds > 0 -> { { Text("Too many attempts. Try again in ${formatWait(lockoutSeconds)}.") } }
    wrongPin -> { { Text("Wrong PIN") } }
    else -> null
}

private fun formatWait(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${(seconds + 59) / 60} min"

/** Seconds left on the PIN lockout, ticking down while a prompt is on screen. */
@Composable
fun rememberPinLockoutSeconds(attemptKey: Any?): Int {
    var seconds by remember { mutableIntStateOf(0) }
    // Re-read after every attempt ([attemptKey] changes) and once a second while locked.
    LaunchedEffect(attemptKey) {
        while (true) {
            seconds = ((PinAttempts.remainingLockoutMs() + 999) / 1000).toInt()
            if (seconds == 0) break
            delay(1000)
        }
    }
    return seconds
}
