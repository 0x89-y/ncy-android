package com.ncy.notechaty.data.sync;

import android.content.Context;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Stable per-install device id. Used as the HLC nodeId and the originDevice on every event.
 * Generated lazily on first access and persisted to SharedPreferences.
 *
 * Phase 0 only stores the id. Phase 1+ will add a long-lived keypair for pairing.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 \u000e2\u00020\u0001:\u0001\u000eB\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u001b\u0010\u0005\u001a\u00020\u00068FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u0016\u0010\u000b\u001a\n \r*\u0004\u0018\u00010\f0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/ncy/notechaty/data/sync/DeviceIdentity;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "deviceId", "", "getDeviceId", "()Ljava/lang/String;", "deviceId$delegate", "Lkotlin/Lazy;", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "Companion", "app_debug"})
public final class DeviceIdentity {
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy deviceId$delegate = null;
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String PREFS = "ncy_device";
    @org.jetbrains.annotations.NotNull()
    @java.lang.Deprecated()
    public static final java.lang.String KEY_DEVICE_ID = "device_id";
    @org.jetbrains.annotations.NotNull()
    private static final com.ncy.notechaty.data.sync.DeviceIdentity.Companion Companion = null;
    
    @javax.inject.Inject()
    public DeviceIdentity(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDeviceId() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0082\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/ncy/notechaty/data/sync/DeviceIdentity$Companion;", "", "()V", "KEY_DEVICE_ID", "", "PREFS", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
}