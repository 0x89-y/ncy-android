package com.ncy.notechaty.data.sync.discovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\u0010\u0010\u0018\u001a\u00020\u00152\u0006\u0010\u0019\u001a\u00020\u0017H\u0002J\u0006\u0010\u001a\u001a\u00020\u0015J\u0006\u0010\u001b\u001a\u00020\u0015R\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0012X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/ncy/notechaty/data/sync/discovery/MdnsDiscovery;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_peers", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/ncy/notechaty/data/sync/discovery/DiscoveredPeer;", "listener", "Landroid/net/nsd/NsdManager$DiscoveryListener;", "nsd", "Landroid/net/nsd/NsdManager;", "peers", "Lkotlinx/coroutines/flow/StateFlow;", "getPeers", "()Lkotlinx/coroutines/flow/StateFlow;", "serviceType", "", "tag", "onResolveResult", "", "info", "Landroid/net/nsd/NsdServiceInfo;", "resolve", "service", "start", "stop", "app_debug"})
public final class MdnsDiscovery {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "MdnsDiscovery";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String serviceType = "_ncy-sync._tcp.";
    @org.jetbrains.annotations.NotNull()
    private final android.net.nsd.NsdManager nsd = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ncy.notechaty.data.sync.discovery.DiscoveredPeer>> _peers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.discovery.DiscoveredPeer>> peers = null;
    @org.jetbrains.annotations.Nullable()
    private android.net.nsd.NsdManager.DiscoveryListener listener;
    
    @javax.inject.Inject()
    public MdnsDiscovery(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.discovery.DiscoveredPeer>> getPeers() {
        return null;
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void start() {
    }
    
    @kotlin.jvm.Synchronized()
    public final synchronized void stop() {
    }
    
    private final void resolve(android.net.nsd.NsdServiceInfo service) {
    }
    
    private final void onResolveResult(android.net.nsd.NsdServiceInfo info) {
    }
}