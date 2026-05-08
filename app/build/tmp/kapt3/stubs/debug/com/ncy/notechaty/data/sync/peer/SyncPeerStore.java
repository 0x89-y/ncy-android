package com.ncy.notechaty.data.sync.peer;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Persists the list of paired remote devices in SharedPreferences. Phase 1 stores secrets
 * in plain prefs — they're scoped to LAN sync and only useful to someone already on the
 * device. EncryptedSharedPreferences is a phase-3 hardening once cloud relay arrives.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\t\n\u0002\b\u0002\b\u0007\u0018\u0000 #2\u00020\u0001:\u0001#B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\bJ\u0010\u0010\u0015\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0016\u001a\u00020\u0017J\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u00192\u0006\u0010\u0014\u001a\u00020\bJ\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u00192\u0006\u0010\u0014\u001a\u00020\bJ\b\u0010\u001b\u001a\u00020\u0013H\u0002J\b\u0010\u001c\u001a\u00020\u0013H\u0002J\u000e\u0010\u001d\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u0017J\u0016\u0010\u001e\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u001f\u001a\u00020\u0019J\u001e\u0010 \u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u001f\u001a\u00020\u00192\u0006\u0010!\u001a\u00020\"R\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0016\u0010\u000f\u001a\n \u0011*\u0004\u0018\u00010\u00100\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006$"}, d2 = {"Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_peers", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "gson", "Lcom/google/gson/Gson;", "peers", "Lkotlinx/coroutines/flow/StateFlow;", "getPeers", "()Lkotlinx/coroutines/flow/StateFlow;", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "addOrReplace", "", "peer", "byId", "peerId", "", "lastReceivedHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "lastSentHlc", "load", "persist", "remove", "updateLastReceived", "hlc", "updateLastSent", "syncedAt", "", "Companion", "app_debug"})
public final class SyncPeerStore {
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ncy.notechaty.data.sync.peer.SyncPeer>> _peers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.peer.SyncPeer>> peers = null;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PREFS = "ncy_sync_peers";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_PEERS = "peers";
    @org.jetbrains.annotations.NotNull()
    private static final com.ncy.notechaty.data.sync.peer.SyncPeerStore.Companion Companion = null;
    
    @javax.inject.Inject()
    public SyncPeerStore(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.peer.SyncPeer>> getPeers() {
        return null;
    }
    
    private final void load() {
    }
    
    private final void persist() {
    }
    
    public final void addOrReplace(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeer peer) {
    }
    
    public final void remove(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.sync.peer.SyncPeer byId(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
        return null;
    }
    
    /**
     * Update the high-water-mark HLC after a successful ack from the peer. Refuses to
     * regress — out-of-order acks for older HLCs are ignored so the watermark only ever
     * advances.
     */
    public final void updateLastSent(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.HlcTimestamp hlc, long syncedAt) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.sync.event.HlcTimestamp lastSentHlc(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeer peer) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.sync.event.HlcTimestamp lastReceivedHlc(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeer peer) {
        return null;
    }
    
    /**
     * Update the high-water mark of events we've successfully applied from this peer.
     * Refuses to regress; sets the watermark for the next reconnect's `Hello.sinceHlc`.
     */
    public final void updateLastReceived(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.HlcTimestamp hlc) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore$Companion;", "", "()V", "KEY_PEERS", "", "PREFS", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}