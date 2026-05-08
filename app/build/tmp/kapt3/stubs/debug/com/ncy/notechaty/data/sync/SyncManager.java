package com.ncy.notechaty.data.sync;

import android.util.Log;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.sync.event.ChatEvent;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;
import com.ncy.notechaty.data.sync.event.SyncSnapshot;
import com.ncy.notechaty.data.sync.peer.SyncPeer;
import com.ncy.notechaty.data.sync.peer.SyncPeerStore;
import com.ncy.notechaty.data.sync.transport.SyncClient;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates one-way push sync to all paired peers. While [start]ed, opens a WebSocket
 * to each peer, streams catch-up events since their last-acked HLC, and forwards new events
 * from [ChatEventBus] in near-real-time. Peer high-water marks update only on `Ack`, so
 * a clean disconnect doesn't lose un-acknowledged events.
 *
 * Phase 1 lifetime: app foreground. NcyApp wires start/stop to ProcessLifecycleOwner.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001:\u0002+,B7\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u000e\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020\u0012J\u0006\u0010#\u001a\u00020!J\u0010\u0010$\u001a\u00020!2\u0006\u0010%\u001a\u00020&H\u0002J\u0006\u0010\'\u001a\u00020!J\u000e\u0010(\u001a\u00020!2\u0006\u0010\"\u001a\u00020\u0012J\u0018\u0010)\u001a\u00020!2\u0006\u0010\"\u001a\u00020\u00122\u0006\u0010*\u001a\u00020\u0013H\u0002R \u0010\u000f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00130\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0014\u001a\u0012\u0012\u0004\u0012\u00020\u0012\u0012\b\u0012\u00060\u0016R\u00020\u00000\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010\u001b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00130\u00110\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020\u0012X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006-"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager;", "", "peerStore", "Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;", "deviceIdentity", "Lcom/ncy/notechaty/data/sync/DeviceIdentity;", "eventBus", "Lcom/ncy/notechaty/data/sync/ChatEventBus;", "syncClient", "Lcom/ncy/notechaty/data/sync/transport/SyncClient;", "snapshot", "Lcom/ncy/notechaty/data/sync/event/SyncSnapshot;", "chatRepository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "(Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;Lcom/ncy/notechaty/data/sync/DeviceIdentity;Lcom/ncy/notechaty/data/sync/ChatEventBus;Lcom/ncy/notechaty/data/sync/transport/SyncClient;Lcom/ncy/notechaty/data/sync/event/SyncSnapshot;Lcom/ncy/notechaty/data/repository/ChatRepository;)V", "_states", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "perPeer", "", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerSession;", "running", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "states", "Lkotlinx/coroutines/flow/StateFlow;", "getStates", "()Lkotlinx/coroutines/flow/StateFlow;", "tag", "disconnect", "", "peerId", "start", "startSession", "peer", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "stop", "syncNow", "updateState", "state", "PeerSession", "PeerState", "app_debug"})
public final class SyncManager {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.peer.SyncPeerStore peerStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.DeviceIdentity deviceIdentity = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.ChatEventBus eventBus = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.transport.SyncClient syncClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.event.SyncSnapshot snapshot = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository chatRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "SyncManager";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerSession> perPeer = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerState>> _states = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerState>> states = null;
    private boolean running = false;
    
