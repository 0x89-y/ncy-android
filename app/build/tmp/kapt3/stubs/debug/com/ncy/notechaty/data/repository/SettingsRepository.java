package com.ncy.notechaty.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.ncy.notechaty.data.model.AccentColor;
import com.ncy.notechaty.data.model.AppSettings;
import com.ncy.notechaty.data.model.ChatSortMode;
import com.ncy.notechaty.data.model.ThemeMode;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u000e\u001a\u00020\u0007H\u0002J\u0016\u0010\u000f\u001a\u00020\u00102\u0006\u0010\n\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\u0011R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0012"}, d2 = {"Lcom/ncy/notechaty/data/repository/SettingsRepository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_settings", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/ncy/notechaty/data/model/AppSettings;", "prefs", "Landroid/content/SharedPreferences;", "settings", "Lkotlinx/coroutines/flow/StateFlow;", "getSettings", "()Lkotlinx/coroutines/flow/StateFlow;", "loadSettings", "updateSettings", "", "(Lcom/ncy/notechaty/data/model/AppSettings;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class SettingsRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.ncy.notechaty.data.model.AppSettings> _settings = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> settings = null;
    
    @javax.inject.Inject()
    public SettingsRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ncy.notechaty.data.model.AppSettings> getSettings() {
        return null;
    }
    
    private final com.ncy.notechaty.data.model.AppSettings loadSettings() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateSettings(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.AppSettings settings, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}