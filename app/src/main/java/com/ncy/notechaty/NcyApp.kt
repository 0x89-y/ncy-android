package com.ncy.notechaty

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ncy.notechaty.backup.BackupScheduler
import com.ncy.notechaty.data.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NcyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        val settings = settingsRepository.settings.value
        if (settings.autoBackupEnabled) {
            BackupScheduler.schedule(this, settings.autoBackupIntervalHours)
        }
    }
}
