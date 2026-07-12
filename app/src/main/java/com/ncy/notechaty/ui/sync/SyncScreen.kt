package com.ncy.notechaty.ui.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
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
import com.ncy.notechaty.data.sync.transport.PhoneSyncServer
import com.ncy.notechaty.data.sync.transport.SyncEndpoint
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val secureView = LocalView.current
    DisposableEffect(secureView) {
        val window = secureView.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val clipboard = LocalClipboardManager.current
    val peers by viewModel.peers.collectAsState()
    val states by viewModel.states.collectAsState()
    val discovered by viewModel.discovered.collectAsState()
    val pairingBusy by viewModel.pairingBusy.collectAsState()
    val pairingMessage by viewModel.pairingMessage.collectAsState()
    val pairingSafetyCode by viewModel.pairingSafetyCode.collectAsState()
    val hostState by viewModel.hostState.collectAsState()
    val hostingEnabled by viewModel.hostingEnabled.collectAsState()
    val hostedPeers by viewModel.hostedPeers.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }
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
    val launchQrScanner = {
        pairingError = null
        pairingSuccess = null
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Point at the pairing QR")
            setOrientationLocked(false)
            setBeepEnabled(false)
            setBarcodeImageEnabled(false)
        }
        scanLauncher.launch(options)
    }

    if (showShareDialog) {
        PairingDialog(
            state = hostState,
            onDismiss = { showShareDialog = false },
            onRegenerate = viewModel::regeneratePairing,
        )
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
                "Pair two phones directly on the same Wi-Fi, or connect to the desktop. " +
                    "Keep Ncy open on the phone sharing its code while syncing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
            Text(
                "Changes merge automatically: the latest simultaneous edit wins, deletes " +
                    "stay deleted, and protection never exposes an unresolved plaintext copy. " +
                    "Profile names remain local labels on each phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )

            NcySettingsSection("Share this phone") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "On the other phone, open Sync devices and scan this phone's code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow phone-to-phone connections")
                            Text(
                                if (hostingEnabled) "Available while Ncy is open"
                                else "Server off — no server battery use",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = hostingEnabled,
                            onCheckedChange = viewModel::setHostingEnabled,
                        )
                    }
                    Button(
                        onClick = { showShareDialog = true },
                        enabled = hostingEnabled &&
                            hostState !is PhoneSyncServer.HostState.Failed &&
                            hostState !is PhoneSyncServer.HostState.Stopped,
                        shape = NcyButtonShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Show pairing code")
                    }
                    if (hostState is PhoneSyncServer.HostState.Failed) {
                        Text(
                            (hostState as PhoneSyncServer.HostState.Failed).reason,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            NcySettingsSection("Add device") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = launchQrScanner,
                        shape = NcyButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan QR")
                    }
                    Text(
                        "or paste pairing JSON:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Pairing JSON contains a credential. It is cleared from the clipboard after a successful pair.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
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
                            val submitted = pairingText
                            val error = viewModel.pair(pairingText)
                            if (error == null) {
                                pairingText = ""
                                pairingSuccess = "Paired. Ncy will connect when in foreground."
                                pairingError = null
                                if (clipboard.getText()?.text == submitted) {
                                    clipboard.setText(AnnotatedString(""))
                                }
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
                            onScanQr = launchQrScanner,
                            onCancel = { viewModel.cancelPairing() },
                        )
                    }
                }
            }

            if (peers.isEmpty() && hostedPeers.isEmpty()) {
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
                            state = states[peer.id] ?: if (peer.disabled) {
                                SyncManager.PeerState.Disabled
                            } else SyncManager.PeerState.Idle,
                            onSyncNow = { viewModel.syncNow(peer.id) },
                            onEnabledChange = { viewModel.setPeerEnabled(peer.id, it) },
                            onUnpair = { viewModel.unpair(peer.id) }
                        )
                    }
                    hostedPeers.forEach { peer ->
                        HostedPeerCard(peer, onRemove = { viewModel.revokeHostedPeer(peer.id) })
                    }
                }
            }
        }

    }
}

