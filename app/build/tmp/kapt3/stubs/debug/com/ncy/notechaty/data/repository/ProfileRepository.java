package com.ncy.notechaty.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ncy.notechaty.data.model.Profile;
import com.ncy.notechaty.security.PinHasher;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import java.io.File;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u0007\u0018\u0000 /2\u00020\u0001:\u0001/B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0015\u001a\u0004\u0018\u00010\nJ\u000e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0007J\u000e\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\u0007J\u000e\u0010\u001b\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0007J\u0010\u0010\u001c\u001a\u00020\n2\b\b\u0002\u0010\u001a\u001a\u00020\u0007J\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u001e\u001a\u00020\u0007J\u000e\u0010\u001f\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\nJ\b\u0010!\u001a\u00020\u0017H\u0002J\b\u0010\"\u001a\u00020\u0017H\u0002J\u0010\u0010#\u001a\u0004\u0018\u00010\n2\u0006\u0010\u0018\u001a\u00020\u0007J\u000e\u0010$\u001a\u00020%2\u0006\u0010\u0018\u001a\u00020\u0007J\u0016\u0010&\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010\u001a\u001a\u00020\u0007J\u0006\u0010\'\u001a\u00020%J\u000e\u0010(\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0007J\u0016\u0010)\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010*\u001a\u00020\u0007J\u0016\u0010+\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010,\u001a\u00020\u0007J\u0016\u0010-\u001a\u00020.2\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010,\u001a\u00020\u0007R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00070\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000e\u00a8\u00060"}, d2 = {"Lcom/ncy/notechaty/data/repository/ProfileRepository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_activeProfileId", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_profiles", "", "Lcom/ncy/notechaty/data/model/Profile;", "activeProfileId", "Lkotlinx/coroutines/flow/StateFlow;", "getActiveProfileId", "()Lkotlinx/coroutines/flow/StateFlow;", "gson", "Lcom/google/gson/Gson;", "prefs", "Landroid/content/SharedPreferences;", "profiles", "getProfiles", "activeProfile", "clearProfilePin", "", "id", "createProfile", "name", "deleteProfile", "ensureDefault", "findProfileForChat", "chatId", "installInitial", "profile", "load", "persist", "profileById", "profileDir", "Ljava/io/File;", "renameProfile", "rootDir", "setActive", "setProfileIcon", "iconKey", "setProfilePin", "pin", "verifyProfilePin", "", "Companion", "app_debug"})
public final class ProfileRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> _profiles = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> profiles = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _activeProfileId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> activeProfileId = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_PROFILES = "profiles";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ACTIVE = "active_profile_id";
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.data.repository.ProfileRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public ProfileRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> getProfiles() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getActiveProfileId() {
        return null;
    }
    
    private final void load() {
    }
    
    private final void persist() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Profile activeProfile() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.Profile profileById(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return null;
    }
    
    /**
     * Bootstrap: ensure at least one profile exists. Used by migration.
     */
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.Profile ensureDefault(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
    }
    
    /**
     * Used by migration only — installs an explicit profile (with a known id) and sets it active.
     */
    public final void installInitial(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.Profile profile) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.Profile createProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
    }
    
    public final void renameProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void deleteProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void setActive(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void setProfilePin(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
    }
    
    public final void clearProfilePin(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void setProfileIcon(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String iconKey) {
    }
    
    public final boolean verifyProfilePin(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
        return false;
    }
    
    /**
     * Walk every profile directory to find the one owning a given chat id. Profile-agnostic.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String findProfileForChat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File rootDir() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File profileDir(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/ncy/notechaty/data/repository/ProfileRepository$Companion;", "", "()V", "KEY_ACTIVE", "", "KEY_PROFILES", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}