package com.ncy.notechaty.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.ncy.notechaty.data.model.ChatData
import java.io.File

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val rootDir = File(context.filesDir, "NoteChaty")
        if (!rootDir.exists()) return

        val gson = Gson()
        val now = System.currentTimeMillis()

        rootDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.forEach { chatDir ->
                val chatFile = File(chatDir, "chat.json")
                if (!chatFile.exists()) return@forEach
                try {
                    val data = gson.fromJson(chatFile.readText(), ChatData::class.java)
                    data.messages
                        .filter { it.reminderAt != null && it.reminderAt > now }
                        .forEach { message ->
                            ReminderScheduler.schedule(
                                context,
                                message,
                                data.chat.name,
                                message.reminderAt!!,
                                isProtected = data.chat.isProtected
                            )
                        }
                } catch (_: Exception) {
                    // Skip malformed chat files
                }
            }
    }
}
