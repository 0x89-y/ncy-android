package com.ncy.notechaty.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {

    fun schedule(context: Context, intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        )
            // Not at once: choosing a folder already runs one through runNow, and two
            // backups in the same second would only duplicate each other.
            .setInitialDelay(intervalHours.toLong(), TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }

    /** One backup right away, e.g. to check a newly chosen folder works. */
    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            AutoBackupWorker.NOW_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AutoBackupWorker>().build(),
        )
    }

    /** How the latest "back up now" went, or null before the first one. */
    fun runNowState(context: Context): Flow<WorkInfo.State?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(AutoBackupWorker.NOW_WORK_NAME)
            .map { infos -> infos.lastOrNull()?.state }
}
