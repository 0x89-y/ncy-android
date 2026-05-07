package com.ncy.notechaty.data.repository;

import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.model.Message;
import kotlinx.coroutines.flow.Flow;
import java.io.File;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\n\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\f\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u001e\u0010\u000e\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\u0014J\u001e\u0010\u0015\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\n2\u0006\u0010\u0012\u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\u0016J\u0016\u0010\u0017\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\u0014J\b\u0010\u0018\u001a\u00020\u0013H&J\u0018\u0010\u0019\u001a\u0004\u0018\u00010\b2\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\u001a\u001a\u00020\u00132\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\u001b\u001a\u00020\u00132\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u001c\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00050\u001d2\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\u001e\u001a\u00020\u00132\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\u001f\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\u0014J\u0014\u0010!\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u001d0\"H&J\u000e\u0010#\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010$J\u001e\u0010%\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\n2\u0006\u0010&\u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\u0016J\u001e\u0010\'\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\n2\u0006\u0010&\u001a\u00020\u0013H\u00a6@\u00a2\u0006\u0002\u0010\u0016J\u0016\u0010(\u001a\u00020\u00032\u0006\u0010)\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010*J\u0016\u0010+\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006,"}, d2 = {"Lcom/ncy/notechaty/data/repository/ChatRepository;", "", "addMessage", "", "message", "Lcom/ncy/notechaty/data/model/Message;", "(Lcom/ncy/notechaty/data/model/Message;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createChat", "Lcom/ncy/notechaty/data/model/Chat;", "name", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteChat", "chatId", "deleteMessage", "messageId", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportAllChats", "outputFile", "Ljava/io/File;", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportChat", "(Ljava/lang/String;Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exportEveryProfile", "getBaseDir", "getChat", "getChatDir", "getImagesDir", "getMessages", "", "getVoiceDir", "importChat", "zipFile", "observeChats", "Lkotlinx/coroutines/flow/Flow;", "scanForNewChats", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setChatAvatar", "sourceFile", "setChatBackground", "updateChat", "chat", "(Lcom/ncy/notechaty/data/model/Chat;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateMessage", "app_debug"})
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
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.io.File getBaseDir();
}