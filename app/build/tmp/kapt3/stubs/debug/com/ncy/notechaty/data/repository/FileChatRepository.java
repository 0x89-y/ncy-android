package com.ncy.notechaty.data.repository;

import android.content.Context;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.serialization.ChatSerializer;
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
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0015\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B!\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aH\u0096@\u00a2\u0006\u0002\u0010\u001bJ\u0016\u0010\u001c\u001a\u00020\f2\u0006\u0010\u001d\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u0016\u0010 \u001a\u00020\u00182\u0006\u0010!\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u001e\u0010\"\u001a\u00020\u00182\u0006\u0010!\u001a\u00020\u001e2\u0006\u0010#\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010$J\u0016\u0010%\u001a\u00020\u00182\u0006\u0010&\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\'J\u001e\u0010(\u001a\u00020\u00182\u0006\u0010!\u001a\u00020\u001e2\u0006\u0010&\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010)J\u0016\u0010*\u001a\u00020\u00182\u0006\u0010&\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\'J\b\u0010+\u001a\u00020\u000eH\u0016J\u0018\u0010,\u001a\u0004\u0018\u00010\f2\u0006\u0010!\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u0016\u0010-\u001a\u00020\u000e2\u0006\u0010!\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u0016\u0010.\u001a\u00020\u000e2\u0006\u0010!\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u001c\u0010/\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000b2\u0006\u0010!\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u0016\u00100\u001a\u00020\u000e2\u0006\u0010!\u001a\u00020\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fJ\u0016\u00101\u001a\u00020\u00182\u0006\u00102\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010\'J\u0014\u00103\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b04H\u0016J\u000e\u00105\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u00106J\u000e\u00107\u001a\u00020\u0018H\u0096@\u00a2\u0006\u0002\u00106J\u001e\u00108\u001a\u00020\u00182\u0006\u0010!\u001a\u00020\u001e2\u0006\u00109\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010)J\u001e\u0010:\u001a\u00020\u00182\u0006\u0010!\u001a\u00020\u001e2\u0006\u00109\u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010)J\u0016\u0010;\u001a\u00020\u00182\u0006\u0010<\u001a\u00020\fH\u0096@\u00a2\u0006\u0002\u0010=J\u0016\u0010>\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aH\u0096@\u00a2\u0006\u0002\u0010\u001bJ\"\u0010?\u001a\u00020\f*\u00020@2\u0006\u0010A\u001a\u00020\u001e2\f\u0010B\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000bH\u0002R\u001a\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\u00020\u000e8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\u00020\u000e8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0010R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006C"}, d2 = {"Lcom/ncy/notechaty/data/repository/FileChatRepository;", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "context", "Landroid/content/Context;", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "chatLockState", "Lcom/ncy/notechaty/security/ChatLockState;", "(Landroid/content/Context;Lcom/ncy/notechaty/data/repository/ProfileRepository;Lcom/ncy/notechaty/security/ChatLockState;)V", "_chats", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/ncy/notechaty/data/model/Chat;", "profileDir", "Ljava/io/File;", "getProfileDir", "()Ljava/io/File;", "rootDir", "getRootDir", "scope", "Lkotlinx/coroutines/CoroutineScope;", "serializer", "Lcom/ncy/notechaty/data/serialization/ChatSerializer;", "addMessage", "", "message", "Lcom/ncy/notechaty/data/model/Message;", "(Lcom/ncy/notechaty/data/model/Message;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createChat", "name", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteChat", "chatId", "deleteMessage", "messageId", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportAllChats", "outputFile", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportChat", "(Ljava/lang/String;Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportEveryProfile", "getBaseDir", "getChat", "getChatDir", "getImagesDir", "getMessages", "getVoiceDir", "importChat", "zipFile", "observeChats", "Lkotlinx/coroutines/flow/Flow;", "refreshChats", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scanForNewChats", "setChatAvatar", "sourceFile", "setChatBackground", "updateChat", "chat", "(Lcom/ncy/notechaty/data/model/Chat;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateMessage", "toChat", "Lcom/ncy/notechaty/data/model/ChatData;", "id", "messages", "app_debug"})
public final class FileChatRepository implements com.ncy.notechaty.data.repository.ChatRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ProfileRepository profileRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.ChatLockState chatLockState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.serialization.ChatSerializer serializer = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> _chats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    
    @javax.inject.Inject()
    public FileChatRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository profileRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.ChatLockState chatLockState) {
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
    
    private final com.ncy.notechaty.data.model.Chat toChat(com.ncy.notechaty.data.model.ChatData $this$toChat, java.lang.String id, java.util.List<com.ncy.notechaty.data.model.Message> messages) {
        return null;
    }
}