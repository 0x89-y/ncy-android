package com.ncy.notechaty.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // A reboot clears every alarm. Reading the chats is quick enough for a
        // receiver, but keep it off the main thread all the same.
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
