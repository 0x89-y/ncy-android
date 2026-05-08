package com.ncy.notechaty.data.sync.transport;

import com.ncy.notechaty.data.sync.event.ChatEvent;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;

/**
 * Messages exchanged on the live WebSocket. One JSON object per WebSocket text frame.
 * Phase 1 carries a one-way push protocol; phase 2 will reuse [Hello]/[Welcome]
 * for bidirectional pulls.
 *
 * Wire form: `{"type":"<Type>", ...}`
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\t\u0003\u0004\u0005\u0006\u0007\b\t\n\u000bB\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\t\f\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u00a8\u0006\u0015"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "", "()V", "Ack", "BlobChunk", "BlobMissing", "BlobRequest", "Challenge", "Error", "Event", "Hello", "Welcome", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Ack;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$BlobChunk;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$BlobMissing;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$BlobRequest;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Challenge;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Error;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Event;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Hello;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Welcome;", "app_debug"})
public abstract class SyncWireMessage {
    
    private SyncWireMessage() {
        super();
    }
    
    /**
     * Server → client. Acknowledges that all events up to and including this HLC have been
     * persisted. Client may advance its `lastSentHlc` watermark for this peer.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Ack;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "upToHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "(Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;)V", "getUpToHlc", "()Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class Ack extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final com.ncy.notechaty.data.sync.event.HlcTimestamp upToHlc = null;
        
        public Ack(@org.jetbrains.annotations.NotNull()
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
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.Ack copy(@org.jetbrains.annotations.NotNull()
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
    
    /**
     * One chunk of a previously-requested blob. Receivers concatenate by `offset`,
     * verify the SHA-256 of the assembled bytes against `hash` on `isLast`, then
     * persist. `data` is base64-encoded so the wire stays uniform JSON text.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0012\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B5\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\u0003\u0012\u0006\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0014\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\nH\u00c6\u0003JE\u0010\u001a\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u00032\b\b\u0002\u0010\t\u001a\u00020\nH\u00c6\u0001J\u0013\u0010\u001b\u001a\u00020\n2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u00d6\u0003J\t\u0010\u001e\u001a\u00020\u0006H\u00d6\u0001J\t\u0010\u001f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\rR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0012\u00a8\u0006 "}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$BlobChunk;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "hash", "", "mediaType", "offset", "", "totalSize", "data", "isLast", "", "(Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;Z)V", "getData", "()Ljava/lang/String;", "getHash", "()Z", "getMediaType", "getOffset", "()I", "getTotalSize", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "other", "", "hashCode", "toString", "app_debug"})
    public static final class BlobChunk extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String hash = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String mediaType = null;
        private final int offset = 0;
        private final int totalSize = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String data = null;
        private final boolean isLast = false;
        
        public BlobChunk(@org.jetbrains.annotations.NotNull()
        java.lang.String hash, @org.jetbrains.annotations.NotNull()
        java.lang.String mediaType, int offset, int totalSize, @org.jetbrains.annotations.NotNull()
        java.lang.String data, boolean isLast) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getHash() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMediaType() {
            return null;
        }
        
        public final int getOffset() {
            return 0;
        }
        
        public final int getTotalSize() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getData() {
            return null;
        }
        
        public final boolean isLast() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        public final int component3() {
            return 0;
        }
        
        public final int component4() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component5() {
            return null;
        }
        
        public final boolean component6() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.BlobChunk copy(@org.jetbrains.annotations.NotNull()
        java.lang.String hash, @org.jetbrains.annotations.NotNull()
        java.lang.String mediaType, int offset, int totalSize, @org.jetbrains.annotations.NotNull()
        java.lang.String data, boolean isLast) {
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
    
    /**
     * Sender does not have the requested blob. Receiver should drop the pending fetch.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$BlobMissing;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "hash", "", "(Ljava/lang/String;)V", "getHash", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class BlobMissing extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String hash = null;
        
        public BlobMissing(@org.jetbrains.annotations.NotNull()
        java.lang.String hash) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getHash() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.BlobMissing copy(@org.jetbrains.annotations.NotNull()
        java.lang.String hash) {
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
    
    /**
     * Either side asks the other for the bytes of a content-addressed blob.
     * `mediaType` is a hint for how the receiver should store/render the bytes
     * (currently `"voice"` or `"image"`).
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0013"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$BlobRequest;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "hash", "", "mediaType", "(Ljava/lang/String;Ljava/lang/String;)V", "getHash", "()Ljava/lang/String;", "getMediaType", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class BlobRequest extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String hash = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String mediaType = null;
        
        public BlobRequest(@org.jetbrains.annotations.NotNull()
        java.lang.String hash, @org.jetbrains.annotations.NotNull()
        java.lang.String mediaType) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getHash() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMediaType() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.BlobRequest copy(@org.jetbrains.annotations.NotNull()
        java.lang.String hash, @org.jetbrains.annotations.NotNull()
        java.lang.String mediaType) {
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
    
    /**
     * Server → client. First frame after connect. Contains the auth nonce to sign.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Challenge;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "nonce", "", "(Ljava/lang/String;)V", "getNonce", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Challenge extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String nonce = null;
        
        public Challenge(@org.jetbrains.annotations.NotNull()
        java.lang.String nonce) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getNonce() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.Challenge copy(@org.jetbrains.annotations.NotNull()
        java.lang.String nonce) {
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
    
    /**
     * Either side. Aborts the session with a human-readable reason.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0013"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Error;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "code", "", "message", "(Ljava/lang/String;Ljava/lang/String;)V", "getCode", "()Ljava/lang/String;", "getMessage", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Error extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String code = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String message = null;
        
        public Error(@org.jetbrains.annotations.NotNull()
        java.lang.String code, @org.jetbrains.annotations.NotNull()
        java.lang.String message) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getCode() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMessage() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.Error copy(@org.jetbrains.annotations.NotNull()
        java.lang.String code, @org.jetbrains.annotations.NotNull()
        java.lang.String message) {
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
    
    /**
     * Client → server. A single chat event.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Event;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "(Lcom/ncy/notechaty/data/sync/event/ChatEvent;)V", "getEvent", "()Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class Event extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final com.ncy.notechaty.data.sync.event.ChatEvent event = null;
        
        public Event(@org.jetbrains.annotations.NotNull()
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
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.Event copy(@org.jetbrains.annotations.NotNull()
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
    
    /**
     * Client → server. Authenticates the client and announces its catch-up watermark.
     * `auth` = base64(HMAC-SHA256(secret, nonce)).
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u000f\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J)\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u00d6\u0003J\t\u0010\u0015\u001a\u00020\u0016H\u00d6\u0001J\t\u0010\u0017\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0018"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Hello;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "deviceId", "", "auth", "sinceHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "(Ljava/lang/String;Ljava/lang/String;Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;)V", "getAuth", "()Ljava/lang/String;", "getDeviceId", "getSinceHlc", "()Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Hello extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String deviceId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String auth = null;
        @org.jetbrains.annotations.Nullable()
        private final com.ncy.notechaty.data.sync.event.HlcTimestamp sinceHlc = null;
        
        public Hello(@org.jetbrains.annotations.NotNull()
        java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
        java.lang.String auth, @org.jetbrains.annotations.Nullable()
        com.ncy.notechaty.data.sync.event.HlcTimestamp sinceHlc) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDeviceId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAuth() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.ncy.notechaty.data.sync.event.HlcTimestamp getSinceHlc() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.ncy.notechaty.data.sync.event.HlcTimestamp component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.Hello copy(@org.jetbrains.annotations.NotNull()
        java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
        java.lang.String auth, @org.jetbrains.annotations.Nullable()
        com.ncy.notechaty.data.sync.event.HlcTimestamp sinceHlc) {
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
    
    /**
     * Server → client. Authentication accepted; client may begin streaming.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0013"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage$Welcome;", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "deviceId", "", "name", "(Ljava/lang/String;Ljava/lang/String;)V", "getDeviceId", "()Ljava/lang/String;", "getName", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Welcome extends com.ncy.notechaty.data.sync.transport.SyncWireMessage {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String deviceId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String name = null;
        
        public Welcome(@org.jetbrains.annotations.NotNull()
        java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
        java.lang.String name) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDeviceId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.transport.SyncWireMessage.Welcome copy(@org.jetbrains.annotations.NotNull()
        java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
        java.lang.String name) {
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