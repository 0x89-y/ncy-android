package com.ncy.notechaty.ui.profiles

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
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
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyButtonShape
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.NcyInputSheet
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.NcySectionLabel
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import com.ncy.notechaty.ui.desktoptheme.ncyAvatarColor
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid

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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showAddProfile = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .ncyDottedGrid()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Each profile has its own chats and lock. Tap a profile to manage it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
            )

            NcySectionLabel("Profiles")
            NcyDividedCard {
                profiles.forEachIndexed { i, profile ->
                    if (i > 0) NcyRowDivider()
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
        NcyAlertDialog(
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
                .clip(NcyAvatarShape)
                .background(ncyAvatarColor(profile.name)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ProfileIcons.iconFor(profile.iconKey),
                contentDescription = null,
                tint = Color.White,
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

@OptIn(ExperimentalMaterial3Api::class)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            // Header — profile icon + name.
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(NcyAvatarShape)
                        .background(ncyAvatarColor(profile.name)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        ProfileIcons.iconFor(profile.iconKey),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            NcySheetActionRow(Icons.Default.Edit, "Rename", onClick = onRename)
            NcySheetActionRow(Icons.Default.Palette, "Change icon", onClick = onPickIcon)
            NcySheetActionRow(
                Icons.Default.Lock,
                if (profile.isProtected) "Change PIN" else "Set PIN",
                onClick = onSetPin
            )
            if (profile.isProtected) {
                NcySheetActionRow(Icons.Default.LockOpen, "Remove PIN", onClick = onClearPin)
            }
            if (canDelete) {
                NcySheetActionRow(
                    Icons.Default.Delete,
                    "Delete profile",
                    destructive = true,
                    onClick = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPickerDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
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
            Text("Pick an icon", style = MaterialTheme.typography.titleLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                items(ProfileIcons.keys) { key ->
                    val selected = key == currentKey
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(NcyAvatarShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onPick(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            ProfileIcons.iconFor(key),
                            contentDescription = key,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileNameDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    NcyInputSheet(
        title = title,
        label = "Profile name",
        confirmLabel = "Save",
        initialValue = initialValue,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
