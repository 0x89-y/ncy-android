package com.ncy.notechaty.ui.chatlist

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
import com.ncy.notechaty.ui.desktoptheme.ncyAvatarColor
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
        // Avatar, name and a dropdown arrow: the bare avatar left the screen without
        // a title, and nothing said which profile you were in.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClickLabel = stringResource(R.string.switch_profile)) { expanded = true }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Round profile avatar — colored circle + icon, matching the
                // Profiles screen (per-name hashed color).
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(NcyAvatarShape)
                        .background(ncyAvatarColor(active?.name ?: "?")),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ProfileIcons.iconFor(active?.iconKey ?: "person"),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (active?.isProtected == true) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 11.dp, y = 11.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(9.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = active?.name ?: "Ncy",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                                    contentDescription = stringResource(R.string.protected_label),
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
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_profile)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddProfile()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.manage_profiles)) },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onManageProfiles()
                }
            )
        }
    }

}
