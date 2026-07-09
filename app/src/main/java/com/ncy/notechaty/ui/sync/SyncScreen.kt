package com.ncy.notechaty.ui.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.ui.desktoptheme.NcyButtonShape
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.NcySectionLabel
import com.ncy.notechaty.ui.desktoptheme.NcySettingsSection
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import com.ncy.notechaty.data.sync.discovery.DiscoveredPeer
import com.ncy.notechaty.data.sync.peer.SyncPeer
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val peers by viewModel.peers.collectAsState()
    val states by viewModel.states.collectAsState()
    val discovered by viewModel.discovered.collectAsState()
    val pairingBusy by viewModel.pairingBusy.collectAsState()
    val pairingMessage by viewModel.pairingMessage.collectAsState()
    val pairingSafetyCode by viewModel.pairingSafetyCode.collectAsState()
    var pairingText by remember { mutableStateOf("") }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var pairingSuccess by remember { mutableStateOf<String?>(null) }

    // Surface discovery-initiated pair results (success / rejection / timeout)
    // via the same success/error slots the QR path already uses.
    LaunchedEffect(pairingMessage) {
        val msg = pairingMessage
        if (!msg.isNullOrBlank()) {
            if (msg.startsWith("Paired")) {
                pairingSuccess = msg
                pairingError = null
            } else {
                pairingError = msg
                pairingSuccess = null
            }
            viewModel.clearPairingMessage()
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            val error = viewModel.pair(contents)
            if (error == null) {
                pairingText = ""
                pairingSuccess = "Paired. Ncy will connect when in foreground."
                pairingError = null
            } else {
                pairingText = contents
                pairingError = error
                pairingSuccess = null
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Sync devices") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .ncyDottedGrid()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Run the desktop sync server, then paste the pairing JSON it prints below. " +
                    "While Ncy is open, new messages push to every paired device on the LAN.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )

            NcySettingsSection("Add device") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            pairingError = null
                            pairingSuccess = null
                            val options = ScanOptions().apply {
                                // Restrict to QR — also switches the viewfinder from a
                                // wide 1D-barcode rectangle to the square portrait one.
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt("Point at the desktop QR")
                                setOrientationLocked(false)
                                setBeepEnabled(false)
                                setBarcodeImageEnabled(false)
                            }
                            scanLauncher.launch(options)
                        },
                        shape = NcyButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan QR")
                    }
                    Text(
                        "or paste the JSON the desktop printed:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pairingText,
                        onValueChange = {
                            pairingText = it
                            pairingError = null
                            pairingSuccess = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pairing JSON") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        minLines = 3,
                        maxLines = 6
                    )
                    pairingError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    pairingSuccess?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = {
                            val error = viewModel.pair(pairingText)
                            if (error == null) {
                                pairingText = ""
                                pairingSuccess = "Paired. Ncy will connect when in foreground."
                                pairingError = null
                            } else {
                                pairingError = error
                                pairingSuccess = null
                            }
                        },
                        shape = NcyButtonShape,
                        enabled = pairingText.isNotBlank()
                    ) {
                        Text("Pair from JSON")
                    }
                }
            }

            // Devices broadcasting `_ncy-sync._tcp` on the LAN. Informational — pairing
            // still needs the QR/JSON since discovery doesn't carry the shared secret.
            val unpairedDiscovered = remember(discovered, peers) {
                discovered.filter { d -> peers.none { it.id == d.deviceId } }
            }
            if (unpairedDiscovered.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NcySectionLabel("Found on this network")
                    unpairedDiscovered.forEach { d ->
                        val busyForThis = pairingBusy == d.name
                        DiscoveredCard(
                            peer = d,
                            isBusy = busyForThis,
                            anyBusy = pairingBusy != null,
                            safetyCode = if (busyForThis) pairingSafetyCode else null,
                            onPair = { viewModel.pairViaDiscovery(d) },
                            onCancel = { viewModel.cancelPairing() },
                        )
                    }
                }
            }

            if (peers.isEmpty()) {
                Text(
                    "No devices paired yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NcySectionLabel("Paired devices")
                    peers.forEach { peer ->
                        PeerCard(
                            peer = peer,
                            state = states[peer.id] ?: SyncManager.PeerState.Idle,
                            onSyncNow = { viewModel.syncNow(peer.id) },
                            onUnpair = { viewModel.unpair(peer.id) }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun PeerCard(
    peer: SyncPeer,
    state: SyncManager.PeerState,
    onSyncNow: () -> Unit,
    onUnpair: () -> Unit
) {
    NcyDividedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${peer.host}:${peer.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                statusLine(state, peer),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor(state)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSyncNow,
                    shape = NcyButtonShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync now")
                }
                OutlinedButton(
                    onClick = onUnpair,
                    shape = NcyButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun DiscoveredCard(
    peer: DiscoveredPeer,
    isBusy: Boolean,
    anyBusy: Boolean,
    safetyCode: String?,
    onPair: () -> Unit,
    onCancel: () -> Unit,
) {
    NcyDividedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${peer.host}:${peer.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (isBusy) "Waiting for the desktop to confirm…"
                else "Tap Pair to ask the desktop to add this phone. Confirm on the desktop when the prompt appears.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Safety code arrives as soon as the desktop replies to the
            // PairRequest (before the desktop user has confirmed) so the
            // phone user can visually compare it against the code shown on
            // the desktop's modal in real time.
            if (isBusy && !safetyCode.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Safety code — check it matches the desktop's modal:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val formatted = if (safetyCode.length == 6) {
                    "${safetyCode.substring(0, 3)} ${safetyCode.substring(3)}"
                } else safetyCode
                Text(
                    formatted,
                    style = MaterialTheme.typography.headlineMedium,
                    letterSpacing = 6.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onCancel, shape = NcyButtonShape) {
                        Text("Cancel")
                    }
                } else {
                    Button(onClick = onPair, shape = NcyButtonShape, enabled = !anyBusy) {
                        Text("Pair")
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(state: SyncManager.PeerState) = when (state) {
    is SyncManager.PeerState.Connected -> MaterialTheme.colorScheme.primary
    is SyncManager.PeerState.Failed -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun statusLine(state: SyncManager.PeerState, peer: SyncPeer): String = when (state) {
    SyncManager.PeerState.Idle -> {
        val last = peer.lastSyncAt
        if (last != null) "Last synced ${DateFormat.getDateTimeInstance().format(Date(last))}"
        else "Not yet synced"
    }
    SyncManager.PeerState.Connecting -> "Connecting…"
    is SyncManager.PeerState.Connected ->
        "Connected · ${state.sentCount} sent · ${state.receivedCount} received"
    is SyncManager.PeerState.Failed -> "Failed: ${state.reason}"
}
