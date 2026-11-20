package com.ncy.notechaty.dev

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ncy.notechaty.data.model.BlockType
import com.ncy.notechaty.data.model.ChecklistItem
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.model.RichTextBlock
import com.ncy.notechaty.data.model.RichTextSpan
import com.ncy.notechaty.data.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Dev builds only. Fills the app with the sample chats from docs/redesign so screens
 * can be checked against the mockups:
 *
 *   adb shell am broadcast -n com.ncy.notechaty.dev/com.ncy.notechaty.dev.DevSeedReceiver
 */
@AndroidEntryPoint
class DevSeedReceiver : BroadcastReceiver() {
    @Inject lateinit var chats: ChatRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { seed() } catch (e: Exception) { Log.e("DevSeed", "seed failed", e) } finally { pending.finish() }
        }
    }

    private fun text(vararg parts: Pair<String, Boolean>) =
        listOf(RichTextBlock(BlockType.PARAGRAPH, parts.map { RichTextSpan(it.first, bold = it.second) }))

    private fun plain(s: String) = text(s to false)

    private suspend fun seed() {
        val now = System.currentTimeMillis()
        val min = 60_000L
        val hour = 60 * min
        val day = 24 * hour

        suspend fun chat(name: String, hue: String, pinned: Boolean = false, notes: List<(String) -> Message>) {
            val c = chats.createChat(name)
            chats.updateChat(c.copy(isPinned = pinned, bubbleColor = hue))
            notes.forEach { chats.addMessage(it(c.id)) }
        }
        fun msg(at: Long, content: List<RichTextBlock>, star: Boolean = false, reminder: Long? = null, reactions: List<String> = emptyList()) =
            { id: String -> Message(id = UUID.randomUUID().toString(), chatId = id, content = content, createdAt = at, isStarred = star, reminderAt = reminder, reactions = reactions) }
        fun list(at: Long, vararg items: Pair<String, Boolean>) =
            { id: String -> Message(id = UUID.randomUUID().toString(), chatId = id, type = MessageType.CHECKLIST, createdAt = at,
                checklistItems = items.map { ChecklistItem(UUID.randomUUID().toString(), it.first, it.second) }) }

        chat("Groceries", "#2F855A", pinned = true, notes = listOf(
            list(now - 3 * hour, "Oat milk" to false, "Eggs" to true, "Lemons" to false, "Bread" to true, "Olive oil" to true, "Coffee" to false, "Rice" to true, "Tomatoes" to false, "Basil" to false),
            msg(now - 2 * hour, plain("Water the plants"), reminder = now - 30 * min),
        ))
        chat("Ideas", "#B4471F", pinned = true, notes = listOf(
            msg(now - 2 * day, text("Onboarding" to true, " should ask for nothing. Open straight into a first chat called Inbox." to false)),
            list(now - 2 * day + 2 * min, "Audit current screens" to true, "Pick a type pairing" to true, "Mockups for home and chat" to false, "Composer states" to false),
            msg(now - day, plain("Maybe pinned chats become cards at the top, with their checklist progress showing.")),
            msg(now - day + 25 * min, text("Yes, and show the " to false, "next reminder" to true, " on the card too." to false), star = true, reactions = listOf("👍", "💡")),
            msg(now - hour, plain("Ask Sam about the sync edge-cases doc"), reminder = now + 6 * hour),
        ))
        chat("Work log", "#2B6CB0", notes = listOf(
            msg(now - 30 * day, plain("Removed internet sync through Pear. Local-network sync only; the app drops to about 6 MB.")),
            msg(now - day - 3 * hour, plain("Shipped 0.8.1: backup folder, bulk delete confirm and faster lists")),
        ))
        chat("Reading list", "#805AD5", notes = listOf(
            msg(now - 4 * day, plain("The Overstory, chapter 12, the banyan bit")),
        ))
        chat("Lisbon trip", "#0F766E", notes = listOf(
            msg(now - 8 * day, plain("Pack the charger and passport"), reminder = now + day + 2 * hour),
            msg(now - 8 * day + hour, plain("Tram 28 early, before the crowds. Pastéis de Belém on the way back.")),
        ))
        chat("Recipes", "#C69214", notes = listOf(
            msg(now - 17 * day, plain("Shakshuka: 2 tins tomatoes, 6 eggs, cumin, paprika, feta to finish.")),
        ))
        Log.i("DevSeed", "seeded")
    }
}
