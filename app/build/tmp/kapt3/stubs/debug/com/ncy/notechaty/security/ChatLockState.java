package com.ncy.notechaty.security;

import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0005J\u0006\u0010\t\u001a\u00020\nJ\u000e\u0010\u000b\u001a\u00020\n2\u0006\u0010\b\u001a\u00020\u0005R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/ncy/notechaty/security/ChatLockState;", "", "()V", "unlocked", "", "", "consume", "", "chatId", "lockAll", "", "unlock", "app_debug"})
public final class ChatLockState {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> unlocked = null;
    
    @javax.inject.Inject()
    public ChatLockState() {
        super();
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void unlock(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized boolean consume(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
        return false;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void lockAll() {
    }
}