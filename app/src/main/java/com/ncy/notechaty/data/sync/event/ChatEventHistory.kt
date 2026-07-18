package com.ncy.notechaty.data.sync.event

import java.io.File

/** Reads an active chat's normal log plus its compact exact message deletions. */
object ChatEventHistory {
    fun read(chatDir: File, codec: ChatEventCodec = ChatEventCodec()): List<ChatEvent> {
        val active = EventLog(File(chatDir, "events.ndjson"), codec).readAll()
        val seen = active.mapTo(HashSet()) { it.eventId }
        val compactDeletes = MessageTombstoneGraveyard.events(chatDir)
            .filter { seen.add(it.eventId) }
        return (active + compactDeletes).sortedBy { it.hlc }
    }
}
