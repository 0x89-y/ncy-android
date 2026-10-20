package com.ncy.notechaty.ui.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ncy.notechaty.data.model.SyncTransportMode
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.DeviceNameDisambiguation
import com.ncy.notechaty.data.sync.SyncFailurePolicy
import com.ncy.notechaty.ui.desktoptheme.NcyButtonShape
import com.ncy.notechaty.ui.desktoptheme.NcySettingsToggleRow
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.NcySettingsSection
import com.ncy.notechaty.ui.desktoptheme.NcySettingsRow
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import com.ncy.notechaty.data.sync.discovery.DiscoveredPeer
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.PairingPayload
import com.ncy.notechaty.data.sync.pear.PearRuntimeStatus
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
    val pairingCodeConfirmed by viewModel.pairingCodeConfirmed.collectAsState()
    val pairingDesktopAccepted by viewModel.pairingDesktopAccepted.collectAsState()
    val hostState by viewModel.hostState.collectAsState()
    val hostingEnabled by viewModel.hostingEnabled.collectAsState()
    val hostedPeers by viewModel.hostedPeers.collectAsState()
    val pendingPairing by viewModel.pendingPairing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pearStatus by viewModel.pearStatus.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var pairingText by remember { mutableStateOf("") }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var pairingSuccess by remember { mutableStateOf<String?>(null) }
    var showAdvancedPairing by rememberSaveable { mutableStateOf(false) }
    var showTransportModeDialog by remember { mutableStateOf(false) }
    var showFileLimitDialog by remember { mutableStateOf(false) }
    var pendingPearMode by remember { mutableStateOf<SyncTransportMode?>(null) }

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
                pairingSuccess = pairingImportMessage(contents)
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
            pearEnabled = settings.syncTransportMode.usesPear,
            pearStatus = pearStatus,
            onDismiss = { showShareDialog = false },
            onRegenerate = viewModel::regeneratePairing,
        )
    }

    if (showInfoDialog) {
        NcyAlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About device sync") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pair phones and desktops using a QR code or pairing JSON. " +
                            "Keep Ncy open on a phone while it is sharing a pairing code."
                    )
                    Text(
                        "Local connections use Wi-Fi or Ethernet. Public and guest Wi-Fi may " +
                            "isolate devices even when they show the same network name. Pear-capable " +
                            "connection modes can reach already-paired devices across networks."
                    )
                    Text(
                        "Ncy never scans the subnet or discovers unknown devices through Pear. " +
                            "Changes merge automatically: the latest simultaneous edit wins, " +
                            "deletes stay deleted, and protected content remains encrypted."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Done") }
            },
        )
    }

    if (showTransportModeDialog) {
        NcyAlertDialog(
            onDismissRequest = { showTransportModeDialog = false },
            title = { Text("Connection mode") },
            text = {
                Column {
                    SyncTransportMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (mode.usesPear && !settings.syncTransportMode.usesPear) {
                                    pendingPearMode = mode
                                } else {
                                    viewModel.setSyncTransportMode(mode)
                                }
                                showTransportModeDialog = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.syncTransportMode == mode, onClick = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(mode.displayName)
                                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTransportModeDialog = false }) { Text("Cancel") } },
        )
    }

    if (showFileLimitDialog) {
        var customMiB by rememberSaveable(settings.automaticFileDownloadLimitBytes) {
            mutableStateOf((settings.automaticFileDownloadLimitBytes / (1024 * 1024)).coerceAtLeast(1).toString())
        }
        val presets = listOf(0L to "Always ask", 25L to "25 MB", 100L to "100 MB", 250L to "250 MB", 500L to "500 MB", 1024L to "1 GB")
        NcyAlertDialog(
            onDismissRequest = { showFileLimitDialog = false },
            title = { Text("Automatic file downloads") },
            text = {
                Column {
                    presets.forEach { (mib, label) ->
                        val bytes = mib * 1024 * 1024
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setAutomaticFileDownloadLimit(bytes)
                                showFileLimitDialog = false
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.automaticFileDownloadLimitBytes == bytes, onClick = null)
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    OutlinedTextField(
                        value = customMiB,
                        onValueChange = { customMiB = it.filter(Char::isDigit).take(4) },
                        label = { Text("Custom limit (MB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = customMiB.toLongOrNull() in 1L..1024L,
                    onClick = {
                        val mib = customMiB.toLongOrNull() ?: return@TextButton
                        viewModel.setAutomaticFileDownloadLimit(mib * 1024 * 1024)
                        showFileLimitDialog = false
                    },
                ) { Text("Use custom") }
            },
            dismissButton = { TextButton(onClick = { showFileLimitDialog = false }) { Text("Cancel") } },
        )
    }

    pendingPearMode?.let { selectedMode ->
        NcyAlertDialog(
            onDismissRequest = { pendingPearMode = null },
            title = { Text("Enable internet sync?") },
            text = {
                Text(
                    "Pear contacts public discovery infrastructure so your paired devices can " +
                        "find each other outside the local network. Operators may observe IP " +
                        "addresses, timing, and traffic volume, but Ncy chat data remains " +
                        "end-to-end encrypted. No chat history is stored there."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSyncTransportMode(selectedMode)
                    pendingPearMode = null
                }) { Text("Use ${selectedMode.displayName}") }
            },
            dismissButton = { TextButton(onClick = { pendingPearMode = null }) { Text("Cancel") } },
        )
    }
    pendingPairing?.let { request ->
        NcyAlertDialog(
            onDismissRequest = {},
            title = { Text("Allow this device?") },
            text = {
                Text(
                    "${request.deviceName} scanned this phone's pairing code. Allowing it will begin encrypted chat synchronization."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::approvePendingPairing) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::rejectPendingPairing) { Text("Reject") }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "About device sync")
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
            val unpairedDiscovered = discovered.filter { found ->
                peers.none { it.id == found.deviceId } && hostedPeers.none { it.id == found.deviceId }
            }
            val duplicateDeviceNames = DeviceNameDisambiguation.duplicateNameKeys(
                peers.map { it.name to it.id } + hostedPeers.map { it.name to it.id }
            )

            NcySettingsSection("Add device") {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (unpairedDiscovered.isNotEmpty()) {
                        Text(if (unpairedDiscovered.size == 1) "Nearby device" else "Nearby devices", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        unpairedDiscovered.forEach { found ->
                            val busyForThis = pairingBusy == found.name
                            DiscoveredCard(
                                peer = found,
                                isBusy = busyForThis,
                                anyBusy = pairingBusy != null,
                                safetyCode = if (busyForThis) pairingSafetyCode else null,
                                codeConfirmed = busyForThis && pairingCodeConfirmed,
                                desktopAccepted = busyForThis && pairingDesktopAccepted,
                                onPair = { viewModel.pairViaDiscovery(found) },
                                onScanQr = launchQrScanner,
                                onConfirmCode = viewModel::confirmPairingCode,
                                onCancel = viewModel::cancelPairing,
                            )
                        }
                        HorizontalDivider()
                    }
                    Button(onClick = launchQrScanner, shape = NcyButtonShape, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan QR")
                    }
                    pairingError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    pairingSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { showAdvancedPairing = !showAdvancedPairing }) {
                        Text(if (showAdvancedPairing) "Hide advanced pairing options" else "Advanced pairing options")
                    }
                    if (showAdvancedPairing) {
                        Text("Pairing JSON contains a credential. It is cleared from the clipboard after a successful pair.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        OutlinedTextField(
                            value = pairingText,
                            onValueChange = { pairingText = it; pairingError = null; pairingSuccess = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Pairing JSON") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            minLines = 3,
                            maxLines = 6,
                        )
                        OutlinedButton(
                            onClick = {
                                val submitted = pairingText
                                val error = viewModel.pair(submitted)
                                if (error == null) {
                                    pairingText = ""
                                    pairingSuccess = pairingImportMessage(submitted)
                                    pairingError = null
                                    if (clipboard.getText()?.text == submitted) clipboard.setText(AnnotatedString(""))
                                } else {
                                    pairingError = error
                                    pairingSuccess = null
                                }
                            },
                            shape = NcyButtonShape,
                            enabled = pairingText.isNotBlank(),
                        ) { Text("Pair from JSON") }
                    }
                }
            }

            NcySettingsSection("Share this phone") {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("On the other device, open Sync and scan this phone's code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Allow phone-to-phone connections")
                            Text(if (hostingEnabled) "Available while Ncy is open" else "Server off — no server battery use", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hostingEnabled, onCheckedChange = viewModel::setHostingEnabled)
                    }
                    Button(
                        onClick = { showShareDialog = true },
                        enabled = hostingEnabled && hostState !is PhoneSyncServer.HostState.Failed && hostState !is PhoneSyncServer.HostState.Stopped,
                        shape = NcyButtonShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Show pairing code")
                    }
                    if (hostState is PhoneSyncServer.HostState.Failed) Text((hostState as PhoneSyncServer.HostState.Failed).reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            NcySettingsSection("Paired devices") {
                if (peers.isEmpty() && hostedPeers.isEmpty()) {
                    Text("No devices paired yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                } else {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        peers.forEachIndexed { index, peer ->
                            if (index > 0) HorizontalDivider()
                            PeerCard(
                                peer = peer,
                                state = states[peer.id] ?: if (peer.disabled) SyncManager.PeerState.Disabled else SyncManager.PeerState.Idle,
                                onSyncNow = { viewModel.syncNow(peer.id) },
                                onEnabledChange = { viewModel.setPeerEnabled(peer.id, it) },
                                onUnpair = { viewModel.unpair(peer.id) },
                                transportPolicy = settings.syncTransportMode.displayName,
                                showFingerprint = DeviceNameDisambiguation.isDuplicate(peer.name, duplicateDeviceNames),
                            )
                        }
                        hostedPeers.forEachIndexed { index, peer ->
                            if (peers.isNotEmpty() || index > 0) HorizontalDivider()
                            HostedPeerCard(peer, showFingerprint = DeviceNameDisambiguation.isDuplicate(peer.name, duplicateDeviceNames), onRemove = { viewModel.revokeHostedPeer(peer.id) })
                        }
                    }
                }
            }

            NcySettingsSection("Sync settings") {
                NcySettingsRow(icon = Icons.Default.Public, title = "Connection mode", subtitle = settings.syncTransportMode.displayName, onClick = { showTransportModeDialog = true })
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = "Automatic file downloads",
                    subtitle = if (settings.automaticFileDownloadLimitBytes == 0L) "Always ask" else "Up to ${formatSyncFileSize(settings.automaticFileDownloadLimitBytes)}",
                    onClick = { showFileLimitDialog = true },
                )
                NcyRowDivider()
                // Pairing into an established mesh replays the whole history at
                // once. Without a cutoff this device would try to pull every
                // attachment ever sent, over whatever connection it has.
                NcySettingsToggleRow(
                    icon = Icons.Default.History,
                    title = "Download media from before pairing",
                    subtitle = if (settings.mediaBackfillCutoffMs == 0L) {
                        "Fetching everything, including media sent before this device was paired"
                    } else {
                        "Older media is listed with a Download button instead"
                    },
                    checked = settings.mediaBackfillCutoffMs == 0L,
                    onCheckedChange = { everything -> viewModel.setMediaBackfill(everything) },
                )
            }        }

    }
}

