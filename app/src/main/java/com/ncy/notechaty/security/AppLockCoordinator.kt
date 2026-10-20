package com.ncy.notechaty.security

import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.protection.ProtectionKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-locks everything once the app has been in the background longer than the
 * "Re-lock chats" grace period: the app lock, chat and profile unlocks, and the
 * protected-chat keys, which otherwise stayed in memory until the process died.
 *
 * A timer clears them while backgrounded; returning re-checks the elapsed time
 * too, because a frozen or dozing process may never have run the timer.
 */
@Singleton
class AppLockCoordinator @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val chatLockState: ChatLockState,
    private val profileLockState: ProfileLockState,
    private val keyStore: ProtectionKeyStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var backgroundedAt = 0L
    private var lockJob: Job? = null

    private val _lockCount = MutableStateFlow(0)
    /** Bumps on every re-lock, so open screens can drop what they decrypted. */
    val lockCount: StateFlow<Int> = _lockCount.asStateFlow()

    private val _verifiedAppPin = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /**
     * The PIN that just passed the app lock, for a protected chat left open under it.
     * Its key needs the PIN itself, so without this the user typed it twice in a
     * row. Not replayed: only a screen open at that moment receives it.
     */
    val verifiedAppPin: SharedFlow<String> = _verifiedAppPin.asSharedFlow()

    /** Call only after the PIN has been verified. */
    fun appUnlockedWithPin(pin: String) {
        _verifiedAppPin.tryEmit(pin)
    }

    @Synchronized
    fun onBackground(now: Long = System.currentTimeMillis()) {
        backgroundedAt = now
        lockJob?.cancel()
        val delayMs = relockAfterMs(settingsRepository.settings.value.protectionGracePeriodMinutes)
        lockJob = scope.launch {
            delay(delayMs)
            lockNow()
        }
    }

    @Synchronized
    fun onForeground(now: Long = System.currentTimeMillis()) {
        lockJob?.cancel()
        lockJob = null
        val since = backgroundedAt
        backgroundedAt = 0L
        val delayMs = relockAfterMs(settingsRepository.settings.value.protectionGracePeriodMinutes)
        if (since > 0L && now - since >= delayMs) lockNow()
    }

    @Synchronized
    private fun lockNow() {
        keyStore.lockAll()
        // Opened copies of sealed attachments exist only while their chat is unlocked.
        com.ncy.notechaty.data.sync.protection.UnsealedMedia.clear()
        chatLockState.lockAll()
        profileLockState.lockAll()
        _lockCount.value++
    }

    companion object {
        /**
         * Floor for the "Always" setting. Picking a photo, scanning a QR code or
         * taking a picture also backgrounds the app; re-locking the moment that
         * happens would drop what was being attached and prompt again on return.
         */
        const val MIN_RELOCK_MS = 60_000L

        fun relockAfterMs(gracePeriodMinutes: Int): Long =
            maxOf(gracePeriodMinutes * 60_000L, MIN_RELOCK_MS)
    }
}
