package com.ncy.notechaty.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // A reboot clears every alarm. Being granted exact alarms leaves the existing
        // ones as they were, inexact; setting them again makes them ring on time.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) return
        // Reading the chats is quick enough for a receiver, but keep it off the main thread.
        val pending = goAsync()
        Thread {
            try {
                ReminderAlarms.reconcile(context.applicationContext, afterBoot = true)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
