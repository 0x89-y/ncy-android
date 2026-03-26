package com.ncy.notechaty.ui.chatconfig;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.repository.ChatRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\f\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u000fJ\u0014\u0010\u0013\u001a\u00020\u00112\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00110\u0015J\u000e\u0010\u0016\u001a\u00020\u00112\u0006\u0010\u0017\u001a\u00020\u0018J\u0006\u0010\u0019\u001a\u00020\u0018J\b\u0010\u001a\u001a\u00020\u0011H\u0002J\u000e\u0010\u001b\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u000fJ\u0016\u0010\u001c\u001a\u00020\u00112\u0006\u0010\u001d\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010\u001eJ\u0016\u0010\u001f\u001a\u00020\u00112\u0006\u0010\u001d\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010\u001eJ\u0010\u0010 \u001a\u00020\u00112\b\u0010!\u001a\u0004\u0018\u00010\u000fJ\u000e\u0010\"\u001a\u00020\u00112\u0006\u0010#\u001a\u00020\u000fR\u0016\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006$"}, d2 = {"Lcom/ncy/notechaty/ui/chatconfig/ChatConfigViewModel;", "Landroidx/lifecycle/ViewModel;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "(Landroidx/lifecycle/SavedStateHandle;Lcom/ncy/notechaty/data/repository/ChatRepository;)V", "_chat", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ncy/notechaty/data/model/Chat;", "chat", "Lkotlinx/coroutines/flow/StateFlow;", "getChat", "()Lkotlinx/coroutines/flow/StateFlow;", "chatId", "", "addTag", "", "tag", "deleteChat", "onDeleted", "Lkotlin/Function0;", "exportChat", "outputFile", "Ljava/io/File;", "getChatDir", "loadChat", "removeTag", "setAvatar", "file", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setBackground", "setBubbleColor", "color", "updateName", "name", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatConfigViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ncy.notechaty.data.model.Chat> _chat = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.Chat> chat = null;
    
    @javax.inject.Inject()
    public ChatConfigViewModel(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedStateHandle, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.Chat> getChat() {
        return null;
    }
    
    private final void loadChat() {
    }
    
    public final void updateName(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object setAvatar(@org.jetbrains.annotations.NotNull()
    java.io.File file, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object setBackground(@org.jetbrains.annotations.NotNull()
    java.io.File file, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void addTag(@org.jetbrains.annotations.NotNull()
    java.lang.String tag) {
    }
    
    public final void removeTag(@org.jetbrains.annotations.NotNull()
    java.lang.String tag) {
    }
    
    public final void setBubbleColor(@org.jetbrains.annotations.Nullable()
    java.lang.String color) {
    }
    
    public final void deleteChat(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDeleted) {
    }
    
    public final void exportChat(@org.jetbrains.annotations.NotNull()
    java.io.File outputFile) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getChatDir() {
        return null;
    }
}