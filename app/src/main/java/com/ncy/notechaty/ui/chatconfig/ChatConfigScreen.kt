package com.ncy.notechaty.ui.chatconfig

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.io.File

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
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            val tempFile = File(context.cacheDir, "export_${chat?.name ?: "chat"}.zip")
            viewModel.exportChat(tempFile)
            // Copy to the URI after export
            outputStream?.use { os ->
                tempFile.inputStream().use { input -> input.copyTo(os) }
            }
            tempFile.delete()
        }
    }

    val topBarColor = MaterialTheme.colorScheme.primaryContainer
    val gradientTopColor = lerp(topBarColor, Color.White, 0.3f)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (appSettings.topBarGradient) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(gradientTopColor, topBarColor)
                        )
                    )
                } else Modifier,
                title = { Text("Chat Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (appSettings.topBarGradient) Color.Transparent
                        else MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar section
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatarFile = chat?.avatarFile?.let { File(chatDir, it) }
                if (avatarFile != null && avatarFile.exists()) {
                    AsyncImage(
                        model = avatarFile,
                        contentDescription = "Chat avatar",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (chat?.name ?: "?").take(1).uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        avatarPicker.launch("image/*")
                    },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change avatar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Name section
            if (editingName) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Chat name") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (nameText.isNotBlank()) {
                                viewModel.updateName(nameText.trim())
                                editingName = false
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                TextButton(onClick = { editingName = true }) {
                    Text(
                        text = chat?.name ?: "Chat",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Edit name", modifier = Modifier.size(18.dp))
                }
            }

            // Background image
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { bgPicker.launch("image/*") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Image", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (chat?.backgroundFile != null) "Tap to change" else "Tap to set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (chat?.backgroundFile != null) {
                        val bgFile = File(chatDir, chat!!.backgroundFile!!)
                        if (bgFile.exists()) {
                            AsyncImage(
                                model = bgFile,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Appearance card (bubble color + gradient)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bubble color
                    Text("Bubble color", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val defaultContainerColor = MaterialTheme.colorScheme.primaryContainer
                    val allPresets = listOf(
                        null to defaultContainerColor,
                        "#DCF8C6" to Color(0xFFDCF8C6),
                        "#BBDEFB" to Color(0xFFBBDEFB),
                        "#F8BBD0" to Color(0xFFF8BBD0),
                        "#FFF9C4" to Color(0xFFFFF9C4),
                        "#005C4B" to Color(0xFF005C4B),
                        "#1A3A5C" to Color(0xFF1A3A5C),
                        "#4A1942" to Color(0xFF4A1942),
                        "#5D2E0C" to Color(0xFF5D2E0C)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allPresets.forEach { (hex, color) ->
                            val isSelected = chat?.bubbleColor == hex
                            val checkTint = if (color.luminance() > 0.4f) Color.Black else Color.White
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setBubbleColor(hex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(20.dp),
                                        tint = checkTint
                                    )
                                } else if (hex == null) {
                                    Text(
                                        "A",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bubble gradient toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Gradient,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bubble gradient", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Subtle gradient effect on message bubbles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = chat?.bubbleGradient == true,
                            onCheckedChange = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setBubbleGradient(it)
                            }
                        )
                    }
                }
            }

            // Tags
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Label, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Tags", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    var newTag by remember { mutableStateOf("") }
                    val currentTags = chat?.tags ?: emptyList()

                    // Existing tags as chips
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
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Add tag input
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            placeholder = { Text("Add tag") },
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
                            Icon(Icons.Default.Add, contentDescription = "Add tag")
                        }
                    }
                }
            }

            // Export
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    exportLauncher.launch("${chat?.name ?: "chat"}.zip")
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Export Chat", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Save as .zip file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Archive
            OutlinedButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.toggleArchive(onBack)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (chat?.isArchived == true) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (chat?.isArchived == true) "Unarchive Chat" else "Archive Chat")
            }

            // Delete
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showDeleteConfirm = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Chat")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chat") },
            text = { Text("Delete \"${chat?.name}\" and all its messages? This cannot be undone.") },
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
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
