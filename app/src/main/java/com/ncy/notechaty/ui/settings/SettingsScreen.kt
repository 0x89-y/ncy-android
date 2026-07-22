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
import com.ncy.notechaty.data.model.SyncTransportMode
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
    var showTransportModeDialog by remember { mutableStateOf(false) }
    var pendingPearMode by remember { mutableStateOf<SyncTransportMode?>(null) }
    var settingsSection by rememberSaveable { mutableStateOf<String?>(null) }
    val settings by viewModel.settings.collectAsState()

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
                        Triple("General", Icons.Default.Lock, "Privacy and app protection"),
                        Triple("Sync", Icons.Default.Sync, "Paired devices and connection mode"),
                        Triple("Interface", Icons.Default.Palette, "Theme and appearance"),
                        Triple("Transcription", Icons.Default.Info, "Voice transcription options"),
                        Triple("Backup", Icons.Default.Backup, "Import, export, and automatic backups"),
                    ).forEachIndexed { index, (title, icon, subtitle) ->
                        NcySettingsRow(icon = icon, title = title, subtitle = subtitle, onClick = { settingsSection = title })
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

            // ---- Sync ----
            if (settingsSection == "Sync") {
            NcySettingsSection("Sync") {
                NcySettingsRow(
                    icon = Icons.Default.Sync,
                    title = "Paired devices",
                    subtitle = "Manage devices that synchronize your chats",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOpenSync()
                    }
                )
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.Public,
                    title = "Connection mode",
                    subtitle = "${settings.syncTransportMode.displayName} · ${settings.syncTransportMode.description}",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showTransportModeDialog = true
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

    if (showTransportModeDialog) {
        NcyAlertDialog(
            onDismissRequest = { showTransportModeDialog = false },
            title = { Text("Connection mode") },
            text = {
                Column {
                    SyncTransportMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (mode.usesPear && !settings.syncTransportMode.usesPear) {
                                        pendingPearMode = mode
                                    } else {
                                        viewModel.setSyncTransportMode(mode)
                                    }
                                    showTransportModeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.syncTransportMode == mode,
                                onClick = null,
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(mode.displayName)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTransportModeDialog = false }) { Text("Cancel") }
            },
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
                TextButton(
                    onClick = {
                        viewModel.setSyncTransportMode(selectedMode)
                        pendingPearMode = null
                    },
                ) { Text("Use ${selectedMode.displayName}") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPearMode = null }) { Text("Cancel") }
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

