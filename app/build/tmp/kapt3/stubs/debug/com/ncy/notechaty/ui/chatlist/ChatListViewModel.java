package com.ncy.notechaty.ui.chatlist;

import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.model.ChatSortMode;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.io.File;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\'\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u000e\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020\rJ\u000e\u0010&\u001a\u00020$2\u0006\u0010\'\u001a\u00020\rJ\u000e\u0010(\u001a\u00020)2\u0006\u0010\'\u001a\u00020\rJ\u000e\u0010*\u001a\u00020$2\u0006\u0010\'\u001a\u00020\rJ\u0006\u0010+\u001a\u00020$J\u000e\u0010,\u001a\u00020$2\u0006\u0010-\u001a\u00020\rJ\u000e\u0010.\u001a\u00020$2\u0006\u0010/\u001a\u000200J\u000e\u00101\u001a\u00020$2\u0006\u00102\u001a\u00020\u0011J\u000e\u00103\u001a\u00020$2\u0006\u00102\u001a\u00020\u0011J\u000e\u00104\u001a\u00020)2\u0006\u00105\u001a\u00020\rR\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0014\u001a\u00020\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u001d\u0010\u001a\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0013R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0013R\u0017\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u0013R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00066"}, d2 = {"Lcom/ncy/notechaty/ui/chatlist/ChatListViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "pinManager", "Lcom/ncy/notechaty/security/PinManager;", "chatLockState", "Lcom/ncy/notechaty/security/ChatLockState;", "(Lcom/ncy/notechaty/data/repository/ChatRepository;Lcom/ncy/notechaty/data/repository/SettingsRepository;Lcom/ncy/notechaty/security/PinManager;Lcom/ncy/notechaty/security/ChatLockState;)V", "_searchQuery", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "archivedChats", "Lkotlinx/coroutines/flow/StateFlow;", "", "Lcom/ncy/notechaty/data/model/Chat;", "getArchivedChats", "()Lkotlinx/coroutines/flow/StateFlow;", "baseDir", "Ljava/io/File;", "getBaseDir", "()Ljava/io/File;", "getChatLockState", "()Lcom/ncy/notechaty/security/ChatLockState;", "chats", "getChats", "getPinManager", "()Lcom/ncy/notechaty/security/PinManager;", "searchQuery", "getSearchQuery", "settings", "Lcom/ncy/notechaty/data/model/AppSettings;", "getSettings", "createChat", "", "name", "deleteChat", "chatId", "isChatUnlockedWithinGrace", "", "markUnlocked", "refresh", "setSearchQuery", "query", "setSortMode", "mode", "Lcom/ncy/notechaty/data/model/ChatSortMode;", "toggleArchive", "chat", "togglePin", "verifyPin", "pin", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatListViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.PinManager pinManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.ChatLockState chatLockState = null;
    @org.jetbrains.annotations.NotNull()
    private final java.io.File baseDir = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> settings = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> chats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> archivedChats = null;
    
    @javax.inject.Inject()
    public ChatListViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.PinManager pinManager, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.ChatLockState chatLockState) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.security.PinManager getPinManager() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.security.ChatLockState getChatLockState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getBaseDir() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> getSettings() {
        return null;
    }
    
    public final boolean verifyPin(@org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
        return false;
    }
    
    public final void markUnlocked(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
    }
    
    /**
     * Returns true only when grace > 0 AND the chat is within its unlock window.
     */
    public final boolean isChatUnlockedWithinGrace(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getSearchQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> getChats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> getArchivedChats() {
        return null;
    }
    
    public final void createChat(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void deleteChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
    }
    
    public final void togglePin(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Chat chat) {
    }
    
    public final void toggleArchive(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Chat chat) {
    }
    
    public final void setSearchQuery(@org.jetbrains.annotations.NotNull()
    java.lang.String query) {
    }
    
    public final void setSortMode(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ChatSortMode mode) {
    }
    
    public final void refresh() {
    }
}