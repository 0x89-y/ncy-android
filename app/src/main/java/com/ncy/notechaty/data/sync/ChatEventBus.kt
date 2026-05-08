package com.ncy.notechaty.data.sync

import com.ncy.notechaty.data.sync.event.ChatEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton broadcast channel that the repository publishes to on every successful event
 * append, and that [com.ncy.notechaty.data.sync.SyncManager] subscribes to in order to push
 * new events to paired peers in near-real-time.
 *
 * Bounded buffer with DROP_OLDEST so a misbehaving subscriber can't pin memory; sync
 * subscribers re-read missed events from the on-disk log on next connect anyway.
 */
@Singleton
class ChatEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<ChatEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    fun publish(event: ChatEvent) {
        _events.tryEmit(event)
    }
}
