package com.ncy.notechaty.data.sync.event;

import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ncy.notechaty.data.model.ChatMetadata;
import com.ncy.notechaty.data.model.Message;
import com.ncy.notechaty.data.sync.protection.ProtectionCrypto;

/**
 * Encodes and decodes [ChatEvent] as one JSON object per line (NDJSON). Format is portable
 * — keys are explicit so a non-Kotlin client can stream-parse without sharing types.
 *
 * Wire schema:
 * ```
 * {"v":1,"type":"<OpType>","eventId":"<uuid>","hlc":{"physicalMs":N,"logical":N,"nodeId":"..."},
 * "originDevice":"<id>","chatId":"<id>","payload":{...}}
 * ```
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B\u000f\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bJ\u000e\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u0006J\u0018\u0010\u000b\u001a\n \r*\u0004\u0018\u00010\f0\f2\u0006\u0010\u000e\u001a\u00020\u000fH\u0002J\u0010\u0010\u0010\u001a\u00020\b2\u0006\u0010\u000e\u001a\u00020\u000fH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;", "", "gson", "Lcom/google/gson/Gson;", "(Lcom/google/gson/Gson;)V", "decode", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "line", "", "encode", "event", "payloadOf", "Lcom/google/gson/JsonElement;", "kotlin.jvm.PlatformType", "op", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "typeOf", "Companion", "app_debug"})
public final class ChatEventCodec {
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    public static final int WIRE_VERSION = 1;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TYPE_MESSAGE_CREATED = "MessageCreated";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TYPE_MESSAGE_EDITED = "MessageEdited";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TYPE_MESSAGE_TOMBSTONED = "MessageTombstoned";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TYPE_CHAT_METADATA_CHANGED = "ChatMetadataChanged";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TYPE_CHAT_TOMBSTONED = "ChatTombstoned";
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.data.sync.event.ChatEventCodec.Companion Companion = null;
    
    public ChatEventCodec(@org.jetbrains.annotations.NotNull()
    com.google.gson.Gson gson) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encode(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEvent event) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.sync.event.ChatEvent decode(@org.jetbrains.annotations.NotNull()
    java.lang.String line) {
        return null;
    }
    
    private final java.lang.String typeOf(com.ncy.notechaty.data.sync.event.ChatEventOp op) {
        return null;
    }
    
    private final com.google.gson.JsonElement payloadOf(com.ncy.notechaty.data.sync.event.ChatEventOp op) {
        return null;
    }
    
    public ChatEventCodec() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventCodec$Companion;", "", "()V", "TYPE_CHAT_METADATA_CHANGED", "", "TYPE_CHAT_TOMBSTONED", "TYPE_MESSAGE_CREATED", "TYPE_MESSAGE_EDITED", "TYPE_MESSAGE_TOMBSTONED", "WIRE_VERSION", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}