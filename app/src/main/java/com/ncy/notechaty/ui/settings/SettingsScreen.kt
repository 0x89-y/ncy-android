package com.ncy.notechaty.ui.settings

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wallpaper
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
import com.ncy.notechaty.ui.desktoptheme.NcyChoiceChip
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.accentSwatch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    // A PIN change or removal re-encrypts every protected chat, so it reports back.
    var pinBusy by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showPurgeTombstonesConfirm by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var settingsSection by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    // A page is local state, not a nav destination, so the system back gesture has
    // to be told; without this it left Settings entirely instead of returning here.
    BackHandler(enabled = settingsSection != null) { settingsSection = null }
    val settings by viewModel.settings.collectAsState()
    val mediaReport by viewModel.mediaReport.collectAsState()
    var resyncing by remember { mutableStateOf(false) }
    var resyncNote by remember { mutableStateOf(context.getString(R.string.ask_every_device_to_resend)) }
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
                    Toast.makeText(context, context.getString(R.string.backup_created_all_profiles), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    tempFile.delete()
                    Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, context.getString(R.string.chat_imported_successfully), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        tempFile.delete()
                        Toast.makeText(context, context.getString(R.string.import_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(settingsSection?.let { stringResource(it.title) } ?: stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (settingsSection != null) settingsSection = null else onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
                NcyDividedCard {
                    // Null page: Sync is its own screen rather than a page here.
                    listOf<Triple<SettingsPage?, ImageVector, Int>>(
                        Triple(SettingsPage.General, Icons.Default.PhoneAndroid, R.string.settings_device_name_hint),
                        Triple(SettingsPage.Privacy, Icons.Default.Shield, R.string.settings_privacy_hint),
                        Triple(null, Icons.Default.Sync, R.string.settings_sync_hint),
                        Triple(SettingsPage.Interface, Icons.Default.Palette, R.string.settings_interface_hint),
                        Triple(SettingsPage.Transcription, Icons.Default.RecordVoiceOver, R.string.settings_transcription_hint),
                        Triple(SettingsPage.Backup, Icons.Default.Backup, R.string.settings_backup_hint),
                    ).forEachIndexed { index, (page, icon, subtitle) ->
                        if (index > 0) NcyRowDivider()
                        NcySettingsRow(
                            icon = icon,
                            title = page?.let { stringResource(it.title) } ?: stringResource(R.string.sync),
                            subtitle = stringResource(subtitle),
                            onClick = { if (page == null) onOpenSync() else settingsSection = page },
                        )
                    }
                }
                NcyDividedCard {
                    NcySettingsRow(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.changelog),
                        subtitle = stringResource(R.string.recent_changes_and_version_history),
                        onClick = { showChangelog = true }
                    )
                    NcyRowDivider()
                    NcySettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(SettingsPage.About.title),
                        subtitle = stringResource(R.string.version_credits_and_open_source_licenses),
                        onClick = { settingsSection = SettingsPage.About }
                    )
                }
            }

            // ---- Backup ----
            if (settingsSection == SettingsPage.Backup) {
            // Media storage. The blob store is append-only, so deleting a message
            // never removed its attachment — measure first, then offer to reclaim.
            NcySettingsSection(stringResource(R.string.media_storage)) {
                val report = mediaReport
                NcySettingsRow(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.downloaded_media),
                    subtitle = when {
                        mediaBusy -> stringResource(R.string.scanning)
                        report == null ->
                            stringResource(R.string.photos_voice_messages_and_attachments)
                        else ->
                            stringResource(R.string.media_storage_summary, formatStorageSize(report.totalBytes), report.scanned)
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.scanMedia(dryRun = true)
                    },
                )
                if (report != null && report.lockedChats.isNotEmpty()) {
                    NcyRowDivider()
                    Text(
                        pluralStringResource(R.plurals.locked_chats_skipped, report.lockedChats.size, report.lockedChats.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (report != null && report.deleted > 0) {
                    NcyRowDivider()
                    NcySettingsRow(
                        icon = Icons.Default.DeleteSweep,
                        title = stringResource(R.string.free_up) + formatStorageSize(report.reclaimedBytes),
                        subtitle = stringResource(R.string.media_no_message_refers_to_any_more),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            viewModel.scanMedia(dryRun = false)
                        },
                    )
                } else if (report != null) {
                    NcyRowDivider()
                    Text(
                        stringResource(R.string.nothing_to_clean_up_every_stored_file_is),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // Recovery for a device that is missing messages, or still showing ones
            // deleted elsewhere. This has to live here, not on the desktop: our
            // watermark is what the desktop treats as authoritative.
            NcySettingsSection(stringResource(R.string.troubleshooting)) {
                NcySettingsRow(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.force_full_resync),
                    subtitle = resyncNote,
                    onClick = {
                        if (!resyncing) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            resyncing = true
                            viewModel.forceFullResync { ok ->
                                resyncing = false
                                resyncNote = if (ok) {
                                    context.getString(R.string.sync_progress_cleared_every_device)
                                } else {
                                    context.getString(R.string.could_not_clear_sync_progress_try_again)
                                }
                            }
                        }
                    },
                )
            }

            NcySettingsSection(stringResource(R.string.import_and_export)) {
                NcySettingsRow(
                    icon = Icons.Default.FileUpload,
                    title = stringResource(R.string.import_chat),
                    subtitle = null,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        importLauncher.launch("application/zip")
                    }
                )
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = stringResource(R.string.back_up_everything),
                    subtitle = stringResource(R.string.all_chats_from_every_profile_the_profile),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        exportAllLauncher.launch("ncy_backup.zip")
                    }
                )
                NcyRowDivider()
                NcySettingsToggleRow(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.auto_backup),
                    subtitle = stringResource(R.string.periodically_backs_up_all_chats),
                    checked = settings.autoBackupEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.setAutoBackupEnabled(it)
                    }
                )
                if (settings.autoBackupEnabled) {
                    NcyRowDivider()
                    NcySettingsPanel {
                        Text(stringResource(R.string.backup_interval), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(6, 12, 24, 48).forEach { hours ->
                                NcyChoiceChip(
                                    selected = settings.autoBackupIntervalHours == hours,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setAutoBackupInterval(hours)
                                    },
                                    label = if (hours < 24) "${hours}h" else "${hours / 24}d",
                                )
                            }
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.forget_deletion_records_2),
                    subtitle = stringResource(R.string.advanced_stale_backups_may_restore),
                    onClick = { showPurgeTombstonesConfirm = true },
                )
            }
            }

            // ---- Device identity ----
            if (settingsSection == SettingsPage.General) {
            NcySettingsSection(null) {
                NcySettingsRow(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.device_name),
                    subtitle = settings.deviceName,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showDeviceNameDialog = true
                    },
                )
            }
            }

            // ---- Privacy & security ----
            if (settingsSection == SettingsPage.Privacy) {
            NcySettingsSection(null) {
                val hasPin = viewModel.hasPin()
                NcySettingsRow(
                    icon = Icons.Default.Lock,
                    title = if (hasPin) stringResource(R.string.change_pin) else stringResource(R.string.set_protected_chats_pin),
                    subtitle = if (hasPin) stringResource(R.string.pin_is_set) else stringResource(R.string.required_to_protect_any_chat),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showPinSetup = true
                    }
                )
                if (hasPin) {
                    NcyRowDivider()
                    NcySettingsRow(
                        icon = Icons.Default.LockOpen,
                        title = stringResource(R.string.remove_pin),
                        subtitle = stringResource(R.string.unprotects_all_chats),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showRemovePinConfirm = true
                        }
                    )
                    val biometricAvailable = BiometricHelper.canUseBiometric(context)
                    NcyRowDivider()
                    NcySettingsToggleRow(
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.use_fingerprint),
                        subtitle = if (biometricAvailable) stringResource(R.string.unlock_protected_chats_with_biometrics)
                            else stringResource(R.string.no_biometrics_enrolled_on_this_device),
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
                        title = stringResource(R.string.lock_app_on_start),
                        subtitle = null,
                        checked = settings.lockOnAppStart,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.setLockOnAppStart(it)
                        }
                    )
                    NcyRowDivider()
                    NcySettingsPanel {
                        Text(stringResource(R.string.re_lock_chats), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.how_long_an_unlock_stays_valid_leaving),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0 to stringResource(R.string.always), 1 to "1m", 5 to "5m", 15 to "15m").forEach { (mins, label) ->
                                NcyChoiceChip(
                                    selected = settings.protectionGracePeriodMinutes == mins,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setProtectionGracePeriod(mins)
                                    },
                                    label = label,
                                )
                            }
                        }
                    }
                }
            }
            }

            // ---- Appearance ----
            if (settingsSection == SettingsPage.Interface) {
            NcySettingsSection(null) {
                NcySettingsPanel {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            NcyChoiceChip(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setThemeMode(mode)
                                },
                                label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            )
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsPanel {
                    Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccentColor.entries.forEach { color ->
                            // The swatch is the colour the choice really produces, named,
                            // so no option is a guess (the old grey "Dynamic" was violet).
                            val swatch = accentSwatch(color, dark)
                            val isSelected = settings.accentColor == color
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setAccentColor(color)
                                    }
                                    .semantics(mergeDescendants = true) { selected = isSelected },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(swatch),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (swatch.luminance() > 0.4f) Color.Black else Color.White,
                                        )
                                    } else if (color == AccentColor.WALLPAPER) {
                                        Icon(
                                            Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (swatch.luminance() > 0.4f) Color.Black else Color.White,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(color.label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsPanel {
                    Text(stringResource(R.string.chat_order), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChatSortMode.entries.forEach { mode ->
                            NcyChoiceChip(
                                selected = settings.chatSortMode == mode,
                                onClick = { viewModel.setChatSortMode(mode) },
                                label = stringResource(mode.label),
                            )
                        }
                    }
                }
            }
            }

            if (settingsSection == SettingsPage.Transcription) {
                NcySettingsSection(null) {
                    NcySettingsPanel {
                        Text(stringResource(R.string.voice_transcription_is_not_available_on))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.voice_messages_can_still_be_recorded),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- About ----
            if (settingsSection == SettingsPage.About) {
            NcySettingsSection(null) {
                NcySettingsRow(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.ncy),
                    subtitle = stringResource(R.string.version_made_by, context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""),
                    onClick = { showAboutDialog = true }
                )
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.open_source_licenses),
                    subtitle = null,
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
                    Text(stringResource(R.string.ncy), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.v_a_note_to_self_chat_app, version ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.made_by_0x89_y),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NcySheetActionRow(Icons.Default.Person, stringResource(R.string.view_on_github)) {
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
            title = { Text(stringResource(R.string.device_name)) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(AuthenticatedDeviceName.MAX_LENGTH) },
                    label = { Text(stringResource(R.string.name)) },
                    supportingText = {
                        Text(stringResource(R.string.shown_to_paired_devices_after))
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
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceNameDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            isChange = viewModel.hasPin(),
            error = pinError,
            busy = pinBusy,
            onDismiss = { if (!pinBusy) { showPinSetup = false; pinError = null } },
            onConfirm = { currentPin, newPin ->
                pinBusy = true
                viewModel.setPin(currentPin, newPin) { error ->
                    pinBusy = false
                    pinError = error
                    if (error == null) {
                        showPinSetup = false
                        Toast.makeText(context, context.getString(R.string.pin_saved), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showRemovePinConfirm) {
        var currentPin by remember { mutableStateOf("") }
        NcyAlertDialog(
            onDismissRequest = { if (!pinBusy) { showRemovePinConfirm = false; pinError = null } },
            title = { Text(stringResource(R.string.remove_pin_2)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.every_protected_chat_is_decrypted_and)
                    )
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it.take(64) },
                        label = { Text(stringResource(R.string.current_pin)) },
                        singleLine = true,
                        isError = pinError != null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        supportingText = pinError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pinBusy = true
                        viewModel.clearPin(currentPin) { error ->
                            pinBusy = false
                            pinError = error
                            if (error == null) {
                                showRemovePinConfirm = false
                                Toast.makeText(context, context.getString(R.string.pin_removed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !pinBusy && currentPin.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(if (pinBusy) stringResource(R.string.removing) else stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinConfirm = false; pinError = null }, enabled = !pinBusy) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showPurgeTombstonesConfirm) {
        NcyAlertDialog(
            onDismissRequest = { showPurgeTombstonesConfirm = false },
            title = { Text(stringResource(R.string.forget_deletion_records)) },
            text = {
                Text(
                    stringResource(R.string.only_continue_if_every_old_device_and)
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
                                context.resources.getQuantityString(R.plurals.forgot_deletion_records, removed, removed),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.forget_permanently)) }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeTombstonesConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupDialog(
    isChange: Boolean,
    error: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (current: String?, new: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
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
            Text(if (isChange) stringResource(R.string.change_pin) else stringResource(R.string.set_pin), style = MaterialTheme.typography.titleLarge)
            Text(
                if (isChange) {
                    stringResource(R.string.protected_chats_are_re_encrypted_with)
                } else stringResource(R.string.choose_a_pin_of_at_least_4_characters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isChange) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it.take(64) },
                    label = { Text(stringResource(R.string.current_pin)) },
                    singleLine = true,
                    isError = error != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.take(64) },
                label = { Text(stringResource(R.string.new_pin)) },
                singleLine = true,
                isError = tooShort,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                supportingText = if (tooShort) { { Text(stringResource(R.string.minimum_4_characters)) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.take(64) },
                label = { Text(stringResource(R.string.confirm_pin)) },
                singleLine = true,
                isError = mismatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                supportingText = if (mismatch) { { Text(stringResource(R.string.pins_don_t_match)) } } else null,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(current.takeIf { isChange }, pin) },
                enabled = !busy && pin.length >= 4 && pin == confirm && (!isChange || current.isNotEmpty()),
                shape = NcyButtonShape,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) stringResource(R.string.saving) else stringResource(R.string.save)) }
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
            Text(stringResource(R.string.what_s_new), style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NcyChoiceChip(selected = selectedSeries == "all", onClick = { selectedSeries = "all" }, label = stringResource(R.string.all))
                series.forEach { value ->
                    NcyChoiceChip(selected = selectedSeries == value, onClick = { selectedSeries = value }, label = "$value.x")
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

/** The pages inside Settings. Sync is a screen of its own, opened from the list. */
private enum class SettingsPage(@androidx.annotation.StringRes val title: Int) {
    General(R.string.page_general),
    Privacy(R.string.page_privacy),
    Interface(R.string.page_interface),
    Transcription(R.string.page_transcription),
    Backup(R.string.page_backup),
    About(R.string.page_about),
}


/** Human-readable byte count for the media storage panel. */
private fun formatStorageSize(bytes: Long): String = when {
    bytes < 1024 -> bytes.toString() + " B"
    bytes < 1024 * 1024 -> (bytes / 1024).toString() + " KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
