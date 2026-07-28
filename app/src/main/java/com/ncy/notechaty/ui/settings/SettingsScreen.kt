package com.ncy.notechaty.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ncy.notechaty.security.BiometricHelper
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.model.ThemeMode
import com.ncy.notechaty.data.sync.AuthenticatedDeviceName
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
import com.ncy.notechaty.ui.desktoptheme.NcyButtonShape
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.NcySettingsPanel
import com.ncy.notechaty.ui.desktoptheme.NcySettingsRow
import com.ncy.notechaty.ui.desktoptheme.NcySettingsSection
import com.ncy.notechaty.ui.desktoptheme.NcySettingsToggleRow
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import com.ncy.notechaty.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DeleteForever

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenSync: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }
    var showPurgeTombstonesConfirm by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var settingsSection by rememberSaveable { mutableStateOf<String?>(null) }
    val settings by viewModel.settings.collectAsState()
    val mediaReport by viewModel.mediaReport.collectAsState()
    var resyncing by remember { mutableStateOf(false) }
    var resyncNote by remember { mutableStateOf("Ask every device to resend its history") }
    val mediaBusy by viewModel.mediaBusy.collectAsState()

    // Export all launcher
    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "ncy_backup.zip")
            scope.launch {
                try {
                    viewModel.exportEverything(tempFile)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        tempFile.inputStream().use { input -> input.copyTo(os) }
                    }
                    tempFile.delete()
                    Toast.makeText(context, "Backup created (all profiles)", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    tempFile.delete()
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "import_temp.zip")
            if (FileUtils.copyUriToFile(context, it, tempFile)) {
                scope.launch {
                    try {
                        viewModel.importChat(tempFile)
                        tempFile.delete()
                        Toast.makeText(context, "Chat imported successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        tempFile.delete()
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(settingsSection ?: "Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (settingsSection != null) settingsSection = null else onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .ncyDottedGrid()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (settingsSection == null) {
                NcySettingsPanel {
                    listOf(
                        Triple("General", Icons.Default.Person, "Device identity and app protection"),
                        Triple("Sync", Icons.Default.Sync, "Paired devices and connection mode"),
                        Triple("Interface", Icons.Default.Palette, "Theme and appearance"),
                        Triple("Transcription", Icons.Default.Info, "Voice transcription options"),
                        Triple("Backup", Icons.Default.Backup, "Import, export, and automatic backups"),
                    ).forEachIndexed { index, (title, icon, subtitle) ->
                        NcySettingsRow(
                            icon = icon,
                            title = title,
                            subtitle = subtitle,
                            onClick = { if (title == "Sync") onOpenSync() else settingsSection = title },
                        )
                        if (index < 4) NcyRowDivider()
                    }
                }
                NcySettingsPanel {
                    NcySettingsRow(
                        icon = Icons.Default.NewReleases,
                        title = "Changelog",
                        subtitle = "Recent changes and version history",
                        onClick = { showChangelog = true }
                    )
                    NcyRowDivider()
                    NcySettingsRow(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version, credits, and open-source licenses",
                        onClick = { settingsSection = "About" }
                    )
                }
            }

            // ---- Backup ----
            if (settingsSection == "Backup") {
            // Media storage. The blob store is append-only, so deleting a message
            // never removed its attachment — measure first, then offer to reclaim.
            NcySettingsSection("Media storage") {
                val report = mediaReport
                NcySettingsRow(
                    icon = Icons.Default.Storage,
                    title = "Downloaded media",
                    subtitle = when {
                        mediaBusy -> "Scanning…"
                        report == null ->
                            "Photos, voice messages and attachments kept for your chats"
                        else ->
                            formatStorageSize(report.totalBytes) + " across " +
                                report.scanned + " files"
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.scanMedia(dryRun = true)
                    },
                )
                if (report != null && report.lockedChats.isNotEmpty()) {
                    NcyRowDivider()
                    Text(
                        report.lockedChats.size.toString() + " protected " +
                            (if (report.lockedChats.size == 1) "chat is" else "chats are") +
                            " locked. Their media references are still encrypted, so their files were left alone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (report != null && report.deleted > 0) {
                    NcyRowDivider()
                    NcySettingsRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Free up " + formatStorageSize(report.reclaimedBytes),
                        subtitle = "Media no message refers to any more",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            viewModel.scanMedia(dryRun = false)
                        },
                    )
                } else if (report != null) {
                    NcyRowDivider()
                    Text(
                        "Nothing to clean up — every stored file is still in use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // Recovery for a device that is missing messages, or still showing ones
            // deleted elsewhere. This has to live here, not on the desktop: our
            // watermark is what the desktop treats as authoritative.
            NcySettingsSection("Troubleshooting") {
                NcySettingsRow(
                    icon = Icons.Default.Sync,
                    title = "Force full resync",
                    subtitle = resyncNote,
                    onClick = {
                        if (!resyncing) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            resyncing = true
                            viewModel.forceFullResync { ok ->
                                resyncing = false
                                resyncNote = if (ok) {
                                    "Sync progress cleared. Every device resends its history on reconnect."
                                } else {
                                    "Could not clear sync progress. Try again."
                                }
                            }
                        }
                    },
                )
            }

            NcySettingsSection("Backup") {
                NcySettingsRow(
                    icon = Icons.Default.FileUpload,
                    title = "Import chat",
                    subtitle = "Import from .zip file",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        importLauncher.launch("application/zip")
                    }
                )
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = "Back up everything",
                    subtitle = "All chats from every profile + the profile list, as one .zip",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        exportAllLauncher.launch("ncy_backup.zip")
                    }
                )
                NcyRowDivider()
                NcySettingsToggleRow(
                    icon = Icons.Default.Backup,
                    title = "Auto-backup",
                    subtitle = "Periodically backs up all chats internally",
                    checked = settings.autoBackupEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.setAutoBackupEnabled(it)
                    }
                )
                if (settings.autoBackupEnabled) {
                    NcyRowDivider()
                    NcySettingsPanel {
                        Text("Backup interval", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(6, 12, 24, 48).forEach { hours ->
                                FilterChip(
                                    selected = settings.autoBackupIntervalHours == hours,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setAutoBackupInterval(hours)
                                    },
                                    label = { Text(if (hours < 24) "${hours}h" else "${hours / 24}d") }
                                )
                            }
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Forget deletion records",
                    subtitle = "Advanced: stale backups may restore deleted chats or messages",
                    onClick = { showPurgeTombstonesConfirm = true },
                )
            }
            }

            // ---- Device identity ----
            if (settingsSection == "General") {
            NcySettingsSection("General") {
                NcySettingsRow(
                    icon = Icons.Default.Person,
                    title = "Device name",
                    subtitle = settings.deviceName,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showDeviceNameDialog = true
                    },
                )
            }
            }

            // ---- Privacy ----
            if (settingsSection == "General") {
            NcySettingsSection("Privacy") {
                val hasPin = viewModel.hasPin()
                NcySettingsRow(
                    icon = Icons.Default.Lock,
                    title = if (hasPin) "Change PIN" else "Set protected chats PIN",
                    subtitle = if (hasPin) "PIN is set" else "Required to protect any chat",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showPinSetup = true
                    }
                )
                if (hasPin) {
                    NcyRowDivider()
                    NcySettingsRow(
                        icon = Icons.Default.LockOpen,
                        title = "Remove PIN",
                        subtitle = "Unprotects all chats",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showRemovePinConfirm = true
                        }
                    )
                    val biometricAvailable = BiometricHelper.canUseBiometric(context)
                    NcyRowDivider()
                    NcySettingsToggleRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Use fingerprint",
                        subtitle = if (biometricAvailable) "Unlock protected chats with biometrics"
                            else "No biometrics enrolled on this device",
                        checked = settings.protectionUseBiometric && biometricAvailable,
                        enabled = biometricAvailable,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.setUseBiometric(it)
                        }
                    )
                    NcyRowDivider()
                    NcySettingsToggleRow(
                        icon = Icons.Default.Lock,
                        title = "Lock app on start",
                        subtitle = "Require PIN to open Ncy",
                        checked = settings.lockOnAppStart,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.setLockOnAppStart(it)
                        }
                    )
                    NcyRowDivider()
                    NcySettingsPanel {
                        Text("Re-lock chats", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "How long an unlock stays valid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0 to "Always", 1 to "1m", 5 to "5m", 15 to "15m").forEach { (mins, label) ->
                                FilterChip(
                                    selected = settings.protectionGracePeriodMinutes == mins,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setProtectionGracePeriod(mins)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
            }

            // ---- Appearance ----
            if (settingsSection == "Interface") {
            NcySettingsSection("Interface") {
                NcySettingsPanel {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setThemeMode(mode)
                                },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsPanel {
                    Text("Accent color", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccentColor.entries.forEach { color ->
                            val circleColor = accentPreviewColor(color)
                            val isSelected = settings.accentColor == color
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setAccentColor(color)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                } else if (color == AccentColor.DYNAMIC) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "Dynamic",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsPanel {
                    Text("Chat order", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChatSortMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.chatSortMode == mode,
                                onClick = { viewModel.setChatSortMode(mode) },
                                label = { Text(mode.displayName) },
                                leadingIcon = if (settings.chatSortMode == mode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
            }

            if (settingsSection == "Transcription") {
                NcySettingsSection("Voice transcription") {
                    NcySettingsPanel {
                        Text("Voice transcription is not available on Android yet.")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Voice messages can still be recorded, sent, and played normally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- About ----
            if (settingsSection == "About") {
            NcySettingsSection("About") {
                NcySettingsRow(
                    icon = Icons.Default.Person,
                    title = "Ncy",
                    subtitle = "v${context.packageManager.getPackageInfo(context.packageName, 0).versionName} \u00b7 Made by 0x89-y",
                    onClick = { showAboutDialog = true }
                )
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.Info,
                    title = "Open source licenses",
                    subtitle = "Third-party libraries used in this app",
                    onClick = onOpenLicenses
                )
            }
            }
        }
    }

    if (showChangelog) {
        ChangelogSheet(onDismiss = { showChangelog = false })
    }

    if (showAboutDialog) {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(NcyAvatarShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "N",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ncy", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "v$version · A note-to-self chat app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Made by 0x89-y",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NcySheetActionRow(Icons.Default.Person, "View on GitHub") {
                    showAboutDialog = false
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/0x89-y"))
                    )
                }
            }
        }
    }

    if (showDeviceNameDialog) {
        var editedName by rememberSaveable(settings.deviceName) { mutableStateOf(settings.deviceName) }
        NcyAlertDialog(
            onDismissRequest = { showDeviceNameDialog = false },
            title = { Text("Device name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(AuthenticatedDeviceName.MAX_LENGTH) },
                    label = { Text("Name") },
                    supportingText = {
                        Text("Shown to paired devices after authentication.")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editedName.isNotBlank(),
                    onClick = {
                        viewModel.setDeviceName(editedName)
                        showDeviceNameDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            isChange = viewModel.hasPin(),
            onDismiss = { showPinSetup = false },
            onConfirm = { newPin ->
                viewModel.setPin(newPin)
                showPinSetup = false
                Toast.makeText(context, "PIN saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showRemovePinConfirm) {
        NcyAlertDialog(
            onDismissRequest = { showRemovePinConfirm = false },
            title = { Text("Remove PIN?") },
            text = { Text("Removing the PIN will unprotect every chat that's currently protected. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearPin()
                        showRemovePinConfirm = false
                        Toast.makeText(context, "PIN removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPurgeTombstonesConfirm) {
        NcyAlertDialog(
            onDismissRequest = { showPurgeTombstonesConfirm = false },
            title = { Text("Forget deletion records?") },
            text = {
                Text(
                    "Only continue if every old device and backup containing deleted chats or messages " +
                        "is gone. Stale history paired later can restore them. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeTombstonesConfirm = false
                        scope.launch {
                            val removed = viewModel.purgeChatTombstones()
                            Toast.makeText(
                                context,
                                "Forgot $removed deletion record${if (removed == 1) "" else "s"}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Forget permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeTombstonesConfirm = false }) { Text("Cancel") }
            },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupDialog(
    isChange: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && pin != confirm
    val tooShort = pin.isNotEmpty() && pin.length < 4

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (isChange) "Change PIN" else "Set PIN", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose a PIN of at least 4 characters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.take(64) },
                label = { Text("New PIN") },
                singleLine = true,
                isError = tooShort,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                supportingText = if (tooShort) { { Text("Minimum 4 characters") } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.take(64) },
                label = { Text("Confirm PIN") },
                singleLine = true,
                isError = mismatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                supportingText = if (mismatch) { { Text("PINs don't match") } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4 && pin == confirm,
                shape = NcyButtonShape,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangelogSheet(onDismiss: () -> Unit) {
    val series = remember { CHANGELOG.map { it.version.split('.').take(2).joinToString(".") }.distinct() }
    var selectedSeries by remember { mutableStateOf(series.firstOrNull() ?: "all") }
    val visibleEntries = remember(selectedSeries) {
        if (selectedSeries == "all") CHANGELOG else CHANGELOG.filter { it.version.startsWith("$selectedSeries.") }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("What's new", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedSeries == "all", onClick = { selectedSeries = "all" }, label = { Text("All") })
                series.forEach { value ->
                    FilterChip(selected = selectedSeries == value, onClick = { selectedSeries = value }, label = { Text("$value.x") })
                }
            }
            visibleEntries.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("v${entry.version}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            entry.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    entry.changes.forEach { change ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary)
                            Text(
                                change,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSettingFileSize(bytes: Long): String =
    if (bytes >= 1024L * 1024 * 1024) "1 GB" else "${bytes / (1024 * 1024)} MB"

private fun accentPreviewColor(accent: AccentColor): Color = when (accent) {
    AccentColor.DYNAMIC -> Color(0xFF757575)
    AccentColor.PURPLE -> Color(0xFF6750A4)
    AccentColor.BLUE -> Color(0xFF1976D2)
    AccentColor.GREEN -> Color(0xFF388E3C)
    AccentColor.RED -> Color(0xFFC62828)
    AccentColor.ORANGE -> Color(0xFFE65100)
    AccentColor.PINK -> Color(0xFFD81B60)
    AccentColor.TEAL -> Color(0xFF00897B)
}


/** Human-readable byte count for the media storage panel. */
private fun formatStorageSize(bytes: Long): String = when {
    bytes < 1024 -> bytes.toString() + " B"
    bytes < 1024 * 1024 -> (bytes / 1024).toString() + " KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
