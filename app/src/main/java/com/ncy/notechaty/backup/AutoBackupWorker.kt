package com.ncy.notechaty.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ncy.notechaty.data.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ChatRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val backupDir = File(applicationContext.filesDir, "NoteChaty/backups")
            backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "ncy_backup_$timestamp.zip")

            repository.exportEveryProfile(backupFile)

            // Keep only the last 5 backups
            backupDir.listFiles()
                ?.filter { it.name.startsWith("ncy_backup_") && it.name.endsWith(".zip") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(5)
                ?.forEach { it.delete() }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "ncy_auto_backup"
    }
}
