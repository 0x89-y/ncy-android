package com.ncy.notechaty.ui.sync;

import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.input.KeyboardCapitalization;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.ncy.notechaty.data.sync.SyncManager;
import com.ncy.notechaty.data.sync.peer.SyncPeer;
import java.text.DateFormat;
import java.util.Date;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\u001a4\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a \u0010\t\u001a\u00020\u00012\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\b\b\u0002\u0010\u000b\u001a\u00020\fH\u0007\u001a\u0015\u0010\r\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u0005H\u0003\u00a2\u0006\u0002\u0010\u000f\u001a\u0018\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0002\u001a\u00020\u0003H\u0002\u00a8\u0006\u0012"}, d2 = {"PeerCard", "", "peer", "Lcom/ncy/notechaty/data/sync/peer/SyncPeer;", "state", "Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;", "onSyncNow", "Lkotlin/Function0;", "onUnpair", "SyncScreen", "onNavigateBack", "viewModel", "Lcom/ncy/notechaty/ui/sync/SyncViewModel;", "statusColor", "Landroidx/compose/ui/graphics/Color;", "(Lcom/ncy/notechaty/data/sync/SyncManager$PeerState;)J", "statusLine", "", "app_debug"})
public final class SyncScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void SyncScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateBack, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.ui.sync.SyncViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PeerCard(com.ncy.notechaty.data.sync.peer.SyncPeer peer, com.ncy.notechaty.data.sync.SyncManager.PeerState state, kotlin.jvm.functions.Function0<kotlin.Unit> onSyncNow, kotlin.jvm.functions.Function0<kotlin.Unit> onUnpair) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final long statusColor(com.ncy.notechaty.data.sync.SyncManager.PeerState state) {
        return 0L;
    }
    
    private static final java.lang.String statusLine(com.ncy.notechaty.data.sync.SyncManager.PeerState state, com.ncy.notechaty.data.sync.peer.SyncPeer peer) {
        return null;
    }
}