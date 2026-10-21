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
        /** Also read by [MainActivity], which opens this chat when the notification is tapped. */
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messagePreview = intent.getStringExtra(EXTRA_MESSAGE_PREVIEW) ?: context.getString(R.string.reminder_notification_hidden)
        val chatName = intent.getStringExtra(EXTRA_CHAT_NAME) ?: context.getString(R.string.ncy)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        createNotificationChannel(context)

        // Into the message's chat, not just the start screen.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_CHAT_ID, intent.getStringExtra(EXTRA_CHAT_ID))
            putExtra(EXTRA_PROFILE_ID, intent.getStringExtra(EXTRA_PROFILE_ID))
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
            context.getString(R.string.reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
