package com.ncy.notechaty.ui.sync

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ncy.notechaty.data.sync.SyncManager
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
    var pairingText by remember { mutableStateOf("") }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var pairingSuccess by remember { mutableStateOf<String?>(null) }

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
        topBar = {
            TopAppBar(
                title = { Text("Sync devices") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Run the desktop sync server, then paste the pairing JSON it prints below. " +
                    "While Ncy is open, new messages push to every paired device on the LAN.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add device", style = MaterialTheme.typography.titleSmall)
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
                Text(
                    "Found on this network",
                    style = MaterialTheme.typography.titleSmall
                )
                unpairedDiscovered.forEach { d ->
                    DiscoveredCard(d)
                }
            }

            if (peers.isEmpty()) {
                Text(
                    "No devices paired yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Paired devices", style = MaterialTheme.typography.titleSmall)
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

@Composable
private fun PeerCard(
    peer: SyncPeer,
    state: SyncManager.PeerState,
    onSyncNow: () -> Unit,
    onUnpair: () -> Unit
) {
    Card {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onSyncNow, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync now")
                }
                OutlinedButton(onClick = onUnpair) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun DiscoveredCard(peer: DiscoveredPeer) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${peer.host}:${peer.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Reachable on this network. Scan its QR or paste its JSON above to pair.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