@Composable
private fun PairingDialog(
    state: PhoneSyncServer.HostState,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copiedPayload by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copiedPayload) {
        val payload = copiedPayload ?: return@LaunchedEffect
        delay(PAIRING_CLIPBOARD_TTL_MS)
        if (clipboard.getText()?.text == payload) {
            clipboard.setText(AnnotatedString(""))
        }
        copiedPayload = null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair with this phone") },
        text = {
            when (state) {
                PhoneSyncServer.HostState.Disabled -> Text(
                    "Turn on phone-to-phone connections to generate a pairing code.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PhoneSyncServer.HostState.Starting,
                PhoneSyncServer.HostState.Stopped -> Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                is PhoneSyncServer.HostState.Failed -> Text(
                    state.reason,
                    color = MaterialTheme.colorScheme.error,
                )
                is PhoneSyncServer.HostState.Ready -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val qr = remember(state.pairingJson) { pairingQrBitmap(state.pairingJson) }
                    if (qr != null) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "Phone pairing QR code",
                            modifier = Modifier.size(240.dp),
                        )
                    }
                    Text(
                        "Scan this from one other device. The code refreshes after it is claimed. Both devices must be on the same Wi-Fi and this phone must keep Ncy open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "This QR and JSON contain a pairing credential. Share them only with the device you intend to pair; copied JSON clears after 60 seconds if unchanged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        if (state.usedFallbackPort) {
                            "Available at ${SyncEndpoint.display(state.host, state.port)}. Port 8765 was busy, so Ncy selected a temporary fallback port and included it in this code."
                        } else {
                            "Available at ${SyncEndpoint.display(state.host, state.port)}. This code refreshes automatically if the phone's network address changes."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(state.pairingJson))
                            copiedPayload = state.pairingJson
                        },
                        shape = NcyButtonShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy pairing JSON") }
                    TextButton(onClick = onRegenerate) {
                        Text("Make a fresh invitation code")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

private const val PAIRING_CLIPBOARD_TTL_MS = 60_000L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun pairingQrBitmap(text: String, size: Int = 768): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}.getOrNull()

@Composable
private fun HostedPeerCard(peer: PhoneSyncServer.HostedPeer, onRemove: () -> Unit) {
    NcyDividedCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Paired to this phone · incoming connection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (peer.connected) "Connected"
                else "Last connected ${DateFormat.getDateTimeInstance().format(Date(peer.lastSeenAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = if (peer.connected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (peer.legacySharedCredential) {
                Text(
                    "Legacy shared credential · remove and re-pair for independent revocation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onRemove,
                shape = NcyButtonShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Revoke")
            }
        }
    }
}

@Composable
private fun PeerCard(
    peer: SyncPeer,
    state: SyncManager.PeerState,
    onSyncNow: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
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
                SyncEndpoint.display(peer.host, peer.port),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Automatic sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = !peer.disabled,
                    onCheckedChange = onEnabledChange,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !peer.disabled,
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
    onScanQr: () -> Unit,
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
                SyncEndpoint.display(peer.host, peer.port),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                when {
                    isBusy -> "Waiting for the desktop to confirm…"
                    peer.supportsPairRequest ->
                        "Tap Pair to ask this device to add this phone, then confirm there."
                    else ->
                        "This device pairs using a QR code or JSON. Show its pairing code, then scan it here."
                },
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
                } else if (peer.supportsPairRequest) {
                    Button(onClick = onPair, shape = NcyButtonShape, enabled = !anyBusy) {
                        Text("Pair")
                    }
                } else {
                    Button(onClick = onScanQr, shape = NcyButtonShape, enabled = !anyBusy) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Scan its QR")
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
    SyncManager.PeerState.Disabled -> "Sync disabled for this device"
    SyncManager.PeerState.WaitingForNetwork -> "Waiting for Wi-Fi or Ethernet"
    SyncManager.PeerState.Connecting -> "Connecting…"
    is SyncManager.PeerState.Connected ->
        "Connected · ${state.sentCount} sent · ${state.receivedCount} received"
    is SyncManager.PeerState.Unavailable -> "Unavailable · ${state.reason}"
    is SyncManager.PeerState.Failed -> "Failed: ${state.reason}"
}
