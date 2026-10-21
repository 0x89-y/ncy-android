package com.ncy.notechaty.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.AppSettings
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.model.ThemeMode
import com.ncy.notechaty.data.model.SyncTransportMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ncy_settings", Context.MODE_PRIVATE)
    // Network consent is installation-local and deliberately excluded from backup/transfer.
    private val transportPrefs: SharedPreferences =
        context.getSharedPreferences("ncy_transport_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return try {
            AppSettings(
                deviceName = prefs.getString("deviceName", null)
                    ?.trim()
                    ?.take(80)
                    ?.takeIf { it.isNotEmpty() }
                    ?: defaultDeviceName(),
                themeMode = prefs.getString("themeMode", null)
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                accentColor = AccentColor.fromStored(prefs.getString("accentColor", null))
                    ?: AccentColor.PURPLE,
                chatSortMode = prefs.getString("chatSortMode", null)
                    ?.let { runCatching { ChatSortMode.valueOf(it) }.getOrNull() }
                    ?: ChatSortMode.LAST_MESSAGE,
                autoBackupEnabled = prefs.getBoolean("autoBackupEnabled", false),
                autoBackupFolder = prefs.getString("autoBackupFolder", null),
                autoBackupIntervalHours = prefs.getInt("autoBackupIntervalHours", 24),
                automaticFileDownloadLimitBytes = prefs.getLong(
                    "automaticFileDownloadLimitBytes",
                    100L * 1024 * 1024,
                ).coerceIn(0L, 1024L * 1024 * 1024),
                mediaBackfillCutoffMs = prefs.getLong("mediaBackfillCutoffMs", 0L),
                syncTransportMode = transportPrefs.getString(KEY_TRANSPORT_MODE, null)
                    ?.let { runCatching { SyncTransportMode.valueOf(it) }.getOrNull() }
                    ?: if (transportPrefs.getBoolean(LEGACY_PEAR_ENABLED, false)) {
                        SyncTransportMode.LOCAL_PREFERRED
                    } else SyncTransportMode.LOCAL_ONLY,
                protectionPinHash = prefs.getString("protectionPinHash", null),
                protectionPinSalt = prefs.getString("protectionPinSalt", null),
                protectionUseBiometric = prefs.getBoolean("protectionUseBiometric", false),
                lockOnAppStart = prefs.getBoolean("lockOnAppStart", false),
                protectionGracePeriodMinutes = prefs.getInt("protectionGracePeriodMinutes", 0)
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    /**
     * Record that this device has joined a mesh, so media predating the moment is
     * listed rather than downloaded.
     *
     * Only the first pairing takes effect. Once stamped, the value is the instant
     * this device became part of the mesh; a later pairing must not move it forward
     * or it would re-classify media that has already arrived.
     */
    suspend fun stampMediaBackfillCutoff() {
        val current = _settings.value
        if (current.mediaBackfillCutoffMs > 0L) return
        updateSettings(current.copy(mediaBackfillCutoffMs = System.currentTimeMillis()))
    }

    suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        prefs.edit()
            .putString("deviceName", settings.deviceName.trim().take(80))
            .putString("themeMode", settings.themeMode.name)
            .putString("accentColor", settings.accentColor.name)
            .putString("chatSortMode", settings.chatSortMode.name)
            .putBoolean("autoBackupEnabled", settings.autoBackupEnabled)
            .putString("autoBackupFolder", settings.autoBackupFolder)
            .putInt("autoBackupIntervalHours", settings.autoBackupIntervalHours)
            .putLong(
                "automaticFileDownloadLimitBytes",
                settings.automaticFileDownloadLimitBytes.coerceIn(0L, 1024L * 1024 * 1024),
            )
            .putLong("mediaBackfillCutoffMs", settings.mediaBackfillCutoffMs)
            .putString("protectionPinHash", settings.protectionPinHash)
            .putString("protectionPinSalt", settings.protectionPinSalt)
            .putBoolean("protectionUseBiometric", settings.protectionUseBiometric)
            .putBoolean("lockOnAppStart", settings.lockOnAppStart)
            .putInt("protectionGracePeriodMinutes", settings.protectionGracePeriodMinutes)
            .apply()
        transportPrefs.edit()
            .putString(KEY_TRANSPORT_MODE, settings.syncTransportMode.name)
            .remove(LEGACY_PEAR_ENABLED)
            .apply()
    }

    private fun defaultDeviceName(): String =
        runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()
            ?.trim()
            ?.take(80)
            ?.takeIf { it.isNotEmpty() }
            ?: Build.MODEL?.trim()?.take(80)?.takeIf { it.isNotEmpty() }
            ?: "Android phone"

    private companion object {
        const val KEY_TRANSPORT_MODE = "syncTransportMode"
        const val LEGACY_PEAR_ENABLED = "pearSyncEnabled"
    }
}
