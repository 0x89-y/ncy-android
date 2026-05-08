package com.ncy.notechaty.ui.sync;

import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.sync.SyncManager;
import com.ncy.notechaty.data.sync.peer.PairingPayload;
import com.ncy.notechaty.data.sync.peer.SyncPeer;
import com.ncy.notechaty.data.sync.peer.SyncPeerStore;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u0013\u001a\u00020\u000fJ\u000e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000fJ\u000e\u0010\u0017\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR#\u0010\r\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00100\u000e0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/ncy/notechaty/ui/sync/SyncViewModel;", "Landroidx/lifecycle/ViewModel;", "peerStore", "Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;", "syncManager", "Lcom/ncy/notechaty/data/sync/SyncManager;", "(Lcom/ncy/notechaty/data/sync/peer/SyncPeerStore;Lcom/ncy/notechaty/data/sync/SyncManager;)V", "peers", "Lkotlinx/coroutines/flow/StateFlow;", "", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "getPeers", "()Lkotlinx/coroutines/flow/StateFlow;", "states", "", "", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "getStates", "pair", "rawText", "syncNow", "", "peerId", "unpair", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SyncViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.peer.SyncPeerStore peerStore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.sync.SyncManager syncManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.sync.peer.SyncPeer>> peers = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.Map<java.lang.String, com.ncy.notechaty.data.sync.SyncManager.PeerState>> states = null;
    
    @javax.inject.Inject()
    public SyncViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.peer.SyncPeerStore peerStore, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.sync.SyncManager syncManager) {
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