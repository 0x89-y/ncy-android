package com.ncy.notechaty.ui.profiles

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncy.notechaty.data.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()

    var showAddProfile by remember { mutableStateOf(false) }
    var profileToManage by remember { mutableStateOf<Profile?>(null) }
    var renamingProfile by remember { mutableStateOf<Profile?>(null) }
    var deletingProfile by remember { mutableStateOf<Profile?>(null) }
    var profilePinSetup by remember { mutableStateOf<Profile?>(null) }
    var iconPickerProfile by remember { mutableStateOf<Profile?>(null) }

    // Re-fetch the live profile so dialogs reflect the latest state.
    val managedProfile = profileToManage?.let { p -> profiles.firstOrNull { it.id == p.id } }
    val iconPickerLive = iconPickerProfile?.let { p -> profiles.firstOrNull { it.id == p.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showAddProfile = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Each profile has its own chats and lock. Tap a profile to manage it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    profiles.forEach { profile ->
                        ProfileRow(
                            profile = profile,
                            isActive = profile.id == activeProfileId,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                profileToManage = profile
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddProfile) {
        ProfileNameDialog(
            title = "New profile",
            initialValue = "",
            onDismiss = { showAddProfile = false },
            onConfirm = { name ->
                viewModel.createProfile(name)
                showAddProfile = false
                Toast.makeText(context, "Profile created", Toast.LENGTH_SHORT).show()
            }
        )
    }

    managedProfile?.let { profile ->
        ProfileManageSheet(
            profile = profile,
            canDelete = profiles.size > 1,
            onDismiss = { profileToManage = null },
            onRename = {
                profileToManage = null
                renamingProfile = profile
            },
            onDelete = {
                profileToManage = null
                deletingProfile = profile
            },
            onSetPin = {
                profileToManage = null
                profilePinSetup = profile
            },
            onClearPin = {
                viewModel.clearProfilePin(profile.id)
                profileToManage = null
                Toast.makeText(context, "PIN removed", Toast.LENGTH_SHORT).show()
            },
            onPickIcon = {
                profileToManage = null
                iconPickerProfile = profile
            }
        )
    }

    iconPickerLive?.let { profile ->
        IconPickerDialog(
            currentKey = profile.iconKey ?: "person",
            onDismiss = { iconPickerProfile = null },
            onPick = { key ->
                viewModel.setProfileIcon(profile.id, key)
                iconPickerProfile = null
            }
        )
    }

    renamingProfile?.let { profile ->
        ProfileNameDialog(
            title = "Rename profile",
            initialValue = profile.name,
            onDismiss = { renamingProfile = null },
            onConfirm = { name ->
                viewModel.renameProfile(profile.id, name)
                renamingProfile = null
            }
        )
    }

    deletingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text("Delete \"${profile.name}\"?") },
            text = { Text("All chats and messages in this profile will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profile.id)
                        deletingProfile = null
                        Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) { Text("Cancel") }
            }
        )
    }

    profilePinSetup?.let { profile ->
        ProfilePinSetupDialog(
            isChange = profile.isProtected,
            onDismiss = { profilePinSetup = null },
            onConfirm = { newPin ->
                viewModel.setProfilePin(profile.id, newPin)
                profilePinSetup = null
                Toast.makeText(context, "PIN saved", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ProfileIcons.iconFor(profile.iconKey),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
                if (profile.isProtected) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Protected",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (isActive) "Active" else "Tap to manage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileManageSheet(
    profile: Profile,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSetPin: () -> Unit,
    onClearPin: () -> Unit,
    onPickIcon: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        ProfileIcons.iconFor(profile.iconKey),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(profile.name)
            }
        },
        text = {
            Column {
                ManageRow(icon = Icons.Default.Edit, label = "Rename", onClick = onRename)
                ManageRow(icon = Icons.Default.Palette, label = "Change icon", onClick = onPickIcon)
                ManageRow(
                    icon = Icons.Default.Lock,
                    label = if (profile.isProtected) "Change PIN" else "Set PIN",
                    onClick = onSetPin
                )
                if (profile.isProtected) {
                    ManageRow(
                        icon = Icons.Default.LockOpen,
                        label = "Remove PIN",
                        onClick = onClearPin
                    )
                }
                if (canDelete) {
                    ManageRow(
                        icon = Icons.Default.Delete,
                        label = "Delete profile",
                        onClick = onDelete,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ManageRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun IconPickerDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an icon") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                items(ProfileIcons.keys) { key ->
                    val selected = key == currentKey
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                            .clickable { onPick(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            ProfileIcons.iconFor(key),
                            contentDescription = key,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ProfileNameDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ProfilePinSetupDialog(
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
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(8.dp))
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
