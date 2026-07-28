package com.ncy.notechaty

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ncy.notechaty.backup.BackupScheduler
import com.ncy.notechaty.data.repository.ProfileRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.event.EventLogCompactor
import com.ncy.notechaty.data.sync.event.EventLogMigrator
import com.ncy.notechaty.migration.ProfileMigration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import android.util.Log
import com.ncy.notechaty.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltAndroidApp
class NcyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var deviceIdentity: DeviceIdentity
    @Inject lateinit var chatRepository: ChatRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Migration must run before any ViewModel/repository observer reads chats.
        ProfileMigration.runIfNeeded(this, profileRepository)
        EventLogMigrator.runIfNeeded(this, deviceIdentity.deviceId)
        // The on-disk chats can outlive a damaged/legacy profile registry. Restore
        // those entries before repositories decide which profile directory to show.
        profileRepository.recoverOrphanedProfileDirectories()
        // Trim oversized event logs once per launch — silent no-op for small chats.
        EventLogCompactor.runIfNeeded(this)

        val settings = settingsRepository.settings.value
        if (settings.autoBackupEnabled) {
            BackupScheduler.schedule(this, settings.autoBackupIntervalHours)
        }

        // Housekeeping, once per launch and then hourly. Retention windows are measured
        // in days, so an exact schedule buys nothing; the media sweep runs after it
        // because expired messages are what orphan their attachments.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (true) {
                runCatching {
                    chatRepository.applyChatRetention()
                    chatRepository.collectUnreferencedMedia(dryRun = false)
                }.onFailure { Log.w("NcyApp", "housekeeping failed", it) }
                delay(60L * 60L * 1000L)
            }
        }
    }
}
