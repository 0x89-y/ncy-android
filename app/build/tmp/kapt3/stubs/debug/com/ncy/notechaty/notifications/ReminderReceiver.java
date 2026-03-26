package com.ncy.notechaty.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.ncy.notechaty.MainActivity;
import com.ncy.notechaty.R;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 \n2\u00020\u0001:\u0001\nB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J\u0018\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\tH\u0016\u00a8\u0006\u000b"}, d2 = {"Lcom/ncy/notechaty/notifications/ReminderReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "createNotificationChannel", "", "context", "Landroid/content/Context;", "onReceive", "intent", "Landroid/content/Intent;", "Companion", "app_debug"})
public final class ReminderReceiver extends android.content.BroadcastReceiver {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_ID = "ncy_reminders";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_MESSAGE_PREVIEW = "message_preview";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_CHAT_NAME = "chat_name";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_NOTIFICATION_ID = "notification_id";
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.notifications.ReminderReceiver.Companion Companion = null;
    
    public ReminderReceiver() {
        super();
    }
    
    @java.lang.Override()
    public void onReceive(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    private final void createNotificationChannel(android.content.Context context) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/ncy/notechaty/notifications/ReminderReceiver$Companion;", "", "()V", "CHANNEL_ID", "", "EXTRA_CHAT_NAME", "EXTRA_MESSAGE_PREVIEW", "EXTRA_NOTIFICATION_ID", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}