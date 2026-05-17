package com.ncy.notechaty.data.sync.event;

import com.ncy.notechaty.data.model.ChatMetadata;
import com.ncy.notechaty.data.model.Message;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0006\u0003\u0004\u0005\u0006\u0007\bB\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0006\t\n\u000b\f\r\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "", "()V", "ChatMetadataChanged", "ChatTombstoned", "EncryptedMessage", "MessageCreated", "MessageEdited", "MessageTombstoned", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp$ChatMetadataChanged;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp$ChatTombstoned;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp$EncryptedMessage;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp$MessageCreated;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp$MessageEdited;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp$MessageTombstoned;", "app_debug"})
public abstract class ChatEventOp {
    
    private ChatEventOp() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp$ChatMetadataChanged;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "metadata", "Lcom/ncy/notechaty/data/model/ChatMetadata;", "(Lcom/ncy/notechaty/data/model/ChatMetadata;)V", "getMetadata", "()Lcom/ncy/notechaty/data/model/ChatMetadata;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class ChatMetadataChanged extends com.ncy.notechaty.data.sync.event.ChatEventOp {
        @org.jetbrains.annotations.NotNull()
        private final com.ncy.notechaty.data.model.ChatMetadata metadata = null;
        
        public ChatMetadataChanged(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.model.ChatMetadata metadata) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.model.ChatMetadata getMetadata() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.model.ChatMetadata component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.ChatEventOp.ChatMetadataChanged copy(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.model.ChatMetadata metadata) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp$ChatTombstoned;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "chatId", "", "(Ljava/lang/String;)V", "getChatId", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class ChatTombstoned extends com.ncy.notechaty.data.sync.event.ChatEventOp {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String chatId = null;
        
        public ChatTombstoned(@org.jetbrains.annotations.NotNull()
        java.lang.String chatId) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getChatId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.ChatEventOp.ChatTombstoned copy(@org.jetbrains.annotations.NotNull()
        java.lang.String chatId) {
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
     * Phase 3: a `MessageCreated` or `MessageEdited` whose body is AES-GCM-sealed
     * under a key derived from the chat's PIN + [ChatMetadata.protectionSalt].
     * `innerType` records the original op type so the materializer can place a
     * typed-correct placeholder. `nonce` + `ciphertext` are the raw bytes (not
     * base64) for the round-trip with [ChatEventCodec].
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B)\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u0012\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J3\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0096\u0002J\b\u0010\u0018\u001a\u00020\u0019H\u0016J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\nR\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\f\u00a8\u0006\u001b"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp$EncryptedMessage;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "innerType", "", "nonce", "", "ciphertext", "supersedes", "(Ljava/lang/String;[B[BLjava/lang/String;)V", "getCiphertext", "()[B", "getInnerType", "()Ljava/lang/String;", "getNonce", "getSupersedes", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class EncryptedMessage extends com.ncy.notechaty.data.sync.event.ChatEventOp {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String innerType = null;
        @org.jetbrains.annotations.NotNull()
        private final byte[] nonce = null;
        @org.jetbrains.annotations.NotNull()
        private final byte[] ciphertext = null;
        
        /**
         * Protected-chat catch-up: the `eventId` of a *plaintext* message event this
         * sealed event replaces. Set when a chat is protected after it had already
         * synced in plaintext to a peer — the peer drops the superseded plaintext
         * from its at-rest log on receipt. Null for normally-authored encrypted
         * messages (nothing to supersede). Travels in the cleartext envelope so a
         * locked peer can act on it without the key.
         */
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String supersedes = null;
        
        public EncryptedMessage(@org.jetbrains.annotations.NotNull()
        java.lang.String innerType, @org.jetbrains.annotations.NotNull()
        byte[] nonce, @org.jetbrains.annotations.NotNull()
        byte[] ciphertext, @org.jetbrains.annotations.Nullable()
        java.lang.String supersedes) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getInnerType() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] getNonce() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] getCiphertext() {
            return null;
        }
        
        /**
         * Protected-chat catch-up: the `eventId` of a *plaintext* message event this
         * sealed event replaces. Set when a chat is protected after it had already
         * synced in plaintext to a peer — the peer drops the superseded plaintext
         * from its at-rest log on receipt. Null for normally-authored encrypted
         * messages (nothing to supersede). Travels in the cleartext envelope so a
         * locked peer can act on it without the key.
         */
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getSupersedes() {
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
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] component3() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.ChatEventOp.EncryptedMessage copy(@org.jetbrains.annotations.NotNull()
        java.lang.String innerType, @org.jetbrains.annotations.NotNull()
        byte[] nonce, @org.jetbrains.annotations.NotNull()
        byte[] ciphertext, @org.jetbrains.annotations.Nullable()
        java.lang.String supersedes) {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp$MessageCreated;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "message", "Lcom/ncy/notechaty/data/model/Message;", "(Lcom/ncy/notechaty/data/model/Message;)V", "getMessage", "()Lcom/ncy/notechaty/data/model/Message;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class MessageCreated extends com.ncy.notechaty.data.sync.event.ChatEventOp {
        @org.jetbrains.annotations.NotNull()
        private final com.ncy.notechaty.data.model.Message message = null;
        
        public MessageCreated(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.model.Message message) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.model.Message getMessage() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.model.Message component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.ChatEventOp.MessageCreated copy(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.model.Message message) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp$MessageEdited;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "message", "Lcom/ncy/notechaty/data/model/Message;", "(Lcom/ncy/notechaty/data/model/Message;)V", "getMessage", "()Lcom/ncy/notechaty/data/model/Message;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class MessageEdited extends com.ncy.notechaty.data.sync.event.ChatEventOp {
        @org.jetbrains.annotations.NotNull()
        private final com.ncy.notechaty.data.model.Message message = null;
        
        public MessageEdited(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.model.Message message) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.model.Message getMessage() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.model.Message component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.ChatEventOp.MessageEdited copy(@org.jetbrains.annotations.NotNull()
        com.ncy.notechaty.data.model.Message message) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/ChatEventOp$MessageTombstoned;", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "messageId", "", "(Ljava/lang/String;)V", "getMessageId", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class MessageTombstoned extends com.ncy.notechaty.data.sync.event.ChatEventOp {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String messageId = null;
        
        public MessageTombstoned(@org.jetbrains.annotations.NotNull()
        java.lang.String messageId) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMessageId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.data.sync.event.ChatEventOp.MessageTombstoned copy(@org.jetbrains.annotations.NotNull()
        java.lang.String messageId) {
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