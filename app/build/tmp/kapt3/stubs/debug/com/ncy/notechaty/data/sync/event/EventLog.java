package com.ncy.notechaty.data.sync.event;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Per-chat append-only event log stored as NDJSON. Writes are fsynced so a crash mid-mutation
 * never leaves the log inconsistent — a partial line at EOF is the worst case and is
 * filtered out on read.
 *
 * The log lives at `<chat dir>/events.ndjson`. Reads always go through here when the
 * materialized view needs to be rebuilt.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0006\u0018\u0000 \u00152\u00020\u0001:\u0001\u0015B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nJ\u0006\u0010\u000b\u001a\u00020\fJ\b\u0010\r\u001a\u0004\u0018\u00010\u000eJ\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\n0\u0010J\u0016\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\n0\u00102\b\u0010\u0012\u001a\u0004\u0018\u00010\u000eJ\u0014\u0010\u0013\u001a\u00020\b2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\n0\u0010R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/EventLog;", "", "file", "Ljava/io/File;", "codec", "Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;", "(Ljava/io/File;Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;)V", "append", "", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "exists", "", "lastHlc", "Lcom/ncy/notechaty/data/sync/event/HlcTimestamp;", "readAll", "", "readSince", "since", "replaceAll", "events", "Companion", "app_debug"})
public final class EventLog {
    @org.jetbrains.annotations.NotNull()
    private final java.io.File file = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.event.ChatEventCodec codec = null;
    @org.jetbrains.annotations.NotNull()
    private static final byte[] NEWLINE = {(byte)10};
    @org.jetbrains.annotations.NotNull()
    private static final com.ncy.notechaty.data.sync.event.EventLog.Companion Companion = null;
    
    public EventLog(@org.jetbrains.annotations.NotNull()
    java.io.File file, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEventCodec codec) {
        super();
    }
    
    public final boolean exists() {
        return false;
    }
    
    /**
     * Append a single event, fsync, return it. Caller is responsible for HLC ordering.
     */
    @kotlin.jvm.Synchronized()
    public final synchronized void append(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEvent event) {
    }
    
    /**
     * Atomically write the full log (used by migration, snapshotting, import).
     */
    @kotlin.jvm.Synchronized()
    public final synchronized void replaceAll(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.sync.event.ChatEvent> events) {
    }
    
    /**
     * Read events with HLC strictly greater than [since]. Pass null for the entire log.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.sync.event.ChatEvent> readSince(@org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.sync.event.HlcTimestamp since) {
        return null;
    }
    
    /**
     * Read every event in append order. Skips malformed lines (e.g. a torn final line).
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.sync.event.ChatEvent> readAll() {
        return null;
    }
    
    /**
     * The largest HLC currently in the log, or null if empty.
     */
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.sync.event.HlcTimestamp lastHlc() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0002\b\u0003\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/EventLog$Companion;", "", "()V", "NEWLINE", "", "getNEWLINE", "()[B", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] getNEWLINE() {
            return null;
        }
    }
}