package com.ncy.notechaty.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.backup.BackupScheduler
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.AppSettings
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.model.ThemeMode
import com.ncy.notechaty.data.model.SyncTransportMode
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.MediaCleanupReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.AuthenticatedDeviceName
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import com.ncy.notechaty.data.sync.transport.PhoneSyncServer
import com.ncy.notechaty.security.ChatLockState
import com.ncy.notechaty.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val pinManager: PinManager,
    private val chatLockState: ChatLockState,
    private val syncManager: SyncManager,
    private val peerStore: SyncPeerStore,
    private val phoneSyncServer: PhoneSyncServer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    /** Last media-storage scan, or null before one has run. */
    private val _mediaReport = MutableStateFlow<MediaCleanupReport?>(null)
    val mediaReport: StateFlow<MediaCleanupReport?> = _mediaReport.asStateFlow()

    private val _mediaBusy = MutableStateFlow(false)
    val mediaBusy: StateFlow<Boolean> = _mediaBusy.asStateFlow()

    /**
     * Measure the media store, or actually reclaim it.
     *
     * Always measure before deleting: nothing has ever removed a blob, so the first
     * run on a long-lived install can free a surprising amount and the user should
     * see the number first.
     */
    fun scanMedia(dryRun: Boolean = true) {
        if (_mediaBusy.value) return
        viewModelScope.launch {
            _mediaBusy.value = true
            try {
                _mediaReport.value = repository.collectUnreferencedMedia(dryRun)
                // Re-measure after a real sweep so the panel reflects what is left.
                if (!dryRun) _mediaReport.value = repository.collectUnreferencedMedia(true)
            } finally {
                _mediaBusy.value = false
            }
        }
    }

    fun hasPin(): Boolean = pinManager.hasPin()

    fun setPin(pin: String) {
        viewModelScope.launch { pinManager.setPin(pin) }
    }

    fun clearPin() {
        viewModelScope.launch {
            pinManager.clearPin()
            // Unprotect every chat so they remain accessible.
            val chats = repository.observeChats().first()
            chats.filter { it.isProtected }.forEach { c ->
                repository.updateChat(c.copy(isProtected = false, hideName = false))
            }
            chatLockState.lockAll()
        }
    }

    fun setUseBiometric(enabled: Boolean) {
        viewModelScope.launch { pinManager.setUseBiometric(enabled) }
    }

    suspend fun importChat(zipFile: File) {
        val restartHost = phoneSyncServer.enabled.value
        syncManager.stop()
        if (restartHost) phoneSyncServer.pauseForRestore()
        try {
            check(peerStore.resetAllWatermarks()) {
                "Could not persist sync progress reset before restore"
            }
            repository.importChat(zipFile)
        } finally {
            syncManager.start()
            if (restartHost) phoneSyncServer.start()
        }
    }

    /**
     * Ask every paired device to send its whole history again.
     *
     * This has to run here rather than on the desktop. The desktop treats our
     * `Hello.sinceHlc` as authoritative — only this device knows what actually
     * survived a reinstall, a restore, or a connection that dropped mid-stream — so
     * clearing a watermark over there is ignored while ours still says "I have
     * everything up to X". Anything stranded below X, a deletion included, stays
     * stranded until we ask from zero.
     *
     * Reconnects both roles: the watermark is read when the connection is built, so
     * a live session would keep using the old one.
     */
    fun forceFullResync(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val restartHost = phoneSyncServer.enabled.value
            syncManager.stop()
            if (restartHost) phoneSyncServer.stop()
            val ok = peerStore.resetAllWatermarks()
            syncManager.start()
            if (restartHost) phoneSyncServer.start()
            onResult(ok)
        }
    }

    fun setDeviceName(name: String) {
        val normalized = AuthenticatedDeviceName.refresh(settings.value.deviceName, name)
        if (normalized == settings.value.deviceName) return
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSettings(settings.value.copy(deviceName = normalized))
            // Hello/Welcome carry the name only during authentication, so reconnect
            // both roles to propagate the change without altering stable device IDs.
            syncManager.stop()
            syncManager.start()
            if (phoneSyncServer.enabled.value) {
                phoneSyncServer.stop()
                phoneSyncServer.start()
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(themeMode = mode))
        }
    }

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(accentColor = color))
        }
    }

    fun setChatSortMode(mode: ChatSortMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(chatSortMode = mode))
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(autoBackupEnabled = enabled))
            if (enabled) {
                BackupScheduler.schedule(context, settings.value.autoBackupIntervalHours)
            } else {
                BackupScheduler.cancel(context)
            }
        }
    }

    fun setAutoBackupInterval(hours: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(autoBackupIntervalHours = hours))
            if (settings.value.autoBackupEnabled) {
                BackupScheduler.schedule(context, hours)
            }
        }
    }

    fun setSyncTransportMode(mode: SyncTransportMode) {
        viewModelScope.launch {
            if (settings.value.syncTransportMode == mode) return@launch
            settingsRepository.updateSettings(settings.value.copy(syncTransportMode = mode))
            syncManager.stop()
            syncManager.start()
        }
    }

    fun setAutomaticFileDownloadLimit(bytes: Long) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(
                    automaticFileDownloadLimitBytes =
                        bytes.coerceIn(0L, 1024L * 1024 * 1024),
                ),
            )
        }
    }

    fun setLockOnAppStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(lockOnAppStart = enabled))
        }
    }

    fun setProtectionGracePeriod(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(protectionGracePeriodMinutes = minutes))
        }
    }

    /** Full backup: every chat from every profile, plus the profile registry. */
    suspend fun exportEverything(outputFile: File) {
        repository.exportEveryProfile(outputFile)
    }

    suspend fun purgeChatTombstones(): Int = repository.purgeChatTombstones()
}
