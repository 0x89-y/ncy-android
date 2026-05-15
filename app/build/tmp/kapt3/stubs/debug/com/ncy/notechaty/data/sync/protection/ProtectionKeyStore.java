package com.ncy.notechaty.data.sync.protection;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Holds derived AES-256 keys for protected chats in memory only, keyed by chatId.
 * Keys are derived via PBKDF2 from the user's PIN plus the chat's `protectionSalt`
 * at unlock time and cleared on lock — never persisted.
 *
 * This is the bridge between the UI (which has the live PIN at the moment of entry
 * and the salt from `ChatMetadata`) and the repository (which needs the raw 32-byte
 * key to encrypt/decrypt event payloads but should never see the PIN itself).
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0005J\u0010\u0010\n\u001a\u0004\u0018\u00010\u00062\u0006\u0010\t\u001a\u00020\u0005J\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\t\u001a\u00020\u0005J\u0006\u0010\r\u001a\u00020\fJ\u001e\u0010\u000e\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\u00052\u0006\u0010\u0010\u001a\u00020\u0005R\u001a\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/data/sync/protection/ProtectionKeyStore;", "", "()V", "keys", "", "", "", "isUnlocked", "", "chatId", "keyFor", "lock", "", "lockAll", "unlock", "pin", "saltB64", "app_debug"})
public final class ProtectionKeyStore {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, byte[]> keys = null;
    
    @javax.inject.Inject()
    public ProtectionKeyStore() {
        super();
    }
    
    /**
     * Derive and cache the key for [chatId] from [pin] + [saltB64]. Idempotent —
     * re-deriving with the same inputs replaces the cached value. Returns the key
     * for callers that want to use it immediately without a second lookup.
     */
    @kotlin.jvm.Synchronized()
    @org.jetbrains.annotations.NotNull()
    public final synchronized byte[] unlock(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String pin, @org.jetbrains.annotations.NotNull()
    java.lang.String saltB64) {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    @org.jetbrains.annotations.Nullable()
    public final synchronized byte[] keyFor(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized boolean isUnlocked(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
        return false;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void lock(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void lockAll() {
    }
}