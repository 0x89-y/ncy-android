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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B-\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\bJ\u000b\u0010\u000f\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000b\u0010\u0011\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J1\u0010\u0012\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0006H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u001a"}, d2 = {"Lcom/ncy/notechaty/ui/chat/ChatUiState;", "", "chat", "Lcom/ncy/notechaty/data/model/Chat;", "messages", "", "Lcom/ncy/notechaty/data/model/Message;", "editingMessage", "(Lcom/ncy/notechaty/data/model/Chat;Ljava/util/List;Lcom/ncy/notechaty/data/model/Message;)V", "getChat", "()Lcom/ncy/notechaty/data/model/Chat;", "getEditingMessage", "()Lcom/ncy/notechaty/data/model/Message;", "getMessages", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
public final class ChatUiState {
    @org.jetbrains.annotations.Nullable()
    private final com.ncy.notechaty.data.model.Chat chat = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.data.model.Message> messages = null;
    @org.jetbrains.annotations.Nullable()
    private final com.ncy.notechaty.data.model.Message editingMessage = null;
    
    public ChatUiState(@org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Chat chat, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Message> messages, @org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Message editingMessage) {
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
    public final com.ncy.notechaty.ui.chat.ChatUiState copy(@org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Chat chat, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Message> messages, @org.jetbrains.annotations.Nullable()
    com.ncy.notechaty.data.model.Message editingMessage) {
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