@Composable
private fun PairingDialog(
    state: PhoneSyncServer.HostState,
    pearEnabled: Boolean,
    pearStatus: PearRuntimeStatus,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copiedPayload by remember { mutableStateOf<String?>(null) }
    var showAdvancedSharing by rememberSaveable { mutableStateOf(false) }
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
                    val payloadHasPear = remember(state.pairingJson) {
                        PairingPayload.parse(state.pairingJson)?.pearPublicKey != null
                    }
                    val qr = remember(state.pairingJson) { pairingQrBitmap(state.pairingJson) }
                    if (qr != null && (!pearEnabled || payloadHasPear)) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "Phone pairing QR code",
                            modifier = Modifier.size(240.dp),
                        )
                    } else if (pearEnabled && pearStatus is PearRuntimeStatus.Starting) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                    Text(
                        when {
                            payloadHasPear -> "Pear internet key included. Scan this from the other device; LAN will still be preferred."
                            pearEnabled && pearStatus is PearRuntimeStatus.Starting -> "Pear is still starting. Wait for the internet key before scanning."
                            pearEnabled && pearStatus is PearRuntimeStatus.Failed -> "Pear failed to start: ${pearStatus.reason}. This code is LAN-only."
                            pearEnabled -> "Pear internet key is not available yet. This code is LAN-only."
                            else -> "Pear is disabled. This code supports LAN sync only."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (payloadHasPear || !pearEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "This QR contains a pairing credential. Share it only with the device you intend to pair.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        if (!state.lanAvailable) {
                            "Available through Pear. This code contains the exact Pear key and pairing credential; it does not discover nearby or unknown devices."
                        } else if (state.usedFallbackPort) {
                            "Available at ${SyncEndpoint.display(state.host, state.port)}. Port 8765 was busy, so Ncy selected a temporary fallback port and included it in this code."
                        } else {
                            "Available at ${SyncEndpoint.display(state.host, state.port)}. This code refreshes automatically if the phone's network address changes."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showAdvancedSharing = !showAdvancedSharing }) {
                        Text(if (showAdvancedSharing) "Hide advanced sharing options" else "Advanced sharing options")
                    }
                    if (showAdvancedSharing) {
                        Text(
                            "Pairing JSON contains the same credential as the QR. Copied JSON clears after 60 seconds if unchanged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(state.pairingJson))
                                copiedPayload = state.pairingJson
                            },
                            shape = NcyButtonShape,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !pearEnabled || payloadHasPear,
                        ) { Text("Copy pairing JSON") }
                        TextButton(onClick = onRegenerate) { Text("Replace unused invitation") }
                        Text(
                            "This replaces only the unclaimed QR/JSON above. Already paired devices keep their individual credentials; remove those from Paired devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

private fun formatSyncFileSize(bytes: Long): String =
    if (bytes >= 1024L * 1024 * 1024) "1 GB" else "${bytes / (1024 * 1024)} MB"

private const val PAIRING_CLIPBOARD_TTL_MS = 60_000L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun pairingImportMessage(raw: String): String =
    if (PairingPayload.parse(raw)?.availability == PairingPayload.AVAILABILITY_FOREGROUND) {
        "Pairing saved. Approve the request on the sharing phone."
    } else {
        "Pairing saved. Ncy will connect when the device is available."
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
private fun HostedPeerCard(
    peer: PhoneSyncServer.HostedPeer,
    showFingerprint: Boolean,
    onRemove: () -> Unit,
) {
    var confirmRevoke by remember { mutableStateOf(false) }
    NcyDividedCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            if (showFingerprint) {
                Text(
                    "Device ID ${DeviceNameDisambiguation.shortFingerprint(peer.id)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                onClick = { confirmRevoke = true },
                shape = NcyButtonShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Revoke & require re-pair")
            }
        }
    }
    if (confirmRevoke) {
        NcyAlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = { Text("Revoke ${peer.name}?") },
            text = {
                Text(
                    "This device disconnects and its credential stops working. Chats remain unchanged, but the device must pair again."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRevoke = false
                        onRemove()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PeerCard(
    peer: SyncPeer,
    state: SyncManager.PeerState,
    onSyncNow: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onUnpair: () -> Unit,
    transportPolicy: String,
    showFingerprint: Boolean,
) {
    var showDiagnostics by remember { mutableStateOf(false) }
    NcyDividedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            if (showFingerprint) {
                Text(
                    "Device ID ${DeviceNameDisambiguation.shortFingerprint(peer.id)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
            TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
                Text(if (showDiagnostics) "Hide diagnostics" else "Show diagnostics")
            }
            if (showDiagnostics) {
                val formatter = DateFormat.getDateTimeInstance()
                val discovered = peer.lastDiscoveredAt?.let { formatter.format(Date(it)) } ?: "Never"
                val authenticated = peer.lastAuthenticatedAt?.let { formatter.format(Date(it)) } ?: "Never"
                val authenticatedEndpoint = if (
                    peer.lastAuthenticatedHost != null && peer.lastAuthenticatedPort != null
                ) SyncEndpoint.display(peer.lastAuthenticatedHost, peer.lastAuthenticatedPort) else "None"
                val retry = when {
                    peer.disabled -> "Paused by user"
                    state is SyncManager.PeerState.Connected -> "Not retrying"
                    state is SyncManager.PeerState.WaitingForNetwork -> "Waiting to connect"
                    peer.nextRetryAt != null -> "Scheduled ${formatter.format(Date(peer.nextRetryAt))}"
                    state is SyncManager.PeerState.Failed &&
                        (state.reason == SyncFailurePolicy.REPAIR_MESSAGE ||
                            state.reason == SyncFailurePolicy.REJECTED_MESSAGE) -> "Stopped until peer is removed"
                    else -> "No retry scheduled"
                }
                val lan = when {
                    state is SyncManager.PeerState.Connected -> "Reachable"
                    state is SyncManager.PeerState.Failed || state is SyncManager.PeerState.Unavailable ->
                        "Possibly blocked; an offline peer looks the same"
                    else -> "Not currently tested"
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Last discovery: $discovered", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Last authenticated: $authenticated at $authenticatedEndpoint",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("Retry: $retry", style = MaterialTheme.typography.bodySmall)
                    Text("Configured policy: $transportPolicy", style = MaterialTheme.typography.bodySmall)
                    Text("Direct LAN: $lan", style = MaterialTheme.typography.bodySmall)
                    if (state is SyncManager.PeerState.Connected) {
                        Text(
                            "Active transport: ${state.transport?.name ?: "Negotiating"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "Pear pairing key: ${if (peer.pearPublicKey != null) "Included" else "Not included"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    peer.lastError?.let {
                        Text(
                            "Last error: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (state is SyncManager.PeerState.Failed &&
                (state.reason == SyncFailurePolicy.REPAIR_MESSAGE ||
                    state.reason == SyncFailurePolicy.REJECTED_MESSAGE)
            ) {
                Text(
                    "Automatic retries are stopped because this is a trust failure, not a network outage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (state is SyncManager.PeerState.Failed &&
                state.reason == SyncFailurePolicy.UNVERIFIED_REJECTION_MESSAGE
            ) {
                Text(
                    "The refusal came before the connection was encrypted, so it can't be confirmed " +
                        "as coming from your device. Retries continue in case it was spoofed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (state is SyncManager.PeerState.Unavailable || state is SyncManager.PeerState.Failed) {
                Text(
                    "If both devices are open, this network may block direct device traffic. " +
                        "Try a private Wi-Fi network or hotspot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    codeConfirmed: Boolean,
    desktopAccepted: Boolean,
    onPair: () -> Unit,
    onScanQr: () -> Unit,
    onConfirmCode: () -> Unit,
    onCancel: () -> Unit,
) {
    val awaitingCodeCheck = isBusy && !safetyCode.isNullOrBlank() && !codeConfirmed
    NcyDividedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Private discovery label · the friendly device name appears after pairing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                SyncEndpoint.display(peer.host, peer.port),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                when {
                    awaitingCodeCheck && desktopAccepted ->
                        "The desktop accepted. Tap Codes match only if both codes are identical."
                    awaitingCodeCheck ->
                        "Compare this code with the desktop's, then tap Codes match. Cancel if they differ."
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
                    if (!awaitingCodeCheck || !desktopAccepted) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedButton(onClick = onCancel, shape = NcyButtonShape) {
                        Text("Cancel")
                    }
                    if (awaitingCodeCheck) {
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onConfirmCode, shape = NcyButtonShape) {
                            Text("Codes match")
                        }
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
        if (last != null) "Last synchronized ${DateFormat.getDateTimeInstance().format(Date(last))}"
        else "Waiting to connect"
    }
    SyncManager.PeerState.Disabled -> "Sync disabled for this device"
    SyncManager.PeerState.WaitingForNetwork -> "Waiting to connect"
    SyncManager.PeerState.Connecting -> "Waiting to connect"
    is SyncManager.PeerState.Connected ->
        "${if (state.transport?.name.equals("PEAR", ignoreCase = true)) "Connected via Pear" else "Connected locally"} · ${state.sentCount} sent · ${state.receivedCount} received"
    is SyncManager.PeerState.Unavailable -> "Unavailable · ${state.reason}"
    is SyncManager.PeerState.Failed -> "Failed: ${state.reason}"
}
