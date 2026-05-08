package com.ncy.notechaty.data.sync;

import com.ncy.notechaty.data.sync.event.ChatEvent;
import kotlinx.coroutines.channels.BufferOverflow;
import kotlinx.coroutines.flow.SharedFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Singleton broadcast channel that the repository publishes to on every successful event
 * append, and that [com.ncy.notechaty.data.sync.SyncManager] subscribes to in order to push
 * new events to paired peers in near-real-time.
 *
 * Bounded buffer with DROP_OLDEST so a misbehaving subscriber can't pin memory; sync
 * subscribers re-read missed events from the on-disk log on next connect anyway.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0005R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\r"}, d2 = {"Lcom/ncy/notechaty/data/sync/ChatEventBus;", "", "()V", "_events", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "events", "Lkotlinx/coroutines/flow/SharedFlow;", "getEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "publish", "", "event", "app_debug"})
public final class ChatEventBus {
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.ncy.notechaty.data.sync.event.ChatEvent> _events = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.ncy.notechaty.data.sync.event.ChatEvent> events = null;
    
    @javax.inject.Inject()
    public ChatEventBus() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.ncy.notechaty.data.sync.event.ChatEvent> getEvents() {
        return null;
    }
    
    public final void publish(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEvent event) {
    }
}