package com.ncy.notechaty.data.sync.peer;

/**
 * A paired remote device the phone can push events to. Created from a [PairingPayload]
 * scanned/pasted from the desktop and persisted by [SyncPeerStore].
 *
 * - [id]: deviceId of the remote, used for HLC tiebreaking and wire-level identification.
 * - [secret]: pre-shared key (base64) used to HMAC challenge nonces during handshake.
 * - [lastSentHlcMs]/[lastSentHlcLogical]/[lastSentHlcNode]: high-water mark of events that
 *  the peer has acked. Updated only on `ack`. Null for never-synced peers.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b,\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B\u0089\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0003\u0012\u0006\u0010\t\u001a\u00020\n\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\n\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\n\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\n\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0012J\t\u0010\'\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010(\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010)\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001aJ\u0010\u0010*\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0017J\u000b\u0010+\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010,\u001a\u00020\u0003H\u00c6\u0003J\t\u0010-\u001a\u00020\u0003H\u00c6\u0003J\t\u0010.\u001a\u00020\u0007H\u00c6\u0003J\t\u0010/\u001a\u00020\u0003H\u00c6\u0003J\t\u00100\u001a\u00020\nH\u00c6\u0003J\u0010\u00101\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001aJ\u0010\u00102\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001aJ\u0010\u00103\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0017J\u009e\u0001\u00104\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00032\b\b\u0002\u0010\t\u001a\u00020\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\n2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\n2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\n2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001\u00a2\u0006\u0002\u00105J\u0013\u00106\u001a\u0002072\b\u00108\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00109\u001a\u00020\u0007H\u00d6\u0001J\t\u0010:\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0014R\u0015\u0010\u0010\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\n\n\u0002\u0010\u0018\u001a\u0004\b\u0016\u0010\u0017R\u0015\u0010\u000f\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010\u001b\u001a\u0004\b\u0019\u0010\u001aR\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0014R\u0015\u0010\r\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\n\n\u0002\u0010\u0018\u001a\u0004\b\u001d\u0010\u0017R\u0015\u0010\f\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010\u001b\u001a\u0004\b\u001e\u0010\u001aR\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0014R\u0015\u0010\u000b\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010\u001b\u001a\u0004\b \u0010\u001aR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u0014R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u0011\u0010\b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u0014\u00a8\u0006;"}, d2 = {"Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "", "id", "", "name", "host", "port", "", "secret", "pairedAt", "", "lastSyncAt", "lastSentHlcMs", "lastSentHlcLogical", "lastSentHlcNode", "lastReceivedHlcMs", "lastReceivedHlcLogical", "lastReceivedHlcNode", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;JLjava/lang/Long;Ljava/lang/Long;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/Long;Ljava/lang/Integer;Ljava/lang/String;)V", "getHost", "()Ljava/lang/String;", "getId", "getLastReceivedHlcLogical", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "getLastReceivedHlcMs", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getLastReceivedHlcNode", "getLastSentHlcLogical", "getLastSentHlcMs", "getLastSentHlcNode", "getLastSyncAt", "getName", "getPairedAt", "()J", "getPort", "()I", "getSecret", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;JLjava/lang/Long;Ljava/lang/Long;Ljava/lang/Integer;Ljava/lang/String;Ljava/lang/Long;Ljava/lang/Integer;Ljava/lang/String;)Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class SyncPeer {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String host = null;
    private final int port = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String secret = null;
    private final long pairedAt = 0L;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastSyncAt = null;
    
    /**
     * Highest HLC the peer has acked from us. Drives our outbound catchup.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastSentHlcMs = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer lastSentHlcLogical = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String lastSentHlcNode = null;
    
    /**
     * Highest HLC we've received and applied from this peer. Sent as Hello.sinceHlc.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastReceivedHlcMs = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer lastReceivedHlcLogical = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String lastReceivedHlcNode = null;
    
    public SyncPeer(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String host, int port, @org.jetbrains.annotations.NotNull()
    java.lang.String secret, long pairedAt, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSyncAt, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSentHlcMs, @org.jetbrains.annotations.Nullable()
    java.lang.Integer lastSentHlcLogical, @org.jetbrains.annotations.Nullable()
    java.lang.String lastSentHlcNode, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastReceivedHlcMs, @org.jetbrains.annotations.Nullable()
    java.lang.Integer lastReceivedHlcLogical, @org.jetbrains.annotations.Nullable()
    java.lang.String lastReceivedHlcNode) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHost() {
        return null;
    }
    
    public final int getPort() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSecret() {
        return null;
    }
    
    public final long getPairedAt() {
        return 0L;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastSyncAt() {
        return null;
    }
    
    /**
     * Highest HLC the peer has acked from us. Drives our outbound catchup.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastSentHlcMs() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getLastSentHlcLogical() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getLastSentHlcNode() {
        return null;
    }
    
    /**
     * Highest HLC we've received and applied from this peer. Sent as Hello.sinceHlc.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastReceivedHlcMs() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getLastReceivedHlcLogical() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getLastReceivedHlcNode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component12() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component13() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    public final int component4() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component5() {
        return null;
    }
    
    public final long component6() {
        return 0L;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.sync.peer.SyncPeer copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String host, int port, @org.jetbrains.annotations.NotNull()
    java.lang.String secret, long pairedAt, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSyncAt, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSentHlcMs, @org.jetbrains.annotations.Nullable()
    java.lang.Integer lastSentHlcLogical, @org.jetbrains.annotations.Nullable()
    java.lang.String lastSentHlcNode, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastReceivedHlcMs, @org.jetbrains.annotations.Nullable()
    java.lang.Integer lastReceivedHlcLogical, @org.jetbrains.annotations.Nullable()
    java.lang.String lastReceivedHlcNode) {
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