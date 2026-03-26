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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    suspend fun importChat(zipFile: File) {
        repository.importChat(zipFile)
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

    suspend fun exportAllChats(outputFile: File) {
        repository.exportAllChats(outputFile)
    }
}
