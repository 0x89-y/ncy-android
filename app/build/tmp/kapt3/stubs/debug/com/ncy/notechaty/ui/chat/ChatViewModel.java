package com.ncy.notechaty.ui.chat;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.data.sync.ChatEventBus;
import com.ncy.notechaty.data.sync.media.MediaHash;
import com.ncy.notechaty.data.sync.protection.ProtectionKeyStore;
import com.ncy.notechaty.notifications.ReminderScheduler;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import java.util.UUID;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0094\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\"\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001BI\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\b\b\u0001\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\u0002\u0010\u0012J\u0006\u0010!\u001a\u00020\"J\u0006\u0010#\u001a\u00020\"J\u000e\u0010$\u001a\u00020\"2\u0006\u0010%\u001a\u00020\u0017J\u0014\u0010&\u001a\u00020\"2\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00170(J\u0006\u0010)\u001a\u00020*J\b\u0010+\u001a\u0004\u0018\u00010\u0017J \u0010,\u001a\u00020*2\u0006\u0010-\u001a\u00020*2\u0006\u0010.\u001a\u00020*2\u0006\u0010/\u001a\u00020\u0017H\u0002J\u0006\u00100\u001a\u00020\"J\u0006\u00101\u001a\u00020\"J\u0006\u00102\u001a\u00020\"J\u0006\u00103\u001a\u00020\"J\u0014\u00104\u001a\u00020\"2\f\u00105\u001a\b\u0012\u0004\u0012\u00020\u001706J\u0014\u00107\u001a\u00020\"2\f\u00108\u001a\b\u0012\u0004\u0012\u00020906J\u000e\u0010:\u001a\u00020\"2\u0006\u0010;\u001a\u00020\u0017J\u0014\u0010<\u001a\u00020\"2\f\u00105\u001a\b\u0012\u0004\u0012\u00020\u001706J\u000e\u0010=\u001a\u00020\"2\u0006\u0010>\u001a\u00020*J\u000e\u0010?\u001a\u00020\"2\u0006\u0010;\u001a\u00020\u0017J\u0014\u0010@\u001a\u00020\"2\f\u00108\u001a\b\u0012\u0004\u0012\u00020906J\u0016\u0010A\u001a\u00020\"2\u0006\u0010B\u001a\u00020*2\u0006\u0010C\u001a\u00020DJ\u0016\u0010E\u001a\u00020\"2\u0006\u0010F\u001a\u00020G2\u0006\u0010H\u001a\u00020DJ\u000e\u0010I\u001a\u00020\"2\u0006\u0010J\u001a\u00020\u0017J\u000e\u0010K\u001a\u00020\"2\u0006\u0010F\u001a\u00020GJ\u0016\u0010L\u001a\u00020\"2\u0006\u0010F\u001a\u00020G2\u0006\u0010M\u001a\u00020\u0017J\u000e\u0010N\u001a\u00020\"2\u0006\u0010F\u001a\u00020GJ\u000e\u0010O\u001a\u00020\u00192\u0006\u0010P\u001a\u00020\u0017R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00150\u001b\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001e\u00a8\u0006Q"}, d2 = {"Lcom/ncy/notechaty/ui/chat/ChatViewModel;", "Landroidx/lifecycle/ViewModel;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "chatLockState", "Lcom/ncy/notechaty/security/ChatLockState;", "pinManager", "Lcom/ncy/notechaty/security/PinManager;", "eventBus", "Lcom/ncy/notechaty/data/sync/ChatEventBus;", "keyStore", "Lcom/ncy/notechaty/data/sync/protection/ProtectionKeyStore;", "appContext", "Landroid/content/Context;", "(Landroidx/lifecycle/SavedStateHandle;Lcom/ncy/notechaty/data/repository/ChatRepository;Lcom/ncy/notechaty/data/repository/SettingsRepository;Lcom/ncy/notechaty/security/ChatLockState;Lcom/ncy/notechaty/security/PinManager;Lcom/ncy/notechaty/data/sync/ChatEventBus;Lcom/ncy/notechaty/data/sync/protection/ProtectionKeyStore;Landroid/content/Context;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ncy/notechaty/ui/chat/ChatUiState;", "chatId", "", "hasUnlockToken", "", "settings", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/ncy/notechaty/data/model/AppSettings;", "getSettings", "()Lkotlinx/coroutines/flow/StateFlow;", "uiState", "getUiState", "cancelEditing", "", "clearSearch", "deleteMessage", "messageId", "deleteMessages", "messageIds", "", "getChatDir", "Ljava/io/File;", "getSearchResultMessageId", "ingestMedia", "destDir", "source", "ext", "loadChat", "markUnlocked", "nextSearchResult", "previousSearchResult", "saveChecklistEdit", "items", "", "saveEdit", "blocks", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "saveEditPlainText", "text", "sendChecklistMessage", "sendImageMessage", "imageFile", "sendPlainTextMessage", "sendTextMessage", "sendVoiceMessage", "voiceFile", "durationMs", "", "setReminder", "message", "Lcom/ncy/notechaty/data/model/Message;", "triggerAtMillis", "setSearchQuery", "query", "startEditing", "toggleChecklistItem", "itemId", "toggleStar", "verifyPin", "pin", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.ChatLockState chatLockState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.PinManager pinManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.ChatEventBus eventBus = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.protection.ProtectionKeyStore keyStore = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context appContext = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    private boolean hasUnlockToken;
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
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.ChatLockState chatLockState, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.PinManager pinManager, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.ChatEventBus eventBus, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.protection.ProtectionKeyStore keyStore, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext) {
        super();
    }
    
    public final void markUnlocked() {
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
    
    /**
     * Verify the user's PIN against the app-wide hash. Side-effect on success: derive
     * and cache the per-chat AES-256 key when this chat has a phase-3 `protectionSalt`,
     * so subsequent author/read paths can encrypt + decrypt without a second PIN prompt.
     */
    public final boolean verifyPin(@org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
        return false;
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
    
    /**
     * Copy [source] into [destDir] under a content-addressed name (`<sha256>.<ext>`).
     * Subsequent ingests of the same bytes reuse the existing file (dedup). The
     * SHA-256 in the filename is what the desktop later asks for via `BlobRequest`.
     */
    private final java.io.File ingestMedia(java.io.File destDir, java.io.File source, java.lang.String ext) {
        return null;
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