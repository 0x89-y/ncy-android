package com.ncy.notechaty.data.sync.event;

import com.ncy.notechaty.data.repository.ProfileRepository;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Reads events from every chat folder under the active profile (or all profiles, if needed)
 * to produce the catch-up stream a peer needs on (re)connect. Events come back sorted by HLC
 * across chats so the receiver can apply them in a single pass.
 *
 * Phase 1: cross-profile sync isn't supported (each device has its own profile state), so
 * this scopes to the active profile only.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\b\u0010\n\u001a\u0004\u0018\u00010\u000bR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/SyncSnapshot;", "", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "(Lcom/ncy/notechaty/data/repository/ProfileRepository;)V", "codec", "Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;", "eventsSince", "", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "since", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "app_debug"})
public final class SyncSnapshot {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ProfileRepository profileRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.event.ChatEventCodec codec = null;
    
    @javax.inject.Inject()
    public SyncSnapshot(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository profileRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.sync.event.ChatEvent> eventsSince(@org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.sync.event.HlcTimestamp since) {
        return null;
    }
}