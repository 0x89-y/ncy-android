package com.ncy.notechaty;

import android.app.Application;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import com.ncy.notechaty.backup.BackupScheduler;
import com.ncy.notechaty.data.repository.ProfileRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.data.sync.DeviceIdentity;
import com.ncy.notechaty.data.sync.event.EventLogMigrator;
import com.ncy.notechaty.migration.ProfileMigration;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;

@dagger.hilt.android.HiltAndroidApp()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\b\u0010 \u001a\u00020!H\u0016R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u0014\u0010\u0016\u001a\u00020\u00178VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\u0019R\u001e\u0010\u001a\u001a\u00020\u001b8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001c\u0010\u001d\"\u0004\b\u001e\u0010\u001f\u00a8\u0006\""}, d2 = {"Lcom/ncy/notechaty/NcyApp;", "Landroid/app/Application;", "Landroidx/work/Configuration$Provider;", "()V", "deviceIdentity", "Lcom/ncy/notechaty/data/sync/DeviceIdentity;", "getDeviceIdentity", "()Lcom/ncy/notechaty/data/sync/DeviceIdentity;", "setDeviceIdentity", "(Lcom/ncy/notechaty/data/sync/DeviceIdentity;)V", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "getProfileRepository", "()Lcom/ncy/notechaty/data/repository/ProfileRepository;", "setProfileRepository", "(Lcom/ncy/notechaty/data/repository/ProfileRepository;)V", "settingsRepository", "Lcom/ncy/notechaty/data/repository/SettingsRepository;", "getSettingsRepository", "()Lcom/ncy/notechaty/data/repository/SettingsRepository;", "setSettingsRepository", "(Lcom/ncy/notechaty/data/repository/SettingsRepository;)V", "workManagerConfiguration", "Landroidx/work/Configuration;", "getWorkManagerConfiguration", "()Landroidx/work/Configuration;", "workerFactory", "Landroidx/hilt/work/HiltWorkerFactory;", "getWorkerFactory", "()Landroidx/hilt/work/HiltWorkerFactory;", "setWorkerFactory", "(Landroidx/hilt/work/HiltWorkerFactory;)V", "onCreate", "", "app_debug"})
public final class NcyApp extends android.app.Application implements androidx.work.Configuration.Provider {
    @javax.inject.Inject()
    public androidx.hilt.work.HiltWorkerFactory workerFactory;
    @javax.inject.Inject()
    public com.ncy.notechaty.data.repository.SettingsRepository settingsRepository;
    @javax.inject.Inject()
    public com.ncy.notechaty.data.repository.ProfileRepository profileRepository;
    @javax.inject.Inject()
    public com.ncy.notechaty.data.sync.DeviceIdentity deviceIdentity;
    
    public NcyApp() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.hilt.work.HiltWorkerFactory getWorkerFactory() {
        return null;
    }
    
    public final void setWorkerFactory(@org.jetbrains.annotations.NotNull()
    androidx.hilt.work.HiltWorkerFactory p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.repository.SettingsRepository getSettingsRepository() {
        return null;
    }
    
    public final void setSettingsRepository(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.SettingsRepository p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.repository.ProfileRepository getProfileRepository() {
        return null;
    }
    
    public final void setProfileRepository(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.sync.DeviceIdentity getDeviceIdentity() {
        return null;
    }
    
    public final void setDeviceIdentity(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.DeviceIdentity p0) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.work.Configuration getWorkManagerConfiguration() {
        return null;
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
}