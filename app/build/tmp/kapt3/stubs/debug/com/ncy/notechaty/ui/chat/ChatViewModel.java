package com.ncy.notechaty.ui.chat;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.notifications.ReminderScheduler;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import java.util.UUID;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000v\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\"\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B)\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0001\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0006\u0010\u0017\u001a\u00020\u0018J\u0006\u0010\u0019\u001a\u00020\u0018J\u000e\u0010\u001a\u001a\u00020\u00182\u0006\u0010\u001b\u001a\u00020\u000fJ\u0014\u0010\u001c\u001a\u00020\u00182\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u000f0\u001eJ\u0006\u0010\u001f\u001a\u00020 J\b\u0010!\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010\"\u001a\u00020\u0018J\u0006\u0010#\u001a\u00020\u0018J\u0006\u0010$\u001a\u00020\u0018J\u0014\u0010%\u001a\u00020\u00182\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\u000f0\'J\u0014\u0010(\u001a\u00020\u00182\f\u0010)\u001a\b\u0012\u0004\u0012\u00020*0\'J\u000e\u0010+\u001a\u00020\u00182\u0006\u0010,\u001a\u00020\u000fJ\u0014\u0010-\u001a\u00020\u00182\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\u000f0\'J\u000e\u0010.\u001a\u00020\u00182\u0006\u0010/\u001a\u00020 J\u000e\u00100\u001a\u00020\u00182\u0006\u0010,\u001a\u00020\u000fJ\u0014\u00101\u001a\u00020\u00182\f\u0010)\u001a\b\u0012\u0004\u0012\u00020*0\'J\u0016\u00102\u001a\u00020\u00182\u0006\u00103\u001a\u00020 2\u0006\u00104\u001a\u000205J\u0016\u00106\u001a\u00020\u00182\u0006\u00107\u001a\u0002082\u0006\u00109\u001a\u000205J\u000e\u0010:\u001a\u00020\u00182\u0006\u0010;\u001a\u00020\u000fJ\u000e\u0010<\u001a\u00020\u00182\u0006\u00107\u001a\u000208J\u0016\u0010=\u001a\u00020\u00182\u0006\u00107\u001a\u0002082\u0006\u0010>\u001a\u00020\u000fJ\u000e\u0010?\u001a\u00020\u00182\u0006\u00107\u001a\u000208R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\r0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0014\u00a8\u0006@"}, d2 = {"Lcom/ncy/notechaty/ui/chat/ChatViewModel;", "Landroidx/lifecycle/ViewModel;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "appContext", "Landroid/content/Context;", "(Landroidx/lifecycle/SavedStateHandle;Lcom/ncy/notechaty/data/repository/ChatRepository;Lcom/ncy/notechaty/data/repository/SettingsRepository;Landroid/content/Context;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ncy/notechaty/ui/chat/ChatUiState;", "chatId", "", "settings", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/ncy/notechaty/data/model/AppSettings;", "getSettings", "()Lkotlinx/coroutines/flow/StateFlow;", "uiState", "getUiState", "cancelEditing", "", "clearSearch", "deleteMessage", "messageId", "deleteMessages", "messageIds", "", "getChatDir", "Ljava/io/File;", "getSearchResultMessageId", "loadChat", "nextSearchResult", "previousSearchResult", "saveChecklistEdit", "items", "", "saveEdit", "blocks", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "saveEditPlainText", "text", "sendChecklistMessage", "sendImageMessage", "imageFile", "sendPlainTextMessage", "sendTextMessage", "sendVoiceMessage", "voiceFile", "durationMs", "", "setReminder", "message", "Lcom/ncy/notechaty/data/model/Message;", "triggerAtMillis", "setSearchQuery", "query", "startEditing", "toggleChecklistItem", "itemId", "toggleStar", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context appContext = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> settings = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ncy.notechaty.ui.chat.ChatUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.ui.chat.ChatUiState> uiState = null;
    
    @javax.inject.Inject()
    public ChatViewModel(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.SavedStateHandle savedStateHandle, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> getSettings() {
        return null;
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
    
    public final void saveChecklistEdit(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> items) {
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
    
    public final void setSearchQuery(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    public final void clearSearch() {
    }
    
    public final void nextSearchResult() {
    }
    
    public final void previousSearchResult() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSearchResultMessageId() {
        return null;
    }
    
    public final void sendChecklistMessage(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> items) {
    }
    
    public final void toggleChecklistItem(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Message message, @org.jetbrains.annotations.NotNull()
    java.lang.String itemId) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getChatDir() {
        return null;
    }
}