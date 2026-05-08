package com.ncy.notechaty.ui.sync;

import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.sync.SyncManager;
import com.ncy.notechaty.data.sync.discovery.DiscoveredPeer;
import com.ncy.notechaty.data.sync.discovery.MdnsDiscovery;
import com.ncy.notechaty.data.sync.peer.PairingPayload;
import com.ncy.notechaty.data.sync.peer.SyncPeer;
import com.ncy.notechaty.data.sync.peer.SyncPeerStore;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u0010\u0017\u001a\u00020\u0018H\u0014J\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u00142\u0006\u0010\u001a\u001a\u00020\u0014J\u000e\u0010\u001b\u001a\u00020\u00182\u0006\u0010\u001c\u001a\u00020\u0014J\u000e\u0010\u001d\u001a\u00020\u00182\u0006\u0010\u001c\u001a\u00020\u0014R\u001d\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000eR#\u0010\u0012\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00150\u00130\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u000eR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lcom/ncy/notechaty/ui/sync/SyncViewModel;", "Landroidx/lifecycle/ViewModel;", "peerStore", "Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;", "syncManager", "Lcom/ncy/notechaty/data/sync/SyncManager;", "mdnsDiscovery", "Lcom/ncy/notechaty/data/sync/discovery/MdnsDiscovery;", "(Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;Lcom/ncy/notechaty/data/sync/SyncManager;Lcom/ncy/notechaty/data/sync/discovery/MdnsDiscovery;)V", "discovered", "Lkotlinx/coroutines/flow/StateFlow;", "", "Lcom/ncy/notechaty/data/sync/discovery/DiscoveredPeer;", "getDiscovered", "()Lkotlinx/coroutines/flow/StateFlow;", "peers", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "getPeers", "states", "", "", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "getStates", "onCleared", "", "pair", "rawText", "syncNow", "peerId", "unpair", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SyncViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.peer.SyncPeerStore peerStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.SyncManager syncManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.discovery.MdnsDiscovery mdnsDiscovery = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.peer.SyncPeer>> peers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerState>> states = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.discovery.DiscoveredPeer>> discovered = null;
    
    @javax.inject.Inject()
    public SyncViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeerStore peerStore, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.SyncManager syncManager, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.discovery.MdnsDiscovery mdnsDiscovery) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.peer.SyncPeer>> getPeers() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerState>> getStates() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.discovery.DiscoveredPeer>> getDiscovered() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    /**
     * Returns null on success, an error message on failure.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String pair(@org.jetbrains.annotations.NotNull()
    java.lang.String rawText) {
        return null;
    }
    
    public final void unpair(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
    }
    
    public final void syncNow(@org.jetbrains.annotations.NotNull()
    java.lang.String peerId) {
    }
}