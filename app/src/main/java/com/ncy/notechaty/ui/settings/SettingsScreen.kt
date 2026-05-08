package com.ncy.notechaty.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ncy.notechaty.security.BiometricHelper
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.ThemeMode
import com.ncy.notechaty.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Sync

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
    var showPinSetup by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }
    val settings by viewModel.settings.collectAsState()

    // Export all launcher
    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            val tempFile = File(context.cacheDir, "ncy_all_chats.zip")
            scope.launch {
                try {
                    viewModel.exportAllChats(tempFile)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        tempFile.inputStream().use { input -> input.copyTo(os) }
                    }
                    tempFile.delete()
                    Toast.makeText(context, "All chats exported", Toast.LENGTH_SHORT).show()
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

    val topBarColor = MaterialTheme.colorScheme.primaryContainer
    val gradientTopColor = lerp(topBarColor, Color.White, 0.3f)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (settings.topBarGradient) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(gradientTopColor, topBarColor)
                        )
                    )
                } else Modifier,
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (settings.topBarGradient) Color.Transparent
                        else MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Data card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Data",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                    SettingsRow(
                        icon = Icons.Default.FileUpload,
                        title = "Import chat",
                        subtitle = "Import from .zip file",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            importLauncher.launch("application/zip")
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.FileDownload,
                        title = "Export all chats",
                        subtitle = "Save all chats as a single .zip file",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            exportAllLauncher.launch("ncy_all_chats.zip")
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Sync,
                        title = "Sync devices",
                        subtitle = "Push chats to a paired desktop on the same LAN",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenSync()
                        }
                    )
                }
            }

            // Auto-backup card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Auto-backup",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-backup", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Periodically backs up all chats internally",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoBackupEnabled,
                            onCheckedChange = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setAutoBackupEnabled(it)
                            }
                        )
                    }
                    if (settings.autoBackupEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Backup interval",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
            }

            // Privacy card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Privacy",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                    val hasPin = viewModel.hasPin()
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = if (hasPin) "Change PIN" else "Set protected chats PIN",
                        subtitle = if (hasPin) "PIN is set" else "Required to protect any chat",
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showPinSetup = true
                        }
                    )
                    if (hasPin) {
                        SettingsRow(
                            icon = Icons.Default.LockOpen,
                            title = "Remove PIN",
                            subtitle = "Unprotects all chats",
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showRemovePinConfirm = true
                            }
                        )
                        val biometricAvailable = BiometricHelper.canUseBiometric(context)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use fingerprint", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (biometricAvailable) "Unlock protected chats with biometrics"
                                    else "No biometrics enrolled on this device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.protectionUseBiometric && biometricAvailable,
                                enabled = biometricAvailable,
                                onCheckedChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setUseBiometric(it)
                                }
                            )
                        }
                        // Lock on app start
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lock app on start", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Require PIN to open NoteChaty",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.lockOnAppStart,
                                onCheckedChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setLockOnAppStart(it)
                                }
                            )
                        }
                        // Re-lock interval
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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

            // Appearance card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme mode
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Accent color
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Top bar gradient
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gradient,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top bar gradient", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Subtle gradient effect on the header bar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.topBarGradient,
                            onCheckedChange = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setTopBarGradient(it)
                            }
                        )
                    }
                }
            }

            // About card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "Ncy",
                        subtitle = "v${context.packageManager.getPackageInfo(context.packageName, 0).versionName} \u00b7 Made by 0x89-y",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "Open source licenses",
                        subtitle = "Third-party libraries used in this app",
                        onClick = onOpenLicenses
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Ncy v$version") },
            text = {
                Column {
                    Text(
                        text = "A note-to-self chat app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Made by 0x89-y",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/0x89-y"))
                                )
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
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
        AlertDialog(
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
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(if (isChange) "Change PIN" else "Set PIN") },
        text = {
            Column {
                Text(
                    "Choose a numeric PIN of at least 4 digits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(12) },
                    label = { Text("New PIN") },
                    singleLine = true,
                    isError = tooShort,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    supportingText = if (tooShort) { { Text("Minimum 4 digits") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter { c -> c.isDigit() }.take(12) },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    supportingText = if (mismatch) { { Text("PINs don't match") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4 && pin == confirm
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
