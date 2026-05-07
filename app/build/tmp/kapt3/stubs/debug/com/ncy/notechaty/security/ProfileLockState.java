package com.ncy.notechaty.security;

import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010%\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u0012J\u0006\u0010\u0013\u001a\u00020\u0014J\u000e\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0010\u001a\u00020\u0006R \u0010\u0003\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010\b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u00050\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001a\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/ncy/notechaty/security/ProfileLockState;", "", "()V", "_timestamps", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "", "", "timestamps", "Lkotlinx/coroutines/flow/StateFlow;", "getTimestamps", "()Lkotlinx/coroutines/flow/StateFlow;", "unlockedAt", "", "isUnlocked", "", "profileId", "gracePeriodMinutes", "", "lockAll", "", "unlock", "app_debug"})
public final class ProfileLockState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Long> unlockedAt = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.String, java.lang.Long>> _timestamps = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, java.lang.Long>> timestamps = null;
    
    @javax.inject.Inject()
    public ProfileLockState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, java.lang.Long>> getTimestamps() {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void unlock(@org.jetbrains.annotations.NotNull()
    java.lang.String profileId) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized boolean isUnlocked(@org.jetbrains.annotations.NotNull()
    java.lang.String profileId, int gracePeriodMinutes) {
        return false;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void lockAll() {
    }
}