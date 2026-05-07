package com.ncy.notechaty.security;

import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010%\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0011\u001a\u00020\u0012J\u0006\u0010\u0013\u001a\u00020\u0014J\u0016\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0016\u001a\u00020\b2\u0006\u0010\u0017\u001a\u00020\u0018J\u0006\u0010\u0019\u001a\u00020\u0012J\u000e\u0010\u001a\u001a\u00020\u00122\u0006\u0010\u0016\u001a\u00020\bJ\u0006\u0010\u001b\u001a\u00020\u0012R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u0006\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00050\u00070\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00050\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR#\u0010\r\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00050\u00070\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u001a\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00050\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/ncy/notechaty/security/ChatLockState;", "", "()V", "_appUnlockedAt", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_timestamps", "", "", "appUnlockedAt", "Lkotlinx/coroutines/flow/StateFlow;", "getAppUnlockedAt", "()Lkotlinx/coroutines/flow/StateFlow;", "timestamps", "getTimestamps", "unlockedAt", "", "clearChatUnlocks", "", "isAppUnlocked", "", "isUnlocked", "chatId", "gracePeriodMinutes", "", "lockAll", "unlock", "unlockApp", "app_debug"})
public final class ChatLockState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Long> unlockedAt = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.String, java.lang.Long>> _timestamps = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, java.lang.Long>> timestamps = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _appUnlockedAt = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> appUnlockedAt = null;
    
    @javax.inject.Inject()
    public ChatLockState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, java.lang.Long>> getTimestamps() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getAppUnlockedAt() {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void unlock(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized boolean isUnlocked(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, int gracePeriodMinutes) {
        return false;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void unlockApp() {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized boolean isAppUnlocked() {
        return false;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void lockAll() {
    }
    
    /**
     * Clear per-chat unlocks without touching the app-wide unlock. Used on profile switch.
     */
    @kotlin.jvm.Synchronized()
    public final synchronized void clearChatUnlocks() {
    }
}