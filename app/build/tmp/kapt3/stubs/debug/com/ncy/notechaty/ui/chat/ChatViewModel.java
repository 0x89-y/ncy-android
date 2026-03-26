package com.ncy.notechaty.ui.chat;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.notifications.ReminderScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import java.util.UUID;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\"\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B!\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0012\u001a\u00020\u0013J\u000e\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\rJ\u0014\u0010\u0016\u001a\u00020\u00132\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\r0\u0018J\u0006\u0010\u0019\u001a\u00020\u001aJ\u0006\u0010\u001b\u001a\u00020\u0013J\u0014\u0010\u001c\u001a\u00020\u00132\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001f0\u001eJ\u000e\u0010 \u001a\u00020\u00132\u0006\u0010!\u001a\u00020\rJ\u000e\u0010\"\u001a\u00020\u00132\u0006\u0010#\u001a\u00020\u001aJ\u000e\u0010$\u001a\u00020\u00132\u0006\u0010!\u001a\u00020\rJ\u0014\u0010%\u001a\u00020\u00132\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001f0\u001eJ\u0016\u0010&\u001a\u00020\u00132\u0006\u0010\'\u001a\u00020\u001a2\u0006\u0010(\u001a\u00020)J\u0016\u0010*\u001a\u00020\u00132\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020)J\u000e\u0010.\u001a\u00020\u00132\u0006\u0010+\u001a\u00020,J\u000e\u0010/\u001a\u00020\u00132\u0006\u0010+\u001a\u00020,R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011\u00a8\u00060"}, d2 = {"Lcom/ncy/notechaty/ui/chat/ChatViewModel;", "Landroidx/lifecycle/ViewModel;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "appContext", "Landroid/content/Context;", "(Landroidx/lifecycle/SavedStateHandle;Lcom/ncy/notechaty/data/repository/ChatRepository;Landroid/content/Context;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ncy/notechaty/ui/chat/ChatUiState;", "chatId", "", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "cancelEditing", "", "deleteMessage", "messageId", "deleteMessages", "messageIds", "", "getChatDir", "Ljava/io/File;", "loadChat", "saveEdit", "blocks", "", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "saveEditPlainText", "text", "sendImageMessage", "imageFile", "sendPlainTextMessage", "sendTextMessage", "sendVoiceMessage", "voiceFile", "durationMs", "", "setReminder", "message", "Lcom/ncy/notechaty/data/model/Message;", "triggerAtMillis", "startEditing", "toggleStar", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context appContext = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ncy.notechaty.ui.chat.ChatUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.ui.chat.ChatUiState> uiState = null;
    
    @javax.inject.Inject()
    public ChatViewModel(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedStateHandle, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.ui.chat.ChatUiState> getUiState() {
        return null;
    }
    
    public final void loadChat() {
    }
    
    public final void sendTextMessage(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.RichTextBlock> blocks) {
    }
    
    public final void sendPlainTextMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void sendVoiceMessage(@org.jetbrains.annotations.NotNull()
    java.io.File voiceFile, long durationMs) {
    }
    
    public final void sendImageMessage(@org.jetbrains.annotations.NotNull()
    java.io.File imageFile) {
    }
    
    public final void startEditing(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message) {
    }
    
    public final void cancelEditing() {
    }
    
    public final void saveEdit(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.RichTextBlock> blocks) {
    }
    
    public final void saveEditPlainText(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void deleteMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String messageId) {
    }
    
    public final void deleteMessages(@org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> messageIds) {
    }
    
    public final void toggleStar(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message) {
    }
    
    public final void setReminder(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message, long triggerAtMillis) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getChatDir() {
        return null;
    }
}