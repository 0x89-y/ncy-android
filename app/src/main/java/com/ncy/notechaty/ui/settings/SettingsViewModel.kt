package com.ncy.notechaty.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.backup.BackupScheduler
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.AppSettings
import com.ncy.notechaty.data.model.ThemeMode
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import com.ncy.notechaty.data.sync.transport.PhoneSyncServer
import com.ncy.notechaty.security.ChatLockState
import com.ncy.notechaty.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    fun setTopBarGradient(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings.value.copy(topBarGradient = enabled))
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
