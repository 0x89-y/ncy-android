package com.ncy.notechaty.data.repository;

import android.content.Context;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.serialization.ChatSerializer;
import com.google.gson.Gson;
import com.ncy.notechaty.data.sync.ChatEventBus;
import com.ncy.notechaty.data.sync.DeviceIdentity;
import com.ncy.notechaty.data.sync.media.MediaHash;
import com.ncy.notechaty.data.sync.protection.ProtectionCrypto;
import com.ncy.notechaty.data.sync.protection.ProtectionKeyStore;
import com.ncy.notechaty.data.sync.event.ChatEvent;
import com.ncy.notechaty.data.sync.event.ChatEventCodec;
import com.ncy.notechaty.data.sync.event.ChatEventOp;
import com.ncy.notechaty.data.sync.event.EventLog;
import com.ncy.notechaty.data.sync.event.EventLogMigrator;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;
import com.ncy.notechaty.data.sync.event.MaterializedView;
import com.ncy.notechaty.security.ChatLockState;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u009c\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0012\n\u0002\b\u001f\n\u0002\u0018\u0002\n\u0002\b\u0010\b\u0007\u0018\u00002\u00020\u0001B9\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$H\u0096@\u00a2\u0006\u0002\u0010%J\u001a\u0010&\u001a\u0004\u0018\u00010\'2\u0006\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020+H\u0002J\u0016\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020/H\u0096@\u00a2\u0006\u0002\u00100J\u0010\u00101\u001a\u00020-2\u0006\u0010.\u001a\u00020/H\u0002J\u0016\u00102\u001a\u00020\u00122\u0006\u00103\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u001a\u00105\u001a\u00020/2\u0006\u0010.\u001a\u00020/2\b\u00106\u001a\u0004\u0018\u000107H\u0002J\u0016\u00108\u001a\u00020\"2\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u001e\u00109\u001a\u00020\"2\u0006\u0010(\u001a\u00020)2\u0006\u0010:\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u0010;J\u0018\u0010<\u001a\u00020+2\u0006\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020+H\u0002J\u0016\u0010=\u001a\u00020\"2\u0006\u0010>\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010?J\u001e\u0010@\u001a\u00020\"2\u0006\u0010(\u001a\u00020)2\u0006\u0010>\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010AJ\u0016\u0010B\u001a\u00020\"2\u0006\u0010>\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010?J\b\u0010C\u001a\u00020\u0018H\u0016J\u0018\u0010D\u001a\u0004\u0018\u00010\u00122\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u0016\u0010E\u001a\u00020\u00182\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u0016\u0010F\u001a\u00020\u00182\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u001c\u0010G\u001a\b\u0012\u0004\u0012\u00020$0\u00112\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u001c\u0010H\u001a\b\u0012\u0004\u0012\u00020$0\u00112\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u0016\u0010I\u001a\u00020\u00182\u0006\u0010(\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u00104J\u0016\u0010J\u001a\u00020\"2\u0006\u0010K\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010?J \u0010L\u001a\u00020\u00182\u0006\u0010M\u001a\u00020\u00182\u0006\u0010N\u001a\u00020\u00182\u0006\u0010O\u001a\u00020)H\u0002J\u0012\u0010P\u001a\u0004\u0018\u00010)2\u0006\u0010Q\u001a\u00020)H\u0002J \u0010R\u001a\u0004\u0018\u00010\u00182\u0006\u0010S\u001a\u00020)2\u0006\u0010Q\u001a\u00020)H\u0096@\u00a2\u0006\u0002\u0010;J\u0018\u0010T\u001a\n\u0012\u0004\u0012\u00020)\u0018\u00010\u00112\u0006\u0010Q\u001a\u00020)H\u0002J\u0012\u0010U\u001a\u0004\u0018\u00010)2\u0006\u0010Q\u001a\u00020)H\u0002J\u0014\u0010V\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110WH\u0016J\u000e\u0010X\u001a\u00020\"H\u0086@\u00a2\u0006\u0002\u0010YJ.\u0010Z\u001a\u00020\"2\u0006\u0010(\u001a\u00020)2\u0006\u0010S\u001a\u00020)2\u0006\u0010Q\u001a\u00020)2\u0006\u0010[\u001a\u000207H\u0096@\u00a2\u0006\u0002\u0010\\J\u000e\u0010]\u001a\u00020\"H\u0096@\u00a2\u0006\u0002\u0010YJ\u001e\u0010^\u001a\u00020\"2\u0006\u0010(\u001a\u00020)2\u0006\u0010N\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010AJ\u001e\u0010_\u001a\u00020\"2\u0006\u0010(\u001a\u00020)2\u0006\u0010N\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u0010AJ\u0016\u0010`\u001a\u00020\"2\u0006\u0010a\u001a\u00020\u0012H\u0096@\u00a2\u0006\u0002\u0010bJ\u0016\u0010c\u001a\u00020\"2\u0006\u0010#\u001a\u00020$H\u0096@\u00a2\u0006\u0002\u0010%J\"\u0010d\u001a\u00020\u0012*\u00020\'2\u0006\u0010e\u001a\u00020)2\f\u0010f\u001a\b\u0012\u0004\u0012\u00020$0\u0011H\u0002R\u001a\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0017\u001a\u00020\u00188BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0019\u0010\u001aR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001b\u001a\u00020\u00188BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u001c\u0010\u001aR\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020 X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006g"}, d2 = {"Lcom/ncy/notechaty/data/repository/FileChatRepository;", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "context", "Landroid/content/Context;", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "chatLockState", "Lcom/ncy/notechaty/security/ChatLockState;", "deviceIdentity", "Lcom/ncy/notechaty/data/sync/DeviceIdentity;", "eventBus", "Lcom/ncy/notechaty/data/sync/ChatEventBus;", "keyStore", "Lcom/ncy/notechaty/data/sync/protection/ProtectionKeyStore;", "(Landroid/content/Context;Lcom/ncy/notechaty/data/repository/ProfileRepository;Lcom/ncy/notechaty/security/ChatLockState;Lcom/ncy/notechaty/data/sync/DeviceIdentity;Lcom/ncy/notechaty/data/sync/ChatEventBus;Lcom/ncy/notechaty/data/sync/protection/ProtectionKeyStore;)V", "_chats", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/ncy/notechaty/data/model/Chat;", "eventCodec", "Lcom/ncy/notechaty/data/sync/event/ChatEventCodec;", "gson", "Lcom/google/gson/Gson;", "profileDir", "Ljava/io/File;", "getProfileDir", "()Ljava/io/File;", "rootDir", "getRootDir", "scope", "Lkotlinx/coroutines/CoroutineScope;", "serializer", "Lcom/ncy/notechaty/data/serialization/ChatSerializer;", "addMessage", "", "message", "Lcom/ncy/notechaty/data/model/Message;", "(Lcom/ncy/notechaty/data/model/Message;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "appendAndMaterialize", "Lcom/ncy/notechaty/data/model/ChatData;", "chatId", "", "op", "Lcom/ncy/notechaty/data/sync/event/ChatEventOp;", "applyExternalEvent", "", "event", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "(Lcom/ncy/notechaty/data/sync/event/ChatEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "applyExternalEventLocked", "createChat", "name", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "decryptOpIfPossible", "key", "", "deleteChat", "deleteMessage", "messageId", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "encryptIfProtected", "exportAllChats", "outputFile", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportChat", "(Ljava/lang/String;Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportEveryProfile", "getBaseDir", "getChat", "getChatDir", "getImagesDir", "getMessages", "getMessagesDecrypted", "getVoiceDir", "importChat", "zipFile", "ingestChatImage", "chatDir", "sourceFile", "subdir", "mediaExt", "mediaType", "mediaFileForHash", "hash", "mediaSearchSubdirs", "mediaWriteSubdir", "observeChats", "Lkotlinx/coroutines/flow/Flow;", "refreshChats", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveMediaBlob", "bytes", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[BLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scanForNewChats", "setChatAvatar", "setChatBackground", "updateChat", "chat", "(Lcom/ncy/notechaty/data/model/Chat;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateMessage", "toChat", "id", "messages", "app_debug"})
public final class FileChatRepository implements com.ncy.notechaty.data.repository.ChatRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ProfileRepository profileRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.ChatLockState chatLockState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.DeviceIdentity deviceIdentity = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.ChatEventBus eventBus = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.protection.ProtectionKeyStore keyStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.serialization.ChatSerializer serializer = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.event.ChatEventCodec eventCodec = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> _chats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    
    @javax.inject.Inject()
    public FileChatRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository profileRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.ChatLockState chatLockState, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.DeviceIdentity deviceIdentity, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.ChatEventBus eventBus, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.protection.ProtectionKeyStore keyStore) {
        super();
    }
    
    private final java.io.File getRootDir() {
        return null;
    }
    
    private final java.io.File getProfileDir() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.io.File getBaseDir() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.ncy.notechaty.data.model.Chat>> observeChats() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ncy.notechaty.data.model.Chat> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object createChat(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ncy.notechaty.data.model.Chat> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateChat(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Chat chat, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object deleteChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getMessages(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ncy.notechaty.data.model.Message>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object addMessage(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateMessage(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object deleteMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object setChatAvatar(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.io.File sourceFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object setChatBackground(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.io.File sourceFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Copy [sourceFile] into `<chatDir>/<subdir>/<sha256>.jpg`, dedup'd by hash. Used by
     * `setChatAvatar` and `setChatBackground` so both produce content-addressed files
     * that paired desktops can fetch via the existing blob protocol.
     */
    private final java.io.File ingestChatImage(java.io.File chatDir, java.io.File sourceFile, java.lang.String subdir) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object mediaFileForHash(@org.jetbrains.annotations.NotNull()
    java.lang.String hash, @org.jetbrains.annotations.NotNull()
    java.lang.String mediaType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object saveMediaBlob(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String hash, @org.jetbrains.annotations.NotNull()
    java.lang.String mediaType, @org.jetbrains.annotations.NotNull()
    byte[] bytes, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.String mediaExt(java.lang.String mediaType) {
        return null;
    }
    
    /**
     * Where to look when *searching* by hash. Images can live under `images/` (message
     * attachments), `avatar/` (chat avatars), or `background/` (chat wallpapers).
     */
    private final java.util.List<java.lang.String> mediaSearchSubdirs(java.lang.String mediaType) {
        return null;
    }
    
    /**
     * Where to *write* a freshly-downloaded blob. Single canonical dir per media type.
     */
    private final java.lang.String mediaWriteSubdir(java.lang.String mediaType) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getChatDir(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getImagesDir(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getVoiceDir(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.io.File> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object exportChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object exportAllChats(@org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object exportEveryProfile(@org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object importChat(@org.jetbrains.annotations.NotNull()
    java.io.File zipFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object scanForNewChats(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object refreshChats(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Apply an event authored by a remote peer. Idempotent — re-applying the same `eventId`
     * is a no-op. Does *not* call HLC.tick (the event already carries its own HLC); future
     * local writes that read the chat's prior events will naturally absorb the remote
     * physicalMs via [HlcTimestamp.tick]'s `max` rule.
     *
     * Returns true if the event was newly applied (caller should ack); false if duplicate.
     */
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object applyExternalEvent(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.event.ChatEvent event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    private final synchronized boolean applyExternalEventLocked(com.ncy.notechaty.data.sync.event.ChatEvent event) {
        return false;
    }
    
    /**
     * Append a new event to the chat's log and re-materialize chat.json from the full log.
     * The event log is the source of truth; chat.json is a derived view. Synchronized so
     * concurrent mutations don't generate colliding HLCs (same physicalMs+logical+nodeId).
     */
    @kotlin.jvm.Synchronized()
    private final synchronized com.ncy.notechaty.data.model.ChatData appendAndMaterialize(java.lang.String chatId, com.ncy.notechaty.data.sync.event.ChatEventOp op) {
        return null;
    }
    
    private final com.ncy.notechaty.data.sync.event.ChatEventOp encryptIfProtected(java.lang.String chatId, com.ncy.notechaty.data.sync.event.ChatEventOp op) {
        return null;
    }
    
    /**
     * Phase 3 decrypt path: read raw events, replace any `EncryptedMessage` op we have
     * the key for with the decrypted `MessageCreated`/`MessageEdited`, then materialize.
     * Falls back to placeholders when no key is cached. Used by the UI when a chat is
     * unlocked — the on-disk `chat.json` always carries placeholders (so a stolen
     * device can't read protected content), and decryption happens transiently in
     * memory on each call.
     */
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getMessagesDecrypted(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ncy.notechaty.data.model.Message>> $completion) {
        return null;
    }
    
    private final com.ncy.notechaty.data.sync.event.ChatEvent decryptOpIfPossible(com.ncy.notechaty.data.sync.event.ChatEvent event, byte[] key) {
        return null;
    }
    
    private final com.ncy.notechaty.data.model.Chat toChat(com.ncy.notechaty.data.model.ChatData $this$toChat, java.lang.String id, java.util.List<com.ncy.notechaty.data.model.Message> messages) {
        return null;
    }
}