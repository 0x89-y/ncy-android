package com.ncy.notechaty.data.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b+\b\u0086\b\u0018\u00002\u00020\u0001B\u0083\u0001\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\r\u0012\b\b\u0002\u0010\u000e\u001a\u00020\t\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u000b\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u000b\u0012\b\b\u0002\u0010\u0011\u001a\u00020\t\u0012\b\b\u0002\u0010\u0012\u001a\u00020\t\u0012\b\b\u0002\u0010\u0013\u001a\u00020\r\u00a2\u0006\u0002\u0010\u0014J\t\u0010\'\u001a\u00020\u0003H\u00c6\u0003J\t\u0010(\u001a\u00020\tH\u00c6\u0003J\t\u0010)\u001a\u00020\tH\u00c6\u0003J\t\u0010*\u001a\u00020\rH\u00c6\u0003J\t\u0010+\u001a\u00020\u0005H\u00c6\u0003J\t\u0010,\u001a\u00020\u0007H\u00c6\u0003J\t\u0010-\u001a\u00020\tH\u00c6\u0003J\u000b\u0010.\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J\t\u0010/\u001a\u00020\rH\u00c6\u0003J\t\u00100\u001a\u00020\tH\u00c6\u0003J\u000b\u00101\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J\u000b\u00102\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J\u0087\u0001\u00103\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\t2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u000b2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u000b2\b\b\u0002\u0010\u0011\u001a\u00020\t2\b\b\u0002\u0010\u0012\u001a\u00020\t2\b\b\u0002\u0010\u0013\u001a\u00020\rH\u00c6\u0001J\u0013\u00104\u001a\u00020\t2\b\u00105\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00106\u001a\u00020\rH\u00d6\u0001J\t\u00107\u001a\u00020\u000bH\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0013\u0010\n\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0011\u0010\u0012\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0018R\u0011\u0010\u0013\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001cR\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001aR\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001aR\u0011\u0010\u0011\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0018R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u0011\u0010\u000e\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u0018\u00a8\u00068"}, d2 = {"Lcom/ncy/notechaty/data/model/AppSettings;", "", "themeMode", "Lcom/ncy/notechaty/data/model/ThemeMode;", "accentColor", "Lcom/ncy/notechaty/data/model/AccentColor;", "chatSortMode", "Lcom/ncy/notechaty/data/model/ChatSortMode;", "autoBackupEnabled", "", "autoBackupFolder", "", "autoBackupIntervalHours", "", "topBarGradient", "protectionPinHash", "protectionPinSalt", "protectionUseBiometric", "lockOnAppStart", "protectionGracePeriodMinutes", "(Lcom/ncy/notechaty/data/model/ThemeMode;Lcom/ncy/notechaty/data/model/AccentColor;Lcom/ncy/notechaty/data/model/ChatSortMode;ZLjava/lang/String;IZLjava/lang/String;Ljava/lang/String;ZZI)V", "getAccentColor", "()Lcom/ncy/notechaty/data/model/AccentColor;", "getAutoBackupEnabled", "()Z", "getAutoBackupFolder", "()Ljava/lang/String;", "getAutoBackupIntervalHours", "()I", "getChatSortMode", "()Lcom/ncy/notechaty/data/model/ChatSortMode;", "getLockOnAppStart", "getProtectionGracePeriodMinutes", "getProtectionPinHash", "getProtectionPinSalt", "getProtectionUseBiometric", "getThemeMode", "()Lcom/ncy/notechaty/data/model/ThemeMode;", "getTopBarGradient", "component1", "component10", "component11", "component12", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class AppSettings {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.model.ThemeMode themeMode = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.model.AccentColor accentColor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.model.ChatSortMode chatSortMode = null;
    private final boolean autoBackupEnabled = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String autoBackupFolder = null;
    private final int autoBackupIntervalHours = 0;
    private final boolean topBarGradient = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String protectionPinHash = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String protectionPinSalt = null;
    private final boolean protectionUseBiometric = false;
    private final boolean lockOnAppStart = false;
    private final int protectionGracePeriodMinutes = 0;
    
    public AppSettings(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ThemeMode themeMode, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.AccentColor accentColor, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ChatSortMode chatSortMode, boolean autoBackupEnabled, @org.jetbrains.annotations.Nullable()
    java.lang.String autoBackupFolder, int autoBackupIntervalHours, boolean topBarGradient, @org.jetbrains.annotations.Nullable()
    java.lang.String protectionPinHash, @org.jetbrains.annotations.Nullable()
    java.lang.String protectionPinSalt, boolean protectionUseBiometric, boolean lockOnAppStart, int protectionGracePeriodMinutes) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ThemeMode getThemeMode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.AccentColor getAccentColor() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ChatSortMode getChatSortMode() {
        return null;
    }
    
    public final boolean getAutoBackupEnabled() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAutoBackupFolder() {
        return null;
    }
    
    public final int getAutoBackupIntervalHours() {
        return 0;
    }
    
    public final boolean getTopBarGradient() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProtectionPinHash() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProtectionPinSalt() {
        return null;
    }
    
    public final boolean getProtectionUseBiometric() {
        return false;
    }
    
    public final boolean getLockOnAppStart() {
        return false;
    }
    
    public final int getProtectionGracePeriodMinutes() {
        return 0;
    }
    
    public AppSettings() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ThemeMode component1() {
        return null;
    }
    
    public final boolean component10() {
        return false;
    }
    
    public final boolean component11() {
        return false;
    }
    
    public final int component12() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.AccentColor component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ChatSortMode component3() {
        return null;
    }
    
    public final boolean component4() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.AppSettings copy(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ThemeMode themeMode, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.AccentColor accentColor, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ChatSortMode chatSortMode, boolean autoBackupEnabled, @org.jetbrains.annotations.Nullable()
    java.lang.String autoBackupFolder, int autoBackupIntervalHours, boolean topBarGradient, @org.jetbrains.annotations.Nullable()
    java.lang.String protectionPinHash, @org.jetbrains.annotations.Nullable()
    java.lang.String protectionPinSalt, boolean protectionUseBiometric, boolean lockOnAppStart, int protectionGracePeriodMinutes) {
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