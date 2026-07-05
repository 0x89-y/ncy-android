package com.ncy.notechaty.data.repository;

import android.content.Context;
import com.ncy.notechaty.data.model.*;
import com.ncy.notechaty.data.serialization.ChatSerializer;
import com.google.gson.Gson;
import com.ncy.notechaty.data.sync.ChatEventBus;
import com.ncy.notechaty.data.sync.DeviceIdentity;
import com.ncy.notechaty.data.sync.media.MediaHash;
import com.ncy.notechaty.data.sync.protection.ProtectionCrypto;
import com.ncy.notechaty.data.sync.protection.ProtectionKeyStore;
import com.ncy.notechaty.data.sync.event.ChatEvent;
import com.ncy.notechaty.data.sync.event.ChatEventCodec;
import com.ncy.notechaty.data.sync.event.ChatEventOp;
import com.ncy.notechaty.data.sync.event.EventLog;
import com.ncy.notechaty.data.sync.event.EventLogMigrator;
import com.ncy.notechaty.data.sync.event.HlcTimestamp;
import com.ncy.notechaty.data.sync.event.MaterializedView;
import com.ncy.notechaty.security.ChatLockState;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import androidx.annotation.Keep;
import android.util.Log;

/**
 * Profile registry snapshot embedded in a full backup zip as `profiles.json`.
 * Profiles live in SharedPreferences, not on disk, so without this a restored
 * `profile_<id>/` directory tree would be orphaned (no entry in the switcher).
 * Carries the full [Profile] (name, icon, and PIN hash/salt) so a restore is
 * full-fidelity. Optional: absent in pre-manifest / single-chat backups.
 *
 * Public + `@Keep` because Gson reflects on it; private/minified classes are
 * unreliable to instantiate. The secondary no-arg constructor is belt-and-
 * suspenders so Gson never has to fall back to `Unsafe`-allocation.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002B7\u0012\u000e\b\u0002\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\nJ\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010\u0013\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\u000b\u0010\u0014\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J;\u0010\u0015\u001a\u00020\u00002\u000e\b\u0002\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\b\b\u0002\u0010\u0006\u001a\u00020\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0007H\u00c6\u0001J\u0013\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0013\u0010\b\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0013\u0010\t\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006\u001c"}, d2 = {"Lcom/ncy/notechaty/data/repository/BackupManifest;", "", "()V", "profiles", "", "Lcom/ncy/notechaty/data/model/Profile;", "activeProfileId", "", "appPinHash", "appPinSalt", "(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getActiveProfileId", "()Ljava/lang/String;", "getAppPinHash", "getAppPinSalt", "getProfiles", "()Ljava/util/List;", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
@androidx.annotation.Keep()
public final class BackupManifest {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.data.model.Profile> profiles = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String activeProfileId = null;
    
    /**
     * The device's app-wide protection PIN, so a fresh-install restore can
     * unlock protected chats (whose sealed content is encrypted under
     * `PBKDF2(that PIN, ChatMetadata.protectionSalt)`). Without this, the
     * salt+ciphertext survive but there's nothing to check the entered PIN
     * against, so `ChatViewModel.verifyPin` refuses every PIN. Both fields
     * are `null` on backups from devices with no app PIN set, and on legacy
     * backups from before this field existed.
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String appPinHash = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String appPinSalt = null;
    
    public BackupManifest(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Profile> profiles, @org.jetbrains.annotations.NotNull()
    java.lang.String activeProfileId, @org.jetbrains.annotations.Nullable()
    java.lang.String appPinHash, @org.jetbrains.annotations.Nullable()
    java.lang.String appPinSalt) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.Profile> getProfiles() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getActiveProfileId() {
        return null;
    }
    
    /**
     * The device's app-wide protection PIN, so a fresh-install restore can
     * unlock protected chats (whose sealed content is encrypted under
     * `PBKDF2(that PIN, ChatMetadata.protectionSalt)`). Without this, the
     * salt+ciphertext survive but there's nothing to check the entered PIN
     * against, so `ChatViewModel.verifyPin` refuses every PIN. Both fields
     * are `null` on backups from devices with no app PIN set, and on legacy
     * backups from before this field existed.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAppPinHash() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAppPinSalt() {
        return null;
    }
    
    @kotlin.Suppress(names = {"unused"})
    public BackupManifest() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.Profile> component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.repository.BackupManifest copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Profile> profiles, @org.jetbrains.annotations.NotNull()
    java.lang.String activeProfileId, @org.jetbrains.annotations.Nullable()
    java.lang.String appPinHash, @org.jetbrains.annotations.Nullable()
    java.lang.String appPinSalt) {
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