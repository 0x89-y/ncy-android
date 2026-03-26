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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.ThemeMode
import com.ncy.notechaty.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material.icons.filled.Backup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
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
        ) {
            SectionHeader("Data")
            SettingsRow(
                icon = Icons.Default.FileUpload,
                title = "Import chat",
                subtitle = "Import from .zip file",
                onClick = { importLauncher.launch("application/zip") }
            )
            SettingsRow(
                icon = Icons.Default.FileDownload,
                title = "Export all chats",
                subtitle = "Save all chats as a single .zip file",
                onClick = { exportAllLauncher.launch("ncy_all_chats.zip") }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("Auto-backup")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                )
            }
            if (settings.autoBackupEnabled) {
                Text(
                    text = "Backup interval",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(6, 12, 24, 48).forEach { hours ->
                        FilterChip(
                            selected = settings.autoBackupIntervalHours == hours,
                            onClick = { viewModel.setAutoBackupInterval(hours) },
                            label = { Text(if (hours < 24) "${hours}h" else "${hours / 24}d") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("Appearance")

            // Theme mode
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 56.dp, top = 8.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Accent color
            Text(
                text = "Accent color",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 56.dp, top = 12.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccentColor.entries.forEach { color ->
                    val circleColor = accentPreviewColor(color)
                    val isSelected = settings.accentColor == color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { viewModel.setAccentColor(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == AccentColor.DYNAMIC) {
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
            Spacer(modifier = Modifier.height(8.dp))

            // Top bar gradient
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    onCheckedChange = { viewModel.setTopBarGradient(it) }
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            SectionHeader("About")
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
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
