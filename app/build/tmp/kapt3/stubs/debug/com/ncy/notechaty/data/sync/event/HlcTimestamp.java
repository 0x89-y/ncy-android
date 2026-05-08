package com.ncy.notechaty.data.sync.event;

/**
 * Hybrid Logical Clock (Kulkarni et al.). Used to order events deterministically
 * across devices that may be offline. Comparison is (physicalMs, logical, nodeId).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u000f\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\u0010\u0000\n\u0002\b\u0004\b\u0086\b\u0018\u0000 \u001a2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u001aB\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0011\u0010\u000f\u001a\u00020\u00052\u0006\u0010\u0010\u001a\u00020\u0000H\u0096\u0002J\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0007H\u00c6\u0003J\'\u0010\u0014\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0015\u001a\u00020\u00162\b\u0010\u0010\u001a\u0004\u0018\u00010\u0017H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\u0019\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u001b"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "", "physicalMs", "", "logical", "", "nodeId", "", "(JILjava/lang/String;)V", "getLogical", "()I", "getNodeId", "()Ljava/lang/String;", "getPhysicalMs", "()J", "compareTo", "other", "component1", "component2", "component3", "copy", "equals", "", "", "hashCode", "toString", "Companion", "app_debug"})
public final class HlcTimestamp implements java.lang.Comparable<com.ncy.notechaty.data.sync.event.HlcTimestamp> {
    private final long physicalMs = 0L;
    private final int logical = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String nodeId = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.data.sync.event.HlcTimestamp.Companion Companion = null;
    
    public HlcTimestamp(long physicalMs, int logical, @org.jetbrains.annotations.NotNull()
    java.lang.String nodeId) {
        super();
    }
    
    public final long getPhysicalMs() {
        return 0L;
    }
    
    public final int getLogical() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getNodeId() {
        return null;
    }
    
    @java.lang.Override()
    public int compareTo(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.HlcTimestamp other) {
        return 0;
    }
    
    public final long component1() {
        return 0L;
    }
    
    public final int component2() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.sync.event.HlcTimestamp copy(long physicalMs, int logical, @org.jetbrains.annotations.NotNull()
    java.lang.String nodeId) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J(\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nJ \u0010\u000b\u001a\u00020\u00042\b\u0010\f\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n\u00a8\u0006\r"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/HlcTimestamp$Companion;", "", "()V", "receive", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "local", "remote", "nowMs", "", "nodeId", "", "tick", "previous", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        /**
         * Generate the next local timestamp given the previous local clock and current wall-clock.
         */
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.HlcTimestamp tick(@org.jetbrains.annotations.Nullable()
        com.ncy.notechaty.data.sync.event.HlcTimestamp previous, long nowMs, @org.jetbrains.annotations.NotNull()
        java.lang.String nodeId) {
            return null;
        }
        
        /**
         * Merge a remote timestamp into the local clock and return the new local timestamp.
         */
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.HlcTimestamp receive(@org.jetbrains.annotations.Nullable()
        com.ncy.notechaty.data.sync.event.HlcTimestamp local, @org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.sync.event.HlcTimestamp remote, long nowMs, @org.jetbrains.annotations.NotNull()
        java.lang.String nodeId) {
            return null;
        }
    }
}