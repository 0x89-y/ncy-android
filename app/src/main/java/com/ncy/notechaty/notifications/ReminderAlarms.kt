package com.ncy.notechaty.notifications

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.ChatMetadata
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.serialization.ChatFileCache
import com.ncy.notechaty.data.model.Profile
import java.io.File

/**
 * Keeps the phone's reminder alarms in step with the reminders stored in the chats.
 *
 * Alarms used to be set only where a reminder was set in this app, and never removed.
 * So a reminder set on another device never rang here, while a removed reminder, a
 * deleted message or a deleted chat still rang. Instead, every chat of every profile
 * is read and the alarms are made to match: missing ones set, stale ones cancelled.
 * Which alarms are set is remembered here, since Android cannot be asked.
 */
object ReminderAlarms {

    private const val PREFS = "ncy_reminder_alarms"
    private val gson = Gson()

    /** A chat's details and its messages that carry a reminder, past or upcoming. */
    private class ChatReminders(val chat: ChatMetadata, val messages: List<Message>)

    private val remindersByFile = ChatFileCache<ChatReminders>()

    private data class Wanted(
        val chatId: String,
        val profileId: String,
        val chatName: String,
        val hideText: Boolean,
        val message: Message,
    )

    /**
     * Make the alarms match the stored reminders. [afterBoot] forgets what was set
     * before: a reboot clears every alarm, so all of them have to be set again.
     */
    @Synchronized
    fun reconcile(context: Context, afterBoot: Boolean = false) {
        val root = File(context.filesDir, "NoteChaty")
        if (!root.isDirectory) return
        val now = System.currentTimeMillis()
        val protectedProfiles = protectedProfileIds(context)

        val wanted = mutableMapOf<String, Wanted>()
        chatDirs(root).forEach { (profileId, chatDir) ->
            val chatFile = File(chatDir, "chat.json")
            if (!chatFile.isFile) return@forEach
            // Runs after every change; only chats whose file changed are read again.
            val reminders = remindersByFile.get(chatFile) {
                runCatching { gson.fromJson(chatFile.readText(), ChatData::class.java) }.getOrNull()
                    ?.let { data -> ChatReminders(data.chat, data.messages.filter { it.reminderAt != null }) }
            } ?: return@forEach
            reminders.messages
                .filter { (it.reminderAt ?: 0L) > now }
                .forEach { message ->
                    wanted[message.id] = Wanted(
                        chatId = reminders.chat.id,
                        profileId = profileId,
                        chatName = reminders.chat.name,
                        // A notification shows on the lock screen: say nothing about
                        // what is behind a PIN, whether the chat's or the profile's.
                        hideText = reminders.chat.isProtected || profileId in protectedProfiles,
                        message = message,
                    )
                }
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val scheduled: Map<String, Long> = if (afterBoot) emptyMap() else
            prefs.all.mapNotNull { (id, at) -> (at as? Long)?.let { id to it } }.toMap()

        var set = 0
        wanted.forEach { (id, w) ->
            val at = w.message.reminderAt ?: return@forEach
            if (scheduled[id] == at) return@forEach
            ReminderScheduler.schedule(
                context, w.message, w.chatName, at,
                isProtected = w.hideText, chatId = w.chatId, profileId = w.profileId,
            )
            set++
        }
        val stale = scheduled.keys - wanted.keys
        stale.forEach { ReminderScheduler.cancel(context, it) }

        prefs.edit().clear().apply {
            wanted.forEach { (id, w) -> putLong(id, w.message.reminderAt ?: return@forEach) }
        }.apply()
        if (set > 0 || stale.isNotEmpty()) {
            Log.i("ReminderAlarms", "set $set, cancelled ${stale.size}, pending ${wanted.size}")
        }
    }

    /** Each chat folder with its profile id; "" for chats from before profiles. */
    private fun chatDirs(root: File): List<Pair<String, File>> = buildList {
        root.listFiles()?.forEach { entry ->
            if (!entry.isDirectory) return@forEach
            when {
                entry.name.startsWith("profile_") -> entry.listFiles()
                    ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                    ?.forEach { add(entry.name.removePrefix("profile_") to it) }
                entry.name.startsWith("chat_") -> add("" to entry)
            }
        }
    }

    private fun protectedProfileIds(context: Context): Set<String> {
        val json = context.getSharedPreferences("ncy_profiles", Context.MODE_PRIVATE)
            .getString("profiles", null) ?: return emptySet()
        return runCatching { gson.fromJson(json, Array<Profile>::class.java) }
            .getOrNull()
            ?.filter { it.isProtected }
            ?.mapTo(HashSet()) { it.id }
            ?: emptySet()
    }
}