    @javax.inject.Inject()
    public SyncManager(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeerStore peerStore, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.DeviceIdentity deviceIdentity, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.ChatEventBus eventBus, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.transport.SyncClient syncClient, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.SyncSnapshot snapshot, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository chatRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerState>> getStates() {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void start() {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void stop() {
    }
    
    /**
     * Manually (re)connect to a single peer, regardless of [start]/[stop] state.
     */
    @kotlin.jvm.Synchronized()
    public final synchronized void syncNow(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void disconnect(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
    }
    
    private final void startSession(com.ncy.notechaty.data.sync.peer.SyncPeer peer) {
    }
    
    private final void updateState(java.lang.String peerId, com.ncy.notechaty.data.sync.SyncManager.PeerState state) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0082\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0082@\u00a2\u0006\u0002\u0010\u0015J\b\u0010\u0016\u001a\u00020\u0012H\u0002J\u0006\u0010\u0017\u001a\u00020\u0012J\u0018\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u0014H\u0002J\u0006\u0010\u001b\u001a\u00020\u0012J\u0010\u0010\u001c\u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u0010H\u0002R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001d"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager$PeerSession;", "", "peer", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "(Lcom/ncy/notechaty/data/sync/SyncManager;Lcom/ncy/notechaty/data/sync/peer/SyncPeer;)V", "lastAckedHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "lastReceivedHlc", "liveJob", "Lkotlinx/coroutines/Job;", "peerName", "", "receivedCount", "", "sentCount", "session", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Session;", "handleInbound", "", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "(Lcom/ncy/notechaty/data/sync/event/ChatEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pushCatchup", "run", "sendEvent", "", "s", "shutdown", "startLiveForwarding", "app_debug"})
    final class PeerSession {
        @org.jetbrains.annotations.NotNull()
        private final com.ncy.notechaty.data.sync.peer.SyncPeer peer = null;
        @org.jetbrains.annotations.Nullable()
        private com.ncy.notechaty.data.sync.transport.SyncClient.Session session;
        @org.jetbrains.annotations.Nullable()
        private kotlinx.coroutines.Job liveJob;
        private int sentCount = 0;
        private int receivedCount = 0;
        @org.jetbrains.annotations.NotNull()
        private java.lang.String peerName;
        @kotlin.jvm.Volatile()
        @org.jetbrains.annotations.Nullable()
        private volatile com.ncy.notechaty.data.sync.event.HlcTimestamp lastAckedHlc;
        @kotlin.jvm.Volatile()
        @org.jetbrains.annotations.Nullable()
        private volatile com.ncy.notechaty.data.sync.event.HlcTimestamp lastReceivedHlc;
        
        public PeerSession(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.sync.peer.SyncPeer peer) {
            super();
        }
        
        public final void run() {
        }
        
        private final java.lang.Object handleInbound(com.ncy.notechaty.data.sync.event.ChatEvent event, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
            return null;
        }
        
        private final void pushCatchup() {
        }
        
        private final void startLiveForwarding(com.ncy.notechaty.data.sync.transport.SyncClient.Session s) {
        }
        
        @kotlin.jvm.Synchronized()
        private final synchronized boolean sendEvent(com.ncy.notechaty.data.sync.transport.SyncClient.Session s, com.ncy.notechaty.data.sync.event.ChatEvent event) {
            return false;
        }
        
        public final void shutdown() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0003\u0004\u0005\u0006B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0004\u0007\b\t\n\u00a8\u0006\u000b"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "", "()V", "Connected", "Connecting", "Failed", "Idle", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Connected;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Connecting;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Failed;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Idle;", "app_debug"})
    public static abstract class PeerState {
        
        private PeerState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0005H\u00c6\u0003J\'\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u00d6\u0003J\t\u0010\u0015\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000b\u00a8\u0006\u0017"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Connected;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "peerName", "", "sentCount", "", "receivedCount", "(Ljava/lang/String;II)V", "getPeerName", "()Ljava/lang/String;", "getReceivedCount", "()I", "getSentCount", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "toString", "app_debug"})
        public static final class Connected extends com.ncy.notechaty.data.sync.SyncManager.PeerState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String peerName = null;
            private final int sentCount = 0;
            private final int receivedCount = 0;
            
            public Connected(@org.jetbrains.annotations.NotNull()
            java.lang.String peerName, int sentCount, int receivedCount) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getPeerName() {
                return null;
            }
            
            public final int getSentCount() {
                return 0;
            }
            
            public final int getReceivedCount() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            public final int component2() {
                return 0;
            }
            
            public final int component3() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.SyncManager.PeerState.Connected copy(@org.jetbrains.annotations.NotNull()
            java.lang.String peerName, int sentCount, int receivedCount) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Connecting;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "()V", "app_debug"})
        public static final class Connecting extends com.ncy.notechaty.data.sync.SyncManager.PeerState {
            @org.jetbrains.annotations.NotNull()
            public static final com.ncy.notechaty.data.sync.SyncManager.PeerState.Connecting INSTANCE = null;
            
            private Connecting() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Failed;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "reason", "", "(Ljava/lang/String;)V", "getReason", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Failed extends com.ncy.notechaty.data.sync.SyncManager.PeerState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String reason = null;
            
            public Failed(@org.jetbrains.annotations.NotNull()
            java.lang.String reason) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getReason() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.SyncManager.PeerState.Failed copy(@org.jetbrains.annotations.NotNull()
            java.lang.String reason) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/ncy/notechaty/data/sync/SyncManager$PeerState$Idle;", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "()V", "app_debug"})
        public static final class Idle extends com.ncy.notechaty.data.sync.SyncManager.PeerState {
            @org.jetbrains.annotations.NotNull()
            public static final com.ncy.notechaty.data.sync.SyncManager.PeerState.Idle INSTANCE = null;
            
            private Idle() {
            }
        }
    }
}