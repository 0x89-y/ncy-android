package com.ncy.notechaty.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ncy.notechaty.R
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.ui.theme.NcyAvatarShape
import com.ncy.notechaty.ui.theme.NcySheetActionRow
import com.ncy.notechaty.ui.theme.ncyAvatarColor
import com.ncy.notechaty.ui.profiles.ProfileIcons

/**
 * The top bar's title: whose chats are listed — one profile, or all of them. Tapping
 * it opens [ProfileSwitcherSheet].
 */
@Composable
fun ProfileSwitcherButton(
    activeProfile: Profile?,
    showAll: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = stringResource(R.string.switch_profile), onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        if (showAll) AllChatsBadge(size = 32.dp) else ProfileAvatar(activeProfile, size = 32.dp)
        Text(
            text = if (showAll) stringResource(R.string.all_chats) else activeProfile?.name ?: stringResource(R.string.ncy),
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
}

/**
 * Account-style switcher: "All chats" first, then each profile with its chat count,
 * then adding and managing profiles. Replaces a plain dropdown that showed names only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    profiles: List<Profile>,
    activeProfileId: String,
    showAll: Boolean,
    /** Chats per profile id; null while still being counted. */
    chatCounts: Map<String, Int>?,
    onDismiss: () -> Unit,
    onShowAll: () -> Unit,
    onProfileSelected: (String) -> Unit,
    onAddProfile: () -> Unit,
    onManageProfiles: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Fully open at once: half-open, the lower profiles were below the fold.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = com.ncy.notechaty.ui.theme.NcySheetShape,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            com.ncy.notechaty.ui.theme.NcySheetTitle(stringResource(R.string.profiles))
            if (profiles.size > 1) {
                val total = chatCounts?.values?.sum()
                SwitcherRow(
                    selected = showAll,
                    leading = { AllChatsBadge(size = 40.dp) },
                    title = stringResource(R.string.all_chats),
                    subtitle = total?.let {
                        pluralStringResource(R.plurals.chats_across_profiles, it, it, profiles.size)
                    },
                    onClick = onShowAll,
                )
            }
            profiles.forEach { profile ->
                val count = chatCounts?.get(profile.id)
                SwitcherRow(
                    selected = !showAll && profile.id == activeProfileId,
                    leading = { ProfileAvatar(profile, size = 40.dp) },
                    title = profile.name,
                    subtitle = listOfNotNull(
                        count?.let { pluralStringResource(R.plurals.chat_count, it, it) },
                        if (profile.isProtected) stringResource(R.string.protected_label) else null,
                    ).joinToString(" · ").ifEmpty { null },
                    onClick = { onProfileSelected(profile.id) },
                )
            }
            Spacer(Modifier.padding(top = 6.dp))
            com.ncy.notechaty.ui.theme.NcyRowDivider()
            Spacer(Modifier.padding(top = 6.dp))
            NcySheetActionRow(Icons.Default.Add, stringResource(R.string.add_profile), onClick = onAddProfile)
            NcySheetActionRow(Icons.Default.Settings, stringResource(R.string.manage_profiles), onClick = onManageProfiles)
        }
    }
}

@Composable
private fun SwitcherRow(
    selected: Boolean,
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    // The current profile: a stronger accent tint, an accent bar and a bold name,
    // like the open chat and the active settings section. No check mark.
    val c = com.ncy.notechaty.ui.theme.NcyTheme.colors
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.background(c.accentSelected) else Modifier)
            .then(
                if (selected) Modifier.drawBehind {
                    val bar = 3.dp.toPx()
                    drawRect(accent, topLeft = androidx.compose.ui.geometry.Offset(0f, 8.dp.toPx()), size = androidx.compose.ui.geometry.Size(bar, size.height - 16.dp.toPx()))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold,
                ),
                color = if (selected) c.onAccentSoft else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

    }
}

/**
 * Picks one profile in a form, such as where a new chat goes. A single field
 * however many profiles there are, unlike a row of chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdownField(
    profiles: List<Profile>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.create_in_profile)) },
            leadingIcon = { Box(Modifier.padding(start = 12.dp)) { ProfileAvatar(selected, size = 26.dp) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { ProfileAvatar(profile, size = 26.dp) },
                    trailingIcon = if (profile.id == selected?.id) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        onSelected(profile.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Coloured rounded square with the profile's icon, and a lock badge if protected. */
@Composable
fun ProfileAvatar(profile: Profile?, size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        AvatarTile(
            icon = ProfileIcons.iconFor(profile?.iconKey ?: "person"),
            color = ncyAvatarColor(profile?.name ?: "?"),
            size = size,
        )
        if (profile?.isProtected == true) {
            val badge = size * 0.44f
            Box(
                modifier = Modifier
                    .size(badge)
                    .offset(x = size * 0.36f, y = size * 0.36f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(badge * 0.64f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllChatsBadge(size: Dp) {
    com.ncy.notechaty.ui.theme.NcyInkTile(size = size) {
        Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(size * 0.52f))
    }
}

/** A profile's icon on a paper tint of its colour, like a chat's tile. */
@Composable
private fun AvatarTile(icon: ImageVector, color: Color, size: Dp) {
    com.ncy.notechaty.ui.theme.NcyTile(hue = color, size = size) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.52f))
    }
}
