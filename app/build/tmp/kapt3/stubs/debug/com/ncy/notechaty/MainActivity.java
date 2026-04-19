package com.ncy.notechaty;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.compose.ui.Modifier;
import androidx.core.view.WindowInsetsControllerCompat;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0014R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001e\u0010\u000f\u001a\u00020\u00108\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0004\b\u0013\u0010\u0014\u00a8\u0006\u0019"}, d2 = {"Lcom/ncy/notechaty/MainActivity;", "Landroidx/fragment/app/FragmentActivity;", "()V", "chatLockState", "Lcom/ncy/notechaty/security/ChatLockState;", "getChatLockState", "()Lcom/ncy/notechaty/security/ChatLockState;", "setChatLockState", "(Lcom/ncy/notechaty/security/ChatLockState;)V", "pinManager", "Lcom/ncy/notechaty/security/PinManager;", "getPinManager", "()Lcom/ncy/notechaty/security/PinManager;", "setPinManager", "(Lcom/ncy/notechaty/security/PinManager;)V", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "getSettingsRepository", "()Lcom/ncy/notechaty/data/repository/SettingsRepository;", "setSettingsRepository", "(Lcom/ncy/notechaty/data/repository/SettingsRepository;)V", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "app_debug"})
public final class MainActivity extends androidx.fragment.app.FragmentActivity {
    @javax.inject.Inject()
    public com.ncy.notechaty.data.repository.SettingsRepository settingsRepository;
    @javax.inject.Inject()
    public com.ncy.notechaty.security.PinManager pinManager;
    @javax.inject.Inject()
    public com.ncy.notechaty.security.ChatLockState chatLockState;
    
    public MainActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.repository.SettingsRepository getSettingsRepository() {
        return null;
    }
    
    public final void setSettingsRepository(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.security.PinManager getPinManager() {
        return null;
    }
    
    public final void setPinManager(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.PinManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.security.ChatLockState getChatLockState() {
        return null;
    }
    
    public final void setChatLockState(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.security.ChatLockState p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
}