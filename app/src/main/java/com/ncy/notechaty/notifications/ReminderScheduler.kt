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
        triggerAtMillis: Long,
        isProtected: Boolean = false
    ) {
        val title = if (isProtected) "Protected chat" else chatName
        val preview = if (isProtected) "New reminder" else message.plainTextPreview()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MESSAGE_PREVIEW, preview)
            putExtra(ReminderReceiver.EXTRA_CHAT_NAME, title)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, message.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = alarmManager.canScheduleExactAlarms() && runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }.isSuccess
        if (!exact) {
            // Exact alarms need the user's grant from Android 14 on. Still fire in
            // Doze, a few minutes late at worst; plain `set` could defer for hours.
            alarmManager.setAndAllowWhileIdle(
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
