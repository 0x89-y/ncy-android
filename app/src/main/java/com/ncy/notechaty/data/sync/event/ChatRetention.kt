package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.Message

/**
 * Per-chat automatic message retention.
 *
 * A chat may declare `retentionDays`, after which its messages are deleted — on every
 * device, because the rule rides in `ChatMetadata` and the deletion is an ordinary
 * tombstone that syncs like a manual one. The point is keeping scratch chats tidy,
 * not saving disk; media eviction is the separate, device-local feature for that.
 *
 * Kept pure so the rule is testable without a repository or a clock, and identical to
 * the desktop's `storage::retention` — a chat swept by one device and not the other
 * would look like data loss.
 */
object ChatRetention {

    /**
     * Which messages the chat's rule says should go.
     *
     * Exempt regardless of age:
     * - **starred** — starring is an explicit "keep this", and age must not override it;
     * - **a reminder still in the future** — the message is scheduled to matter again.
     *
     * A reminder that has already fired offers no protection, so a chat used for
     * short-lived reminders still tidies itself up.
     */
    fun expiredMessageIds(chat: Chat, messages: List<Message>, nowMs: Long): List<String> {
        val days = chat.retentionDays ?: return emptyList()
        if (days <= 0) return emptyList()
        val cutoff = nowMs - days.toLong() * 24L * 60L * 60L * 1000L
        return messages
            .filter { it.createdAt < cutoff && !it.isStarred && (it.reminderAt ?: 0L) <= nowMs }
            .map { it.id }
    }
}
