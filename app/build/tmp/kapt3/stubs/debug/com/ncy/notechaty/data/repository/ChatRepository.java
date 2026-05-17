package com.ncy.notechaty.data.repository;

import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.model.Message;
import com.ncy.notechaty.data.sync.event.ChatEvent;
import kotlinx.coroutines.flow.Flow;
import java.io.File;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010 \n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0002\b\u000b\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u001e\u0010\u0013\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0017\u001a\u00020\u0018H\u00a6@\u00a2\u0006\u0002\u0010\u0019J\u001e\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0017\u001a\u00020\u0018H\u00a6@\u00a2\u0006\u0002\u0010\u001bJ\u0016\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u0017\u001a\u00020\u0018H\u00a6@\u00a2\u0006\u0002\u0010\u0019J\b\u0010\u001d\u001a\u00020\u0018H&J\u0018\u0010\u001e\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010\u001f\u001a\u00020\u00182\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010 \u001a\u00020\u00182\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u001c\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00050\"2\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u001c\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00050\"2\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010$\u001a\u00020\u00182\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010%\u001a\u00020\u00032\u0006\u0010&\u001a\u00020\u0018H\u00a6@\u00a2\u0006\u0002\u0010\u0019J \u0010\'\u001a\u0004\u0018\u00010\u00182\u0006\u0010(\u001a\u00020\u000f2\u0006\u0010)\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010*\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000fH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0014\u0010+\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\"0,H&J.\u0010-\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000f2\u0006\u0010(\u001a\u00020\u000f2\u0006\u0010)\u001a\u00020\u000f2\u0006\u0010.\u001a\u00020/H\u00a6@\u00a2\u0006\u0002\u00100J\u000e\u00101\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u00102J\u001e\u00103\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000f2\u0006\u00104\u001a\u00020\u0018H\u00a6@\u00a2\u0006\u0002\u0010\u001bJ\u001e\u00105\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000f2\u0006\u00104\u001a\u00020\u0018H\u00a6@\u00a2\u0006\u0002\u0010\u001bJ\u0016\u00106\u001a\u00020\u00032\u0006\u00107\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u00108J\u0016\u00109\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006:"}, d2 = {"Lcom/ncy/notechaty/data/repository/ChatRepository;", "", "addMessage", "", "message", "Lcom/ncy/notechaty/data/model/Message;", "(Lcom/ncy/notechaty/data/model/Message;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "applyExternalEvent", "", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "(Lcom/ncy/notechaty/data/sync/event/ChatEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createChat", "Lcom/ncy/notechaty/data/model/Chat;", "name", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteChat", "chatId", "deleteMessage", "messageId", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportAllChats", "outputFile", "Ljava/io/File;", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportChat", "(Ljava/lang/String;Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportEveryProfile", "getBaseDir", "getChat", "getChatDir", "getImagesDir", "getMessages", "", "getMessagesDecrypted", "getVoiceDir", "importChat", "zipFile", "mediaFileForHash", "hash", "mediaType", "migrateProtectedChat", "observeChats", "Lkotlinx/coroutines/flow/Flow;", "saveMediaBlob", "bytes", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[BLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scanForNewChats", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setChatAvatar", "sourceFile", "setChatBackground", "updateChat", "chat", "(Lcom/ncy/notechaty/data/model/Chat;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateMessage", "app_debug"})
public abstract interface ChatRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.ncy.notechaty.data.model.Chat>> observeChats();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ncy.notechaty.data.model.Chat> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object createChat(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ncy.notechaty.data.model.Chat> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateChat(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Chat chat, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ncy.notechaty.data.model.Message>> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object addMessage(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateMessage(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setChatAvatar(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.io.File sourceFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setChatBackground(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.io.File sourceFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getChatDir(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getImagesDir(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getVoiceDir(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object exportChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object exportAllChats(@org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object exportEveryProfile(@org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object importChat(@org.jetbrains.annotations.NotNull()
    java.io.File zipFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object scanForNewChats(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Apply an event authored by a remote peer (sync inbound). Idempotent: events whose
     * `eventId` already exists in the local log are dropped. Returns true if the event was
     * newly applied (and thus needs an ack), false if it was a duplicate.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object applyExternalEvent(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEvent event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    /**
     * Phase 3: like [getMessages] but decrypts any `EncryptedMessage` ops on the fly
     * using the in-memory key cached by `ProtectionKeyStore`. Falls back to placeholders
     * for events the caller doesn't have the key for. Used by the UI when a protected
     * chat is currently unlocked.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMessagesDecrypted(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ncy.notechaty.data.model.Message>> $completion);
    
    /**
     * Phase 3 migration: rewrite any plaintext `MessageCreated`/`MessageEdited` events
     * in a protected chat as encrypted ones, in place (same eventId/HLC). Requires the
     * chat's key to already be cached. Idempotent — already-encrypted events are left
     * untouched. No-op if no key is cached or there's nothing plaintext to convert.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object migrateProtectedChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Locate a content-addressed media blob by its SHA-256 hash. `mediaType` is `"voice"`
     * or `"image"`. Returns null if no matching file exists in any chat under the active
     * profile.
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object mediaFileForHash(@org.jetbrains.annotations.NotNull()
    java.lang.String hash, @org.jetbrains.annotations.NotNull()
    java.lang.String mediaType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion);
    
    /**
     * Persist a freshly-downloaded media blob into the given chat. The file is named
     * `<hash>.<ext>` under the appropriate subdirectory (`voice/` or `images/`).
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object saveMediaBlob(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String hash, @org.jetbrains.annotations.NotNull()
    java.lang.String mediaType, @org.jetbrains.annotations.NotNull()
    byte[] bytes, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getBaseDir();
}