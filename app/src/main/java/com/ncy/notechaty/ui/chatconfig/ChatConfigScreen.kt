package com.ncy.notechaty.ui.chatconfig

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Check
import com.ncy.notechaty.ui.theme.NcyAlertDialog
import com.ncy.notechaty.ui.theme.NcyAvatarShape
import com.ncy.notechaty.ui.theme.NcyButtonShape
import com.ncy.notechaty.ui.theme.NcyRowDivider
import com.ncy.notechaty.ui.theme.NcySettingsPanel
import com.ncy.notechaty.ui.theme.NcySettingsRow
import com.ncy.notechaty.ui.theme.NcySettingsSection
import com.ncy.notechaty.ui.theme.NcySettingsToggleRow
import com.ncy.notechaty.ui.theme.ncyAvatarColor
import com.ncy.notechaty.ui.theme.ncyPaper
import com.ncy.notechaty.ui.theme.NcyTheme
import com.ncy.notechaty.ui.theme.NcyPill
import com.ncy.notechaty.ui.theme.NcyInitialTile
import com.ncy.notechaty.ui.theme.NcyChatHues
import com.ncy.notechaty.ui.theme.ncyChatHue
import com.ncy.notechaty.ui.theme.ncyTint
import com.ncy.notechaty.ui.theme.ncyOnTint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import java.io.File

/** Retention windows offered per chat. 0 is the "keep everything" case. */
private val RETENTION_CHOICES = listOf(
    0, 7, 14, 30, 90, 365,
)

