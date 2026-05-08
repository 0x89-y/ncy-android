package com.ncy.notechaty.data.sync.media;

import java.io.File;
import java.security.MessageDigest;

/**
 * SHA-256 helpers for content-addressing media files. Hashes are lowercase hex strings —
 * 64 chars long. Used to name files on disk so the same byte content always lands at the
 * same path on every device.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0019\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0007\u001a\u0004\u0018\u00010\b2\b\u0010\t\u001a\u0004\u0018\u00010\bJ\u0010\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\bJ\u000e\u0010\r\u001a\u00020\b2\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0010\u001a\u00020\b2\u0006\u0010\u0011\u001a\u00020\u0012J\u0010\u0010\u0013\u001a\u00020\b2\u0006\u0010\u000e\u001a\u00020\u000fH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/ncy/notechaty/data/sync/media/MediaHash;", "", "()V", "HEX", "", "PATTERN", "Lkotlin/text/Regex;", "extractHash", "", "reference", "isValidHash", "", "s", "ofBytes", "bytes", "", "ofFile", "file", "Ljava/io/File;", "toHex", "app_debug"})
public final class MediaHash {
    @org.jetbrains.annotations.NotNull()
    private static final char[] HEX = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex PATTERN = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.data.sync.media.MediaHash INSTANCE = null;
    
    private MediaHash() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ofFile(@org.jetbrains.annotations.NotNull()
    java.io.File file) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ofBytes(@org.jetbrains.annotations.NotNull()
    byte[] bytes) {
        return null;
    }
    
    public final boolean isValidHash(@org.jetbrains.annotations.Nullable()
    java.lang.String s) {
        return false;
    }
    
    /**
     * Extract the SHA-256 hash from a content-addressed media reference like
     * `voice/<hash>.m4a` or `images/<hash>.jpg`. Returns null if the reference doesn't
     * follow the post-phase-2b naming convention (legacy random-named media).
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String extractHash(@org.jetbrains.annotations.Nullable()
    java.lang.String reference) {
        return null;
    }
    
    private final java.lang.String toHex(byte[] bytes) {
        return null;
    }
}