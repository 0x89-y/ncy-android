package com.ncy.notechaty.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ncy.notechaty.data.model.Message

object ReminderScheduler {

    fun schedule(
        context: Context,
        message: Message,
        chatName: String,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MESSAGE_PREVIEW, message.plainTextPreview())
            putExtra(ReminderReceiver.EXTRA_CHAT_NAME, chatName)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, message.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fallback to inexact alarm if exact alarms are not permitted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, messageId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            messageId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
