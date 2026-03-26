package com.ncy.notechaty.ui.settings;

import android.content.Context;
import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.backup.BackupScheduler;
import com.ncy.notechaty.data.model.AccentColor;
import com.ncy.notechaty.data.model.AppSettings;
import com.ncy.notechaty.data.model.ThemeMode;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B!\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u0016\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u000e\u0010\u0015\u001a\u00020\u000f2\u0006\u0010\u0016\u001a\u00020\u0017J\u000e\u0010\u0018\u001a\u00020\u000f2\u0006\u0010\u0019\u001a\u00020\u001aJ\u000e\u0010\u001b\u001a\u00020\u000f2\u0006\u0010\u001c\u001a\u00020\u001dJ\u000e\u0010\u001e\u001a\u00020\u000f2\u0006\u0010\u001f\u001a\u00020 R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/ncy/notechaty/ui/settings/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "context", "Landroid/content/Context;", "(Lcom/ncy/notechaty/data/repository/ChatRepository;Lcom/ncy/notechaty/data/repository/SettingsRepository;Landroid/content/Context;)V", "settings", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/ncy/notechaty/data/model/AppSettings;", "getSettings", "()Lkotlinx/coroutines/flow/StateFlow;", "exportAllChats", "", "outputFile", "Ljava/io/File;", "(Ljava/io/File;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "importChat", "zipFile", "setAccentColor", "color", "Lcom/ncy/notechaty/data/model/AccentColor;", "setAutoBackupEnabled", "enabled", "", "setAutoBackupInterval", "hours", "", "setThemeMode", "mode", "Lcom/ncy/notechaty/data/model/ThemeMode;", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> settings = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> getSettings() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object importChat(@org.jetbrains.annotations.NotNull()
    java.io.File zipFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void setThemeMode(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ThemeMode mode) {
    }
    
    public final void setAccentColor(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.AccentColor color) {
    }
    
    public final void setAutoBackupEnabled(boolean enabled) {
    }
    
    public final void setAutoBackupInterval(int hours) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object exportAllChats(@org.jetbrains.annotations.NotNull()
    java.io.File outputFile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}