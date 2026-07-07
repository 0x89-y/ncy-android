package com.ncy.notechaty.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ncy.notechaty.MainActivity
import com.ncy.notechaty.R

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "ncy_reminders"
        const val EXTRA_MESSAGE_PREVIEW = "message_preview"
        const val EXTRA_CHAT_NAME = "chat_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messagePreview = intent.getStringExtra(EXTRA_MESSAGE_PREVIEW) ?: "Reminder"
        val chatName = intent.getStringExtra(EXTRA_CHAT_NAME) ?: "Ncy"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(chatName)
            .setContentText(messagePreview)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Note reminders"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
