package com.ncy.notechaty.backup;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u0016\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\t\u00a8\u0006\n"}, d2 = {"Lcom/ncy/notechaty/backup/BackupScheduler;", "", "()V", "cancel", "", "context", "Landroid/content/Context;", "schedule", "intervalHours", "", "app_debug"})
public final class BackupScheduler {
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.backup.BackupScheduler INSTANCE = null;
    
    private BackupScheduler() {
        super();
    }
    
    public final void schedule(@org.jetbrains.annotations.NotNull()
    android.content.Context context, int intervalHours) {
    }
    
    public final void cancel(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
}