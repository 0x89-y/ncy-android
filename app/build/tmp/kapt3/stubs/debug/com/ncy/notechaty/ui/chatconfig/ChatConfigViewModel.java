package com.ncy.notechaty.ui.chatconfig;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.PinManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0011\b\u0007\u0018\u00002\u00020\u0001B\'\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0013J\u0014\u0010\u001a\u001a\u00020\u00182\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00180\u001cJ\u000e\u0010\u001d\u001a\u00020\u00182\u0006\u0010\u001e\u001a\u00020\u001fJ\u0006\u0010 \u001a\u00020\u001fJ\u0006\u0010!\u001a\u00020\"J\b\u0010#\u001a\u00020\u0018H\u0002J\u000e\u0010$\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0013J\u0016\u0010%\u001a\u00020\u00182\u0006\u0010&\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010\'J\u0016\u0010(\u001a\u00020\u00182\u0006\u0010&\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010\'J\u0010\u0010)\u001a\u00020\u00182\b\u0010*\u001a\u0004\u0018\u00010\u0013J\u000e\u0010+\u001a\u00020\u00182\u0006\u0010,\u001a\u00020\"J\u000e\u0010-\u001a\u00020\u00182\u0006\u0010,\u001a\u00020\"J\u000e\u0010.\u001a\u00020\u00182\u0006\u0010,\u001a\u00020\"J\u0014\u0010/\u001a\u00020\u00182\f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00180\u001cJ\u000e\u00101\u001a\u00020\u00182\u0006\u00102\u001a\u00020\u0013R\u0016\u0010\u000b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0011R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00063"}, d2 = {"Lcom/ncy/notechaty/ui/chatconfig/ChatConfigViewModel;", "Landroidx/lifecycle/ViewModel;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "pinManager", "Lcom/ncy/notechaty/security/PinManager;", "(Landroidx/lifecycle/SavedStateHandle;Lcom/ncy/notechaty/data/repository/ChatRepository;Lcom/ncy/notechaty/data/repository/SettingsRepository;Lcom/ncy/notechaty/security/PinManager;)V", "_chat", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ncy/notechaty/data/model/Chat;", "chat", "Lkotlinx/coroutines/flow/StateFlow;", "getChat", "()Lkotlinx/coroutines/flow/StateFlow;", "chatId", "", "settings", "Lcom/ncy/notechaty/data/model/AppSettings;", "getSettings", "addTag", "", "tag", "deleteChat", "onDeleted", "Lkotlin/Function0;", "exportChat", "outputFile", "Ljava/io/File;", "getChatDir", "hasAppPin", "", "loadChat", "removeTag", "setAvatar", "file", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setBackground", "setBubbleColor", "color", "setBubbleGradient", "enabled", "setHideName", "setProtected", "toggleArchive", "onDone", "updateName", "name", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatConfigViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.PinManager pinManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> settings = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ncy.notechaty.data.model.Chat> _chat = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.Chat> chat = null;
    
    @javax.inject.Inject()
    public ChatConfigViewModel(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedStateHandle, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.PinManager pinManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> getSettings() {
        return null;
    }
    
    public final boolean hasAppPin() {
        return false;
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
    
    public final void setBubbleGradient(boolean enabled) {
    }
    
    public final void setProtected(boolean enabled) {
    }
    
    public final void setHideName(boolean enabled) {
    }
    
    public final void setBubbleColor(@org.jetbrains.annotations.Nullable()
    java.lang.String color) {
    }
    
    public final void toggleArchive(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDone) {
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