package com.ncy.notechaty.ui.chatlist;

import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.model.ChatSortMode;
import com.ncy.notechaty.data.model.Profile;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.ProfileRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import com.ncy.notechaty.security.ProfileLockState;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.*;
import java.io.File;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B7\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u000e\u00100\u001a\u0002012\u0006\u00102\u001a\u00020\u0011J\u000e\u00103\u001a\u0002012\u0006\u00104\u001a\u00020\u0011J\u0010\u00105\u001a\u0004\u0018\u00010\u00142\u0006\u00104\u001a\u00020\u0011J\u000e\u00106\u001a\u0002012\u0006\u00107\u001a\u00020\u0011J\u000e\u00108\u001a\u0002092\u0006\u00107\u001a\u00020\u0011J\u000e\u0010:\u001a\u0002012\u0006\u00107\u001a\u00020\u0011J\u0006\u0010;\u001a\u000201J\u000e\u0010<\u001a\u0002012\u0006\u0010=\u001a\u00020\u0011J\u000e\u0010>\u001a\u0002012\u0006\u0010?\u001a\u00020@J\u000e\u0010A\u001a\u00020B2\u0006\u0010C\u001a\u00020\u0011J\u000e\u0010D\u001a\u0002012\u0006\u0010E\u001a\u00020\u001bJ\u000e\u0010F\u001a\u0002012\u0006\u0010E\u001a\u00020\u001bJ\u000e\u0010G\u001a\u0002092\u0006\u0010H\u001a\u00020\u0011J\u0016\u0010I\u001a\u0002092\u0006\u00102\u001a\u00020\u00112\u0006\u0010H\u001a\u00020\u0011R\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0012\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00140\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00110\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0016R\u001d\u0010\u0019\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0\u001a0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0016R\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0016R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u001d\u0010\"\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0\u001a0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0016R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010&\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u001a0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00110\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010\u0016R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020+0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010-\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110.0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\u0016\u00a8\u0006J"}, d2 = {"Lcom/ncy/notechaty/ui/chatlist/ChatListViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "profileLockState", "Lcom/ncy/notechaty/security/ProfileLockState;", "pinManager", "Lcom/ncy/notechaty/security/PinManager;", "chatLockState", "Lcom/ncy/notechaty/security/ChatLockState;", "(Lcom/ncy/notechaty/data/repository/ChatRepository;Lcom/ncy/notechaty/data/repository/SettingsRepository;Lcom/ncy/notechaty/data/repository/ProfileRepository;Lcom/ncy/notechaty/security/ProfileLockState;Lcom/ncy/notechaty/security/PinManager;Lcom/ncy/notechaty/security/ChatLockState;)V", "_searchQuery", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "activeProfile", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/ncy/notechaty/data/model/Profile;", "getActiveProfile", "()Lkotlinx/coroutines/flow/StateFlow;", "activeProfileId", "getActiveProfileId", "archivedChats", "", "Lcom/ncy/notechaty/data/model/Chat;", "getArchivedChats", "baseDir", "Ljava/io/File;", "getBaseDir", "getChatLockState", "()Lcom/ncy/notechaty/security/ChatLockState;", "chats", "getChats", "getPinManager", "()Lcom/ncy/notechaty/security/PinManager;", "profiles", "getProfiles", "searchQuery", "getSearchQuery", "settings", "Lcom/ncy/notechaty/data/model/AppSettings;", "getSettings", "unlockedChatIds", "", "getUnlockedChatIds", "confirmProfileUnlock", "", "profileId", "createChat", "name", "createProfileAndSwitch", "deleteChat", "chatId", "isChatUnlockedWithinGrace", "", "markUnlocked", "refresh", "setSearchQuery", "query", "setSortMode", "mode", "Lcom/ncy/notechaty/data/model/ChatSortMode;", "switchProfile", "Lcom/ncy/notechaty/ui/chatlist/SwitchResult;", "targetId", "toggleArchive", "chat", "togglePin", "verifyPin", "pin", "verifyProfilePin", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ChatListViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ProfileRepository profileRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.ProfileLockState profileLockState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.PinManager pinManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.security.ChatLockState chatLockState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.io.File> baseDir = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> settings = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> profiles = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> activeProfileId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.Profile> activeProfile = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> chats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Chat>> archivedChats = null;
    
    /**
     * Chat ids that are currently inside the unlock grace window (or app-wide unlocked). Empty when grace = 0.
     */
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Set<java.lang.String>> unlockedChatIds = null;
    
    @javax.inject.Inject()
    public ChatListViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository profileRepository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.ProfileLockState profileLockState, @org.jetbrains.annotations.NotNull()
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
    public final kotlinx.coroutines.flow.StateFlow<java.io.File> getBaseDir() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> getSettings() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> getProfiles() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getActiveProfileId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.Profile> getActiveProfile() {
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
    
    /**
     * Attempt a profile switch. If the target profile is protected and not within its
     * unlock grace, returns [SwitchResult.RequiresAuth] without switching — the screen
     * should then show a PIN/biometric prompt and call [confirmProfileUnlock] on success.
     */
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.ui.chatlist.SwitchResult switchProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String targetId) {
        return null;
    }
    
    public final boolean verifyProfilePin(@org.jetbrains.annotations.NotNull()
    java.lang.String profileId, @org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
        return false;
    }
    
    public final void confirmProfileUnlock(@org.jetbrains.annotations.NotNull()
    java.lang.String profileId) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Profile createProfileAndSwitch(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
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
    
    /**
     * Chat ids that are currently inside the unlock grace window (or app-wide unlocked). Empty when grace = 0.
     */
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Set<java.lang.String>> getUnlockedChatIds() {
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