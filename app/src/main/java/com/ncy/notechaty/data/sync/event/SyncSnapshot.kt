package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.repository.ProfileRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads events from every chat folder under the active profile (or all profiles, if needed)
 * to produce the catch-up stream a peer needs on (re)connect. Events come back sorted by HLC
 * across chats so the receiver can apply them in a single pass.
 *
 * Phase 1: cross-profile sync isn't supported (each device has its own profile state), so
 * this scopes to the active profile only.
 */
@Singleton
class SyncSnapshot @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    private val codec = ChatEventCodec()

    fun eventsSince(since: HlcTimestamp?): List<ChatEvent> {
        val activeId = profileRepository.activeProfileId.value
        val rootDir = profileRepository.rootDir()
        val profileDir = if (activeId.isEmpty()) rootDir else File(rootDir, "profile_$activeId")
        if (!profileDir.exists()) return emptyList()

        val all = ArrayList<ChatEvent>()
        profileDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.forEach { chatDir ->
                val log = EventLog(File(chatDir, "events.ndjson"), codec)
                if (log.exists()) all += log.readSince(since)
            }
        all.sortWith(compareBy { it.hlc })
        return all
    }
}
