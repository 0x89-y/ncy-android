package com.ncy.notechaty.data.sync;

import com.ncy.notechaty.data.sync.event.ChatEvent;
import kotlinx.coroutines.channels.BufferOverflow;
import kotlinx.coroutines.flow.SharedFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Singleton broadcast channels that the repository publishes to on every successful
 * mutation, and that [com.ncy.notechaty.data.sync.SyncManager] / UI ViewModels subscribe
 * to in order to react in near-real-time.
 *
 * Bounded buffers with DROP_OLDEST so a misbehaving subscriber can't pin memory; sync
 * subscribers re-read missed events from the on-disk log on next connect anyway.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0007J\u000e\u0010\u0011\u001a\u00020\u000f2\u0006\u0010\u0012\u001a\u00020\u0005R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00070\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00050\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000b\u00a8\u0006\u0013"}, d2 = {"Lcom/ncy/notechaty/data/sync/ChatEventBus;", "", "()V", "_events", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "_mediaArrived", "", "events", "Lkotlinx/coroutines/flow/SharedFlow;", "getEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "mediaArrived", "getMediaArrived", "notifyMediaArrived", "", "chatId", "publish", "event", "app_debug"})
public final class ChatEventBus {
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.ncy.notechaty.data.sync.event.ChatEvent> _events = null;
    
    /**
     * Phase 2b: media-blob arrival notifications. Carries the `chatId` whose materialized
     * view didn't change (event references unchanged) but whose on-disk media now exists,
     * so the UI should re-render. Kept *separate* from [events] so SyncManager doesn't
     * try to forward fake events back over the wire.
     */
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<java.lang.String> _mediaArrived = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.ncy.notechaty.data.sync.event.ChatEvent> events = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<java.lang.String> mediaArrived = null;
    
    @javax.inject.Inject()
    public ChatEventBus() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.ncy.notechaty.data.sync.event.ChatEvent> getEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<java.lang.String> getMediaArrived() {
        return null;
    }
    
    public final void publish(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEvent event) {
    }
    
    public final void notifyMediaArrived(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
    }
}