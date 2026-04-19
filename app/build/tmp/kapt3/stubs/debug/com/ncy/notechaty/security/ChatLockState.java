package com.ncy.notechaty.security;

import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\b\u001a\u00020\tJ\u0016\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\u00072\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u000b\u001a\u00020\u0007J\u0006\u0010\u0011\u001a\u00020\u000fR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00040\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/ncy/notechaty/security/ChatLockState;", "", "()V", "appUnlockedAt", "", "unlockedAt", "", "", "isAppUnlocked", "", "isUnlocked", "chatId", "gracePeriodMinutes", "", "lockAll", "", "unlock", "unlockApp", "app_debug"})
public final class ChatLockState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Long> unlockedAt = null;
    private long appUnlockedAt = 0L;
    
    @javax.inject.Inject()
    public ChatLockState() {
        super();
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
}