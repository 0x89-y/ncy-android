package com.ncy.notechaty.ui.sync

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
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
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.DeviceNameDisambiguation
import com.ncy.notechaty.data.sync.SyncFailurePolicy
import com.ncy.notechaty.ui.desktoptheme.NcyButtonShape
import com.ncy.notechaty.ui.desktoptheme.NcySettingsToggleRow
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyFlushList
import com.ncy.notechaty.ui.desktoptheme.NcySettingsSection
import com.ncy.notechaty.ui.desktoptheme.NcySettingsRow
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import com.ncy.notechaty.data.sync.discovery.DiscoveredPeer
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.PairingPayload
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val hostState by viewModel.hostState.collectAsState()
    val hostingEnabled by viewModel.hostingEnabled.collectAsState()
    val hostedPeers by viewModel.hostedPeers.collectAsState()
    val pendingPairing by viewModel.pendingPairing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var pairingText by remember { mutableStateOf("") }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var pairingSuccess by remember { mutableStateOf<String?>(null) }
    var showAdvancedPairing by rememberSaveable { mutableStateOf(false) }
    var showFileLimitDialog by remember { mutableStateOf(false) }

    // Surface discovery-initiated pair results (success / rejection / timeout)
    // via the same success/error slots the QR path already uses.
    LaunchedEffect(pairingMessage) {
        val msg = pairingMessage
        if (msg != null && msg.text.isNotBlank()) {
            if (msg.success) {
                pairingSuccess = msg.text
                pairingError = null
            } else {
                pairingError = msg.text
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
                pairingSuccess = pairingImportMessage(context, contents)
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
            setPrompt(context.getString(R.string.point_at_the_pairing_qr))
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

    if (showInfoDialog) {
        NcyAlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.about_device_sync)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.pair_phones_and_computers_by_scanning_a)
                    )
                    Text(
                        stringResource(R.string.on_the_same_wi_fi_devices_talk_to_each)
                    )
                    Text(
                        stringResource(R.string.ncy_only_talks_to_devices_you_paired)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text(stringResource(R.string.done)) }
            },
        )
    }

    if (showFileLimitDialog) {
        var customMiB by rememberSaveable(settings.automaticFileDownloadLimitBytes) {
            mutableStateOf((settings.automaticFileDownloadLimitBytes / (1024 * 1024)).coerceAtLeast(1).toString())
        }
        val presets = listOf(0L to stringResource(R.string.always_ask), 25L to "25 MB", 100L to "100 MB", 250L to "250 MB", 500L to "500 MB", 1024L to "1 GB")
        NcyAlertDialog(
            onDismissRequest = { showFileLimitDialog = false },
            title = { Text(stringResource(R.string.automatic_file_downloads)) },
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
                        label = { Text(stringResource(R.string.custom_limit_mb)) },
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
                ) { Text(stringResource(R.string.use_custom)) }
            },
            dismissButton = { TextButton(onClick = { showFileLimitDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    pendingPairing?.let { request ->
        NcyAlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.allow_this_device)) },
            text = {
                Text(
                    stringResource(R.string.scanned_this_phone_s_code_allow_it_to, request.deviceName)
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::approvePendingPairing) { Text(stringResource(R.string.allow)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::rejectPendingPairing) { Text(stringResource(R.string.reject)) }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.about_device_sync))
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
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val unpairedDiscovered = discovered.filter { found ->
                peers.none { it.id == found.deviceId } && hostedPeers.none { it.id == found.deviceId }
            }
            val duplicateDeviceNames = DeviceNameDisambiguation.duplicateNameKeys(
                peers.map { it.name to it.id } + hostedPeers.map { it.name to it.id }
            )

            NcySettingsSection(stringResource(R.string.add_device)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (unpairedDiscovered.isNotEmpty()) {
                        Text(if (unpairedDiscovered.size == 1) stringResource(R.string.nearby_device) else stringResource(R.string.nearby_devices), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.scan_qr))
                    }
                    pairingError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    pairingSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { showAdvancedPairing = !showAdvancedPairing }) {
                        Text(if (showAdvancedPairing) stringResource(R.string.hide_pairing_text) else stringResource(R.string.pair_without_the_camera))
                    }
                    if (showAdvancedPairing) {
                        Text(stringResource(R.string.pairing_text_works_like_a_qr_code_it_is), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        OutlinedTextField(
                            value = pairingText,
                            onValueChange = { pairingText = it; pairingError = null; pairingSuccess = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.pairing_text)) },
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
                                    pairingSuccess = pairingImportMessage(context, submitted)
                                    pairingError = null
                                    if (clipboard.getText()?.text == submitted) clipboard.setText(AnnotatedString(""))
                                } else {
                                    pairingError = error
                                    pairingSuccess = null
                                }
                            },
                            shape = NcyButtonShape,
                            enabled = pairingText.isNotBlank(),
                        ) { Text(stringResource(R.string.pair_from_text)) }
                    }
                }
            }

            NcySettingsSection(stringResource(R.string.share_this_phone)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.on_the_other_device_open_sync_and_scan), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.let_other_devices_connect))
                            Text(if (hostingEnabled) stringResource(R.string.while_ncy_is_open) else stringResource(R.string.off_so_it_uses_no_battery), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.show_pairing_code))
                    }
                    if (hostState is PhoneSyncServer.HostState.Failed) Text((hostState as PhoneSyncServer.HostState.Failed).reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            NcySettingsSection(stringResource(R.string.paired_devices)) {
                if (peers.isEmpty() && hostedPeers.isEmpty()) {
                    Text(stringResource(R.string.no_devices_paired_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
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

            NcySettingsSection(stringResource(R.string.sync_settings)) {
                NcySettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = stringResource(R.string.automatic_file_downloads),
                    subtitle = if (settings.automaticFileDownloadLimitBytes == 0L) stringResource(R.string.always_ask) else stringResource(R.string.up_to, formatSyncFileSize(settings.automaticFileDownloadLimitBytes)),
                    onClick = { showFileLimitDialog = true },
                )
                NcyRowDivider()
                // Pairing into an established mesh replays the whole history at
                // once. Without a cutoff this device would try to pull every
                // attachment ever sent, over whatever connection it has.
                NcySettingsToggleRow(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.download_media_from_before_pairing),
                    subtitle = if (settings.mediaBackfillCutoffMs == 0L) {
                        stringResource(R.string.fetching_everything_including_media_sent)
                    } else {
                        stringResource(R.string.older_media_is_listed_with_a_download)
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
        title = { Text(stringResource(R.string.pair_with_this_phone)) },
        text = {
            when (state) {
                PhoneSyncServer.HostState.Disabled -> Text(
                    stringResource(R.string.turn_on_let_other_devices_connect_to),
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
                            contentDescription = stringResource(R.string.phone_pairing_qr_code),
                            modifier = Modifier.size(240.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.this_code_only_works_on_the_same_wi_fi),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.anyone_who_scans_this_code_can_pair_with),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        if (state.usedFallbackPort) {
                            stringResource(R.string.reachable_on_this_network_at_the_usual, SyncEndpoint.display(state.host, state.port))
                        } else {
                            stringResource(R.string.reachable_on_this_network_at_the_code, SyncEndpoint.display(state.host, state.port))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showAdvancedSharing = !showAdvancedSharing }) {
                        Text(if (showAdvancedSharing) stringResource(R.string.hide_pairing_text) else stringResource(R.string.share_as_text_instead))
                    }
                    if (showAdvancedSharing) {
                        Text(
                            stringResource(R.string.the_pairing_text_works_like_the_qr_code),
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
                        ) { Text(stringResource(R.string.copy_pairing_text)) }
                        TextButton(onClick = onRegenerate) { Text(stringResource(R.string.make_a_new_code)) }
                        Text(
                            stringResource(R.string.replaces_the_code_above_if_nobody_has),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
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

private fun pairingImportMessage(context: android.content.Context, raw: String): String =
    if (PairingPayload.parse(raw)?.availability == PairingPayload.AVAILABILITY_FOREGROUND) {
        context.getString(R.string.pairing_saved_approve_the_request_on_the)
    } else {
        context.getString(R.string.pairing_saved_ncy_will_connect_when_the)
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
    NcyFlushList {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            if (showFingerprint) {
                Text(
                    stringResource(R.string.device_id, DeviceNameDisambiguation.shortFingerprint(peer.id)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.connects_to_this_phone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (peer.connected) stringResource(R.string.connected)
                else stringResource(R.string.last_connected, DateFormat.getDateTimeInstance().format(Date(peer.lastSeenAt))),
                style = MaterialTheme.typography.bodySmall,
                color = if (peer.connected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (peer.legacySharedCredential) {
                Text(
                    stringResource(R.string.paired_with_an_older_version_remove_and),
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
                Text(stringResource(R.string.unpair))
            }
        }
    }
    if (confirmRevoke) {
        NcyAlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = { Text(stringResource(R.string.unpair_2, peer.name)) },
            text = {
                Text(
                    stringResource(R.string.it_disconnects_and_cannot_sync_until_you)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRevoke = false
                        onRemove()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.unpair)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = false }) { Text(stringResource(R.string.cancel)) }
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
    showFingerprint: Boolean,
) {
    var showDiagnostics by remember { mutableStateOf(false) }
    NcyFlushList {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            if (showFingerprint) {
                Text(
                    stringResource(R.string.device_id, DeviceNameDisambiguation.shortFingerprint(peer.id)),
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
                Text(if (showDiagnostics) stringResource(R.string.hide_diagnostics) else stringResource(R.string.show_diagnostics))
            }
            if (showDiagnostics) {
                val formatter = DateFormat.getDateTimeInstance()
                val discovered = peer.lastDiscoveredAt?.let { formatter.format(Date(it)) } ?: stringResource(R.string.never)
                val authenticated = peer.lastAuthenticatedAt?.let { formatter.format(Date(it)) } ?: stringResource(R.string.never)
                val authenticatedEndpoint = if (
                    peer.lastAuthenticatedHost != null && peer.lastAuthenticatedPort != null
                ) SyncEndpoint.display(peer.lastAuthenticatedHost, peer.lastAuthenticatedPort) else stringResource(R.string.none)
                val retry = when {
                    peer.disabled -> stringResource(R.string.paused_by_user)
                    state is SyncManager.PeerState.Connected -> stringResource(R.string.not_retrying)
                    state is SyncManager.PeerState.WaitingForNetwork -> stringResource(R.string.waiting_to_connect)
                    peer.nextRetryAt != null -> stringResource(R.string.scheduled, formatter.format(Date(peer.nextRetryAt)))
                    state is SyncManager.PeerState.Failed &&
                        (state.reason == SyncFailurePolicy.REPAIR_MESSAGE ||
                            state.reason == SyncFailurePolicy.REJECTED_MESSAGE) -> stringResource(R.string.stopped_until_peer_is_removed)
                    else -> stringResource(R.string.no_retry_scheduled)
                }
                val lan = when {
                    state is SyncManager.PeerState.Connected -> stringResource(R.string.reachable)
                    state is SyncManager.PeerState.Failed || state is SyncManager.PeerState.Unavailable ->
                        stringResource(R.string.possibly_blocked_an_offline_peer_looks)
                    else -> stringResource(R.string.not_currently_tested)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.last_discovery, discovered), style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.last_authenticated_at, authenticated, authenticatedEndpoint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(stringResource(R.string.retry, retry), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.direct_lan, lan), style = MaterialTheme.typography.bodySmall)
                    peer.lastError?.let {
                        Text(
                            stringResource(R.string.last_error, it),
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
                    stringResource(R.string.automatic_retries_are_stopped_because),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (state is SyncManager.PeerState.Failed &&
                state.reason == SyncFailurePolicy.UNVERIFIED_REJECTION_MESSAGE
            ) {
                Text(
                    stringResource(R.string.the_refusal_came_before_the_connection),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (state is SyncManager.PeerState.Unavailable || state is SyncManager.PeerState.Failed) {
                Text(
                    stringResource(R.string.if_both_devices_are_open_this_network),
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
                    stringResource(R.string.automatic_sync),
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
                    Text(stringResource(R.string.sync_now))
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
                    Text(stringResource(R.string.remove))
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
    NcyFlushList {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(peer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.its_name_appears_after_pairing),
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
                        stringResource(R.string.the_computer_is_ready_tap_codes_match)
                    awaitingCodeCheck ->
                        stringResource(R.string.compare_this_code_with_the_one_on_the)
                    isBusy -> stringResource(R.string.waiting_for_the_computer_to_confirm)
                    peer.supportsPairRequest ->
                        stringResource(R.string.tap_pair_to_ask_this_device_to_add_this)
                    else ->
                        stringResource(R.string.show_the_code_on_that_device_then_scan)
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
                    stringResource(R.string.check_this_code_matches_the_one_on_the),
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
                        Text(stringResource(R.string.cancel))
                    }
                    if (awaitingCodeCheck) {
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onConfirmCode, shape = NcyButtonShape) {
                            Text(stringResource(R.string.codes_match))
                        }
                    }
                } else if (peer.supportsPairRequest) {
                    Button(onClick = onPair, shape = NcyButtonShape, enabled = !anyBusy) {
                        Text(stringResource(R.string.pair))
                    }
                } else {
                    Button(onClick = onScanQr, shape = NcyButtonShape, enabled = !anyBusy) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.scan_its_qr))
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

@Composable
private fun statusLine(state: SyncManager.PeerState, peer: SyncPeer): String = when (state) {
    SyncManager.PeerState.Idle -> {
        val last = peer.lastSyncAt
        if (last != null) stringResource(R.string.last_synchronized, DateFormat.getDateTimeInstance().format(Date(last)))
        else stringResource(R.string.waiting_to_connect)
    }
    SyncManager.PeerState.Disabled -> stringResource(R.string.sync_disabled_for_this_device)
    SyncManager.PeerState.WaitingForNetwork -> stringResource(R.string.waiting_to_connect)
    SyncManager.PeerState.Connecting -> stringResource(R.string.waiting_to_connect)
    is SyncManager.PeerState.Connected ->
        "${stringResource(R.string.connected_on_this_network)} · ${state.sentCount} sent · ${state.receivedCount} received"
    is SyncManager.PeerState.Unavailable -> stringResource(R.string.unavailable, state.reason)
    is SyncManager.PeerState.Failed -> stringResource(R.string.failed, state.reason)
}