@Composable
private fun retentionLabel(days: Int): String = when (days) {
    0 -> stringResource(R.string.keep_forever)
    365 -> stringResource(R.string.one_year)
    else -> pluralStringResource(R.plurals.retention_days, days, days)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

@Composable
fun ChatConfigScreen(
    chatId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ChatConfigViewModel = hiltViewModel()
) {
    val chat by viewModel.chat.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    // Non-null while the user is confirming a retention window.
    var retentionMenuOpen by remember { mutableStateOf(false) }
    var pendingRetention by remember { mutableStateOf<Int?>(null) }
    var pendingRetentionCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingRetention) {
        val days = pendingRetention
        // Zero clears the rule and deletes nothing, so it needs no confirmation.
        if (days == null || days == 0) return@LaunchedEffect
        pendingRetentionCount = viewModel.countExpiring(days)
    }
    LaunchedEffect(pendingRetention) {
        if (pendingRetention == 0) {
            viewModel.setRetentionDays(0)
            pendingRetention = null
        }
    }
    var editingName by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val chatDir = viewModel.getChatDir()

    // Update name text when chat loads
    LaunchedEffect(chat) {
        chat?.let { nameText = it.name }
    }

    val scope = rememberCoroutineScope()

    // Avatar picker
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            scope.launch {
                viewModel.setAvatar(tempFile)
                tempFile.delete()
            }
        }
    }

    // Background picker
    val bgPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "temp_bg_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            scope.launch {
                viewModel.setBackground(tempFile)
                tempFile.delete()
            }
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { target ->
            // The temp name is only a scratch path, but it is built from a
            // user-supplied chat name — keep separators and the like out of it.
            val safeName = (chat?.name ?: "chat").replace(Regex("[^A-Za-z0-9._-]"), "_").take(64)
            val tempFile = File(context.cacheDir, "export_$safeName.zip")
            // Copy only once the archive is actually written — see the ViewModel.
            viewModel.exportChat(
                tempFile,
                onReady = { file ->
                    runCatching {
                        context.contentResolver.openOutputStream(target)?.use { os ->
                            file.inputStream().use { input -> input.copyTo(os) }
                        }
                    }.onFailure { Log.w("ChatConfig", "export: copy to document failed", it) }
                    file.delete()
                },
                onError = { error ->
                    Log.w("ChatConfig", "export: writing archive failed", error)
                    tempFile.delete()
                },
            )
        }
    }


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .fillMaxSize()
                .padding(padding)
                .ncyPaper()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // The chat's tile, with a small ink badge to change its picture.
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatarFile = com.ncy.notechaty.data.sync.media.ChatImages.resolve(chatDir, chat?.avatarFile)
                if (avatarFile != null) {
                    AsyncImage(
                        model = avatarFile,
                        contentDescription = stringResource(R.string.chat_avatar),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(NcyAvatarShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    NcyInitialTile(chat?.name ?: "?", ncyChatHue(chat), size = 88.dp)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = 8.dp, y = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(NcyTheme.colors.ink)
                        .clickable(onClickLabel = stringResource(R.string.change_avatar)) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            avatarPicker.launch("image/*")
                        }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.change_avatar), modifier = Modifier.size(15.dp), tint = NcyTheme.colors.onInk)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Name section
            if (editingName) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.chat_name)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (nameText.isNotBlank()) {
                                viewModel.updateName(nameText.trim())
                                editingName = false
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                TextButton(
                    onClick = { editingName = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = chat?.name ?: stringResource(R.string.chat),
                        style = NcyTheme.type.pageTitle.copy(fontSize = 30.sp, lineHeight = 34.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_name),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Auto-delete. Deliberately global: the rule rides in ChatMetadata and
            // the deletion is an ordinary tombstone, so every paired device converges.
            NcySettingsSection(stringResource(R.string.delete_messages_after)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    RETENTION_CHOICES.forEach { days ->
                        NcyPill(
                            selected = (chat?.retentionDays ?: 0) == days,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if ((chat?.retentionDays ?: 0) != days) pendingRetention = days
                            },
                            label = retentionLabel(days),
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.applies_on_every_device_starred_messages),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )

            // Appearance — background image, bubble color, bubble gradient
            NcySettingsSection(stringResource(R.string.appearance)) {
                NcySettingsToggleRow(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    title = stringResource(R.string.other_devices_on_the_left),
                    subtitle = stringResource(R.string.show_messages_created_on_another_ncy),
                    checked = chat?.remoteMessagesOnLeft == true,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.setRemoteMessagesOnLeft(it)
                    },
                )
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.Wallpaper,
                    title = stringResource(R.string.background_image),
                    subtitle = if (chat?.backgroundFile != null) stringResource(R.string.tap_to_change) else stringResource(R.string.tap_to_set),
                    onClick = { bgPicker.launch("image/*") },
                    trailing = {
                        val bg = com.ncy.notechaty.data.sync.media.ChatImages.resolve(chatDir, chat?.backgroundFile)
                        if (bg != null) {
                            AsyncImage(
                                model = bg,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                )
                NcyRowDivider()
                NcySettingsPanel {
                    Text(stringResource(R.string.chat_colour), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    // Null is the colour the name gives; the rest are the paper hues.
                    val choices: List<Pair<String?, Color>> = listOf(null to ncyAvatarColor(chat?.name ?: "")) +
                        NcyChatHues.map { hue -> String.format("#%06X", hue.toArgb() and 0xFFFFFF) to hue }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        choices.forEach { (hex, hue) ->
                            val isSelected = chat?.bubbleColor.equals(hex, ignoreCase = true) || (hex == null && chat?.bubbleColor == null)
                            val ring = MaterialTheme.colorScheme.onSurface
                            val paper = MaterialTheme.colorScheme.background
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .then(if (isSelected) Modifier.border(2.dp, ring, RoundedCornerShape(14.dp)) else Modifier)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ncyTint(hue))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setBubbleColor(hex)
                                    }
                                    .semantics { if (isSelected) selected = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hex == null) {
                                    Text("A", style = NcyTheme.type.name.copy(fontSize = 15.sp), color = ncyOnTint(hue))
                                } else {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(hue))
                                }
                            }
                        }
                    }
                }
            }

            // Privacy
            NcySettingsSection(stringResource(R.string.privacy)) {
                val hasPin = viewModel.hasAppPin()
                NcySettingsToggleRow(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.protect_this_chat),
                    subtitle = if (hasPin) stringResource(R.string.require_pin_biometric_to_open)
                        else stringResource(R.string.set_an_app_pin_in_settings_first),
                    checked = chat?.isProtected == true,
                    enabled = hasPin,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.setProtected(it)
                    }
                )
                if (chat?.isProtected == true) {
                    NcyRowDivider()
                    NcySettingsToggleRow(
                        icon = Icons.Default.VisibilityOff,
                        title = stringResource(R.string.hide_name_on_chat_list),
                        subtitle = stringResource(R.string.show_gibberish_instead_of_the_real_name),
                        checked = chat?.hideName == true,
                        onCheckedChange = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.setHideName(it)
                        }
                    )
                }
            }

            // Organization
            NcySettingsSection(stringResource(R.string.organization)) {
                NcySettingsToggleRow(
                    icon = Icons.Default.PushPin,
                    title = stringResource(R.string.pin_chat),
                    subtitle = null,
                    checked = chat?.isPinned == true,
                    onCheckedChange = { viewModel.setPinned(it) }
                )
                NcyRowDivider()
                NcySettingsPanel {
                    var newTag by remember { mutableStateOf("") }
                    val currentTags = chat?.tags ?: emptyList()

                    if (currentTags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentTags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.removeTag(tag)
                                    },
                                    label = { Text(tag) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            placeholder = { Text(stringResource(R.string.add_tag)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newTag.isNotBlank()) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.addTag(newTag.trim())
                                    newTag = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_tag))
                        }
                    }
                }
                NcyRowDivider()
                NcySettingsToggleRow(
                    icon = Icons.Outlined.Archive,
                    title = stringResource(R.string.archive_chat),
                    subtitle = null,
                    checked = chat?.isArchived == true,
                    onCheckedChange = { viewModel.toggleArchive {} }
                )
            }

            // Data
            NcySettingsSection(stringResource(R.string.data)) {
                NcySettingsRow(
                    icon = Icons.Default.Share,
                    title = stringResource(R.string.export_chat),
                    subtitle = null,
                    onClick = { exportLauncher.launch("${chat?.name ?: "chat"}.zip") }
                )
            }

            NcySettingsSection(null) {
                NcyRowDivider()
                NcySettingsRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.delete_chat),
                    subtitle = null,
                    destructive = true,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showDeleteConfirm = true
                    },
                )
            }
        }
    }

    // Enabling a window deletes everything already past it, immediately and on every
    // device, so say how much that is before doing it rather than after.
    pendingRetention?.takeIf { it > 0 }?.let { days ->
        val count = pendingRetentionCount
        NcyAlertDialog(
            onDismissRequest = { pendingRetention = null; pendingRetentionCount = null },
            title = { Text(stringResource(R.string.auto_delete_old_messages)) },
            text = {
                Text(
                    if (count == null) stringResource(R.string.checking)
                    else if (count > 0)
                        pluralStringResource(R.plurals.retention_confirm, count, count, days)
                    else stringResource(R.string.nothing_in_this_chat_is_older_than_days, days)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = count != null,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.setRetentionDays(days)
                        pendingRetention = null
                        pendingRetentionCount = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if ((count ?: 0) > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) { Text(stringResource(R.string.turn_on)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRetention = null; pendingRetentionCount = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showDeleteConfirm) {
        NcyAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_chat)) },
            text = { Text(stringResource(R.string.delete_chat_confirm, chat?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.deleteChat(onDeleted)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
