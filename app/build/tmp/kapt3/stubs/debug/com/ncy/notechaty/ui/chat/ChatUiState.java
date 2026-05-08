package com.ncy.notechaty.ui.chat;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.data.sync.ChatEventBus;
import com.ncy.notechaty.data.sync.media.MediaHash;
import com.ncy.notechaty.notifications.ReminderScheduler;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import java.util.UUID;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\"\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u001c\b\u0086\b\u0018\u00002\u00020\u0001B[\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\r\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\u0002\u0010\u0010J\u000b\u0010\u001f\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000f\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000b\u0010!\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010\"\u001a\u00020\tH\u00c6\u0003J\u000f\u0010#\u001a\b\u0012\u0004\u0012\u00020\t0\u000bH\u00c6\u0003J\t\u0010$\u001a\u00020\rH\u00c6\u0003J\t\u0010%\u001a\u00020\u000fH\u00c6\u0003J_\u0010&\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\b\u001a\u00020\t2\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u000fH\u00c6\u0001J\u0013\u0010\'\u001a\u00020\u000f2\b\u0010(\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010)\u001a\u00020\rH\u00d6\u0001J\t\u0010*\u001a\u00020\tH\u00d6\u0001R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001e\u00a8\u0006+"}, d2 = {"Lcom/ncy/notechaty/ui/chat/ChatUiState;", "", "chat", "Lcom/ncy/notechaty/data/model/Chat;", "messages", "", "Lcom/ncy/notechaty/data/model/Message;", "editingMessage", "searchQuery", "", "searchResultIds", "", "searchResultIndex", "", "unlockRequired", "", "(Lcom/ncy/notechaty/data/model/Chat;Ljava/util/List;Lcom/ncy/notechaty/data/model/Message;Ljava/lang/String;Ljava/util/Set;IZ)V", "getChat", "()Lcom/ncy/notechaty/data/model/Chat;", "getEditingMessage", "()Lcom/ncy/notechaty/data/model/Message;", "getMessages", "()Ljava/util/List;", "getSearchQuery", "()Ljava/lang/String;", "getSearchResultIds", "()Ljava/util/Set;", "getSearchResultIndex", "()I", "getUnlockRequired", "()Z", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class ChatUiState {
    @org.jetbrains.annotations.Nullable()
    private final com.ncy.notechaty.data.model.Chat chat = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.data.model.Message> messages = null;
    @org.jetbrains.annotations.Nullable()
    private final com.ncy.notechaty.data.model.Message editingMessage = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String searchQuery = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> searchResultIds = null;
    private final int searchResultIndex = 0;
    private final boolean unlockRequired = false;
    
    public ChatUiState(@org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Chat chat, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Message> messages, @org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Message editingMessage, @org.jetbrains.annotations.NotNull()
    java.lang.String searchQuery, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> searchResultIds, int searchResultIndex, boolean unlockRequired) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Chat getChat() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.Message> getMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Message getEditingMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSearchQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> getSearchResultIds() {
        return null;
    }
    
    public final int getSearchResultIndex() {
        return 0;
    }
    
    public final boolean getUnlockRequired() {
        return false;
    }
    
    public ChatUiState() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Chat component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.Message> component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Message component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> component5() {
        return null;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.ui.chat.ChatUiState copy(@org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Chat chat, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Message> messages, @org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Message editingMessage, @org.jetbrains.annotations.NotNull()
    java.lang.String searchQuery, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> searchResultIds, int searchResultIndex, boolean unlockRequired) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}