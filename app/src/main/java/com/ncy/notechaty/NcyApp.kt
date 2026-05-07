package com.ncy.notechaty

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ncy.notechaty.backup.BackupScheduler
import com.ncy.notechaty.data.repository.ProfileRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.migration.ProfileMigration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NcyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var profileRepository: ProfileRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Migration must run before any ViewModel/repository observer reads chats.
        // It's a directory rename + a SharedPreferences commit — synchronous is fine.
        ProfileMigration.runIfNeeded(this, profileRepository)

        val settings = settingsRepository.settings.value
        if (settings.autoBackupEnabled) {
            BackupScheduler.schedule(this, settings.autoBackupIntervalHours)
        }
    }
}
