package com.ncy.notechaty.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.AppSettings
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ncy_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return try {
            AppSettings(
                themeMode = prefs.getString("themeMode", null)
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                accentColor = prefs.getString("accentColor", null)
                    ?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() }
                    ?: AccentColor.DYNAMIC,
                chatSortMode = prefs.getString("chatSortMode", null)
                    ?.let { runCatching { ChatSortMode.valueOf(it) }.getOrNull() }
                    ?: ChatSortMode.LAST_MESSAGE,
                autoBackupEnabled = prefs.getBoolean("autoBackupEnabled", false),
                autoBackupFolder = prefs.getString("autoBackupFolder", null),
                autoBackupIntervalHours = prefs.getInt("autoBackupIntervalHours", 24),
                topBarGradient = prefs.getBoolean("topBarGradient", false),
                protectionPinHash = prefs.getString("protectionPinHash", null),
                protectionPinSalt = prefs.getString("protectionPinSalt", null),
                protectionUseBiometric = prefs.getBoolean("protectionUseBiometric", false)
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        prefs.edit()
            .putString("themeMode", settings.themeMode.name)
            .putString("accentColor", settings.accentColor.name)
            .putString("chatSortMode", settings.chatSortMode.name)
            .putBoolean("autoBackupEnabled", settings.autoBackupEnabled)
            .putString("autoBackupFolder", settings.autoBackupFolder)
            .putInt("autoBackupIntervalHours", settings.autoBackupIntervalHours)
            .putBoolean("topBarGradient", settings.topBarGradient)
            .putString("protectionPinHash", settings.protectionPinHash)
            .putString("protectionPinSalt", settings.protectionPinSalt)
            .putBoolean("protectionUseBiometric", settings.protectionUseBiometric)
            .apply()
    }
}
