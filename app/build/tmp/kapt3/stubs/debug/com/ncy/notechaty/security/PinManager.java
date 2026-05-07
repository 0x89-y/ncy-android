package com.ncy.notechaty.security;

import com.ncy.notechaty.data.repository.SettingsRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007J\u0006\u0010\b\u001a\u00020\tJ\u0016\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\rJ\u0016\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\tH\u0086@\u00a2\u0006\u0002\u0010\u0010J\u000e\u0010\u0011\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/ncy/notechaty/security/PinManager;", "", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "(Lcom/ncy/notechaty/data/repository/SettingsRepository;)V", "clearPin", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "hasPin", "", "setPin", "pin", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setUseBiometric", "enabled", "(ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "verifyPin", "app_debug"})
public final class PinManager {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.SettingsRepository settingsRepository = null;
    
    @javax.inject.Inject()
    public PinManager(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository settingsRepository) {
        super();
    }
    
    public final boolean hasPin() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object setPin(@org.jetbrains.annotations.NotNull()
    java.lang.String pin, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final boolean verifyPin(@org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearPin(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object setUseBiometric(boolean enabled, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}