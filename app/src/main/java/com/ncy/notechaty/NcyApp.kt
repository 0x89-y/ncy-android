package com.ncy.notechaty

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ncy.notechaty.backup.BackupScheduler
import com.ncy.notechaty.data.repository.ProfileRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.event.EventLogCompactor
import com.ncy.notechaty.data.sync.transport.SyncLimits
import com.ncy.notechaty.data.sync.event.EventLogMigrator
import com.ncy.notechaty.data.sync.event.Receipts
import com.ncy.notechaty.data.sync.event.SyncSnapshot
import com.ncy.notechaty.migration.ProfileMigration
import com.ncy.notechaty.security.PinAttempts
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import android.util.Log
import com.ncy.notechaty.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.ncy.notechaty.notifications.ReminderAlarms

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

    @Inject lateinit var syncSnapshot: SyncSnapshot

    override fun onCreate() {
        // Before super: Hilt builds the repositories there, and they can write events
        // straight away. Installation-local, so outside the backed-up chat folder.
        Receipts.init(java.io.File(filesDir, "ncy_sync"))
        super.onCreate()
        PinAttempts.init(this)
        // Nothing opened in a previous run survives it.
        com.ncy.notechaty.data.sync.protection.UnsealedMedia.init(cacheDir)
        // Migration must run before any ViewModel/repository observer reads chats.
        ProfileMigration.runIfNeeded(this, profileRepository)
        EventLogMigrator.runIfNeeded(this, deviceIdentity.deviceId)
        // The on-disk chats can outlive a damaged/legacy profile registry. Restore
        // those entries before repositories decide which profile directory to show.
        profileRepository.recoverOrphanedProfileDirectories()
        // Trim oversized event logs once per launch — silent no-op for small chats.
        EventLogCompactor.runIfNeeded(this)

        // Partial transfers are kept so a broken download resumes, so only age
        // retires the ones nobody comes back to.
        SyncLimits.sweepStaleBlobTemps(chatRepository.getBaseDir())

        val settings = settingsRepository.settings.value
        if (settings.autoBackupEnabled) {
            BackupScheduler.schedule(this, settings.autoBackupIntervalHours)
        }

        // Housekeeping, once per launch and then hourly. Retention windows are measured
        // in days, so an exact schedule buys nothing; the media sweep runs after it
        // because expired messages are what orphan their attachments.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Number every stored event that lacks a receipt: all of them on the first
            // run of a receipt log, otherwise only what a crash or a copied-in file
            // left behind. A late number is caught up on the next connection.
            runCatching { syncSnapshot.numberUnreceiptedEvents() }
                .onSuccess { if (it > 0) Log.i("NcyApp", "numbered $it event(s) for receipt catch-up") }
                .onFailure { Log.w("NcyApp", "receipt numbering failed", it) }
            while (true) {
                runCatching {
                    chatRepository.applyChatRetention()
                    chatRepository.collectUnreferencedMedia(dryRun = false)
                }.onFailure { Log.w("NcyApp", "housekeeping failed", it) }
                delay(60L * 60L * 1000L)
            }
        }

        // Reminder alarms follow the stored reminders: set here, set on a peer,
        // removed, or gone with a deleted message, chat or profile. A short wait
        // lets a burst of synced changes settle into one pass.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            chatRepository.observeChanges()
                .debounce(1_500L)
                .collect {
                    runCatching { ReminderAlarms.reconcile(this@NcyApp) }
                        .onFailure { Log.w("NcyApp", "reminder alarm sync failed", it) }
                }
        }
    }
}
