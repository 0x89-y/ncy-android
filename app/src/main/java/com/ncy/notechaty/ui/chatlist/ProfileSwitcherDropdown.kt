package com.ncy.notechaty.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.ui.profiles.ProfileIcons

@Composable
fun ProfileSwitcherDropdown(
    profiles: List<Profile>,
    activeProfileId: String,
    onProfileSelected: (String) -> Unit,
    onAddProfile: () -> Unit,
    onManageProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val active = profiles.firstOrNull { it.id == activeProfileId }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ProfileIcons.iconFor(active?.iconKey ?: "person"),
                    contentDescription = active?.name ?: "Switch profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (active?.isProtected == true) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .offset(x = 10.dp, y = 10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            ProfileIcons.iconFor(profile.iconKey),
                            contentDescription = null
                        )
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = profile.name,
                                fontWeight = if (profile.id == activeProfileId)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (profile.isProtected) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Protected",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onProfileSelected(profile.id)
                    }
                )
            }
            Divider()
            DropdownMenuItem(
                text = { Text("Add profile") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddProfile()
                }
            )
            DropdownMenuItem(
                text = { Text("Manage profiles") },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onManageProfiles()
                }
            )
        }
    }

}
