package com.ncy.notechaty.data.sync.transport;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ncy.notechaty.data.sync.event.ChatEventCodec;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;

/**
 * Encodes and decodes [SyncWireMessage]s as JSON text frames. Symmetrical with the Python
 * reference server (see scripts/sync_test_server.py). Schema is stable from phase 1.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\u0018\u0000 \r2\u00020\u0001:\u0001\rB\u0019\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nJ\u000e\u0010\u000b\u001a\u00020\n2\u0006\u0010\f\u001a\u00020\bR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireCodec;", "", "gson", "Lcom/google/gson/Gson;", "eventCodec", "Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;", "(Lcom/google/gson/Gson;Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;)V", "decode", "Lcom/ncy/notechaty/data/sync/transport/SyncWireMessage;", "text", "", "encode", "message", "Companion", "app_debug"})
public final class SyncWireCodec {
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.event.ChatEventCodec eventCodec = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String T_CHALLENGE = "Challenge";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String T_HELLO = "Hello";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String T_WELCOME = "Welcome";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String T_EVENT = "Event";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String T_ACK = "Ack";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String T_ERROR = "Error";
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.data.sync.transport.SyncWireCodec.Companion Companion = null;
    
    public SyncWireCodec(@org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEventCodec eventCodec) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encode(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.transport.SyncWireMessage message) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.sync.transport.SyncWireMessage decode(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    public SyncWireCodec() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/ncy/notechaty/data/sync/transport/SyncWireCodec$Companion;", "", "()V", "T_ACK", "", "T_CHALLENGE", "T_ERROR", "T_EVENT", "T_HELLO", "T_WELCOME", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}