package com.ncy.notechaty.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
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
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Backups from before a folder could be chosen sat in app storage, where
        // nothing read them; they only took up space.
        File(applicationContext.filesDir, "NoteChaty/backups").deleteRecursively()

        val tree = settingsRepository.settings.value.autoBackupFolder?.let(Uri::parse)
        if (tree == null || !BackupFolder.isUsable(applicationContext, tree)) {
            // Retrying cannot help until a folder is chosen again in Settings.
            Log.w(TAG, "no usable backup folder; skipping")
            return Result.failure()
        }
        // Its own file: a scheduled run and "back up now" can overlap.
        val temp = File.createTempFile("auto_backup_", ".zip", applicationContext.cacheDir)
        return try {
            repository.exportEveryProfile(temp)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            BackupFolder.store(applicationContext, tree, BackupFolder.fileName(stamp), temp)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "backup failed", e)
            Result.retry()
        } finally {
            temp.delete()
        }
    }

    companion object {
        const val WORK_NAME = "ncy_auto_backup"
        const val NOW_WORK_NAME = "ncy_backup_now"
        private const val TAG = "AutoBackup"
    }
}
