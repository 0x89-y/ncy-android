package com.ncy.notechaty.data.sync.transport;

import android.util.Base64;
import com.ncy.notechaty.data.sync.event.ChatEvent;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;
import com.ncy.notechaty.data.sync.peer.SyncPeer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * One-shot WebSocket session against a paired peer. Performs the challenge/HMAC handshake,
 * then exposes a [Session] the caller uses to push events. State changes flow back through
 * the [onEvent] callback. The caller (SyncManager) is responsible for retry/backoff and for
 * deciding which events to push in what order.
 *
 * Wire path: `ws://<peer host>:<peer port>/sync`. Phase 1 is plaintext-on-LAN; phase 3
 * upgrades to TLS or Noise.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0007\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0003\u0016\u0017\u0018B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J4\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00120\u0010J\u001a\u0010\u0013\u001a\u0004\u0018\u00010\f2\u0006\u0010\u0014\u001a\u00020\f2\u0006\u0010\u0015\u001a\u00020\fH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient;", "", "()V", "codec", "Lcom/ncy/notechaty/data/sync/transport/SyncWireCodec;", "httpClient", "Lokhttp3/OkHttpClient;", "connect", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Session;", "peer", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "localDeviceId", "", "sinceHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "onEvent", "Lkotlin/Function1;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "", "sign", "secretBase64", "nonceBase64", "Companion", "Event", "Session", "app_debug"})
public final class SyncClient {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.transport.SyncWireCodec codec = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient httpClient = null;
    @java.lang.Deprecated()
    public static final int CLOSE_NORMAL = 1000;
    @java.lang.Deprecated()
    public static final int CLOSE_PROTOCOL_ERROR = 1002;
    @org.jetbrains.annotations.NotNull()
    private static final com.ncy.notechaty.data.sync.transport.SyncClient.Companion Companion = null;
    
    @javax.inject.Inject()
    public SyncClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.sync.transport.SyncClient.Session connect(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeer peer, @org.jetbrains.annotations.NotNull()
    java.lang.String localDeviceId, @org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.sync.event.HlcTimestamp sinceHlc, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.ncy.notechaty.data.sync.transport.SyncClient.Event, kotlin.Unit> onEvent) {
        return null;
    }
    
    private final java.lang.String sign(java.lang.String secretBase64, java.lang.String nonceBase64) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Companion;", "", "()V", "CLOSE_NORMAL", "", "CLOSE_PROTOCOL_ERROR", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0006\u0003\u0004\u0005\u0006\u0007\bB\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0006\t\n\u000b\f\r\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "", "()V", "Acked", "Authenticated", "Connecting", "Disconnected", "EventReceived", "Failed", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Acked;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Authenticated;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Connecting;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Disconnected;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$EventReceived;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Failed;", "app_debug"})
    public static abstract class Event {
        
        private Event() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Acked;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "upToHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "(Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;)V", "getUpToHlc", "()Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Acked extends com.ncy.notechaty.data.sync.transport.SyncClient.Event {
            @org.jetbrains.annotations.NotNull()
            private final com.ncy.notechaty.data.sync.event.HlcTimestamp upToHlc = null;
            
            public Acked(@org.jetbrains.annotations.NotNull()
            com.ncy.notechaty.data.sync.event.HlcTimestamp upToHlc) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.event.HlcTimestamp getUpToHlc() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.event.HlcTimestamp component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.transport.SyncClient.Event.Acked copy(@org.jetbrains.annotations.NotNull()
            com.ncy.notechaty.data.sync.event.HlcTimestamp upToHlc) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Authenticated;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "peerName", "", "(Ljava/lang/String;)V", "getPeerName", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Authenticated extends com.ncy.notechaty.data.sync.transport.SyncClient.Event {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String peerName = null;
            
            public Authenticated(@org.jetbrains.annotations.NotNull()
            java.lang.String peerName) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getPeerName() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.transport.SyncClient.Event.Authenticated copy(@org.jetbrains.annotations.NotNull()
            java.lang.String peerName) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Connecting;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "()V", "app_debug"})
        public static final class Connecting extends com.ncy.notechaty.data.sync.transport.SyncClient.Event {
            @org.jetbrains.annotations.NotNull()
            public static final com.ncy.notechaty.data.sync.transport.SyncClient.Event.Connecting INSTANCE = null;
            
            private Connecting() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Disconnected;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "()V", "app_debug"})
        public static final class Disconnected extends com.ncy.notechaty.data.sync.transport.SyncClient.Event {
            @org.jetbrains.annotations.NotNull()
            public static final com.ncy.notechaty.data.sync.transport.SyncClient.Event.Disconnected INSTANCE = null;
            
            private Disconnected() {
            }
        }
        
        /**
         * Server pushed an event back at us (phase 2 bidirectional).
         */
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$EventReceived;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "(Lcom/ncy/notechaty/data/sync/event/ChatEvent;)V", "getEvent", "()Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class EventReceived extends com.ncy.notechaty.data.sync.transport.SyncClient.Event {
            @org.jetbrains.annotations.NotNull()
            private final com.ncy.notechaty.data.sync.event.ChatEvent event = null;
            
            public EventReceived(@org.jetbrains.annotations.NotNull()
            com.ncy.notechaty.data.sync.event.ChatEvent event) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.event.ChatEvent getEvent() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.event.ChatEvent component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.ncy.notechaty.data.sync.transport.SyncClient.Event.EventReceived copy(@org.jetbrains.annotations.NotNull()
            com.ncy.notechaty.data.sync.event.ChatEvent event) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event$Failed;", "Lcom/ncy/notechaty/data/sync/transport/SyncClient$Event;", "reason", "", "(Ljava/lang/String;)V", "getReason", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Failed extends com.ncy.notechaty.data.sync.transport.SyncClient.Event {
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
            public final com.ncy.notechaty.data.sync.transport.SyncClient.Event.Failed copy(@org.jetbrains.annotations.NotNull()
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
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0007H&J\u0010\u0010\b\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\nH&\u00a8\u0006\u000b"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncClient$Session;", "", "ack", "", "upToHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "close", "", "send", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "app_debug"})
    public static abstract interface Session {
        
        public abstract boolean send(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.sync.event.ChatEvent event);
        
        public abstract boolean ack(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.sync.event.HlcTimestamp upToHlc);
        
        public abstract void close();
    }
}