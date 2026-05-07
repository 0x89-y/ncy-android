package com.ncy.notechaty.security;

import java.security.MessageDigest;
import java.security.SecureRandom;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u00c0\u0002\u0018\u00002\u00020\u0001:\u0001\u0012B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0002J\u0010\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u0004H\u0002J\u0018\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u0004H\u0002J\u000e\u0010\f\u001a\u00020\r2\u0006\u0010\n\u001a\u00020\u0006J\u001e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u0006\u00a8\u0006\u0013"}, d2 = {"Lcom/ncy/notechaty/security/PinHasher;", "", "()V", "decode", "", "s", "", "encode", "bytes", "hash", "pin", "salt", "newHash", "Lcom/ncy/notechaty/security/PinHasher$HashedPin;", "verify", "", "savedHash", "savedSaltB64", "HashedPin", "app_debug"})
public final class PinHasher {
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.security.PinHasher INSTANCE = null;
    
    private PinHasher() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.security.PinHasher.HashedPin newHash(@org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
        return null;
    }
    
    public final boolean verify(@org.jetbrains.annotations.NotNull()
    java.lang.String pin, @org.jetbrains.annotations.NotNull()
    java.lang.String savedHash, @org.jetbrains.annotations.NotNull()
    java.lang.String savedSaltB64) {
        return false;
    }
    
    private final java.lang.String hash(java.lang.String pin, byte[] salt) {
        return null;
    }
    
    private final java.lang.String encode(byte[] bytes) {
        return null;
    }
    
    private final byte[] decode(java.lang.String s) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001J\t\u0010\u0011\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0012"}, d2 = {"Lcom/ncy/notechaty/security/PinHasher$HashedPin;", "", "hash", "", "saltB64", "(Ljava/lang/String;Ljava/lang/String;)V", "getHash", "()Ljava/lang/String;", "getSaltB64", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class HashedPin {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String hash = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String saltB64 = null;
        
        public HashedPin(@org.jetbrains.annotations.NotNull()
        java.lang.String hash, @org.jetbrains.annotations.NotNull()
        java.lang.String saltB64) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getHash() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getSaltB64() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.security.PinHasher.HashedPin copy(@org.jetbrains.annotations.NotNull()
        java.lang.String hash, @org.jetbrains.annotations.NotNull()
        java.lang.String saltB64) {
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
}