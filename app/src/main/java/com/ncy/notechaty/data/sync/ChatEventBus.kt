package com.ncy.notechaty.data.sync

import com.ncy.notechaty.data.sync.event.ChatEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton broadcast channels that the repository publishes to on every successful
 * mutation, and that [com.ncy.notechaty.data.sync.SyncManager] / UI ViewModels subscribe
 * to in order to react in near-real-time.
 *
 * Bounded buffers with DROP_OLDEST so a misbehaving subscriber can't pin memory; sync
 * subscribers re-read missed events from the on-disk log on next connect anyway.
 */
@Singleton
class ChatEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<ChatEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Phase 2b: media-blob arrival notifications. Carries the `chatId` whose materialized
     * view didn't change (event references unchanged) but whose on-disk media now exists,
     * so the UI should re-render. Kept *separate* from [events] so SyncManager doesn't
     * try to forward fake events back over the wire.
     */
    private val _mediaArrived = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()
    val mediaArrived: SharedFlow<String> = _mediaArrived.asSharedFlow()

    fun publish(event: ChatEvent) {
        _events.tryEmit(event)
    }

    fun notifyMediaArrived(chatId: String) {
        _mediaArrived.tryEmit(chatId)
    }
}
