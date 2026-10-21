package com.ncy.notechaty.ui.desktoptheme

/*
 * Part of the deletable "Desktop" reskin — see DesktopTheme.kt.
 * Shared layout primitives that reproduce the desktop's card-less look:
 * bordered "divided-list" containers, hairline separators, mono section
 * labels, and the settings-style rows/panels. Used by the chat list,
 * settings, chat-config and profiles screens.
 */

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flat, desktop-style button corner (matches --radius, 10dp). Material buttons
 * default to a full stadium pill, which clashes with the app's tighter corners.
 * Pass as `shape = NcyButtonShape` on Button/OutlinedButton/etc.
 */
val NcyButtonShape = RoundedCornerShape(10.dp)

/**
 * Rounded-square avatar/icon shape — ncy-desktop uses `border-radius: size * 0.28`
 * for avatars, i.e. a 28% corner. Use for chat avatars and profile icons.
 */
val NcyAvatarShape = RoundedCornerShape(percent = 28)

/** Dialog corner — tighter than Material's default 28dp, matching the app's cards. */
val NcyDialogShape = RoundedCornerShape(16.dp)

/**
 * Flat AlertDialog matching the reskin: tighter corners + a plain surface
 * container instead of Material's tonal elevated one. Drop-in for AlertDialog —
 * same named params.
 */
@Composable
fun NcyAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = NcyDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/** Bordered, rounded container that clips its rows — the desktop `.divided-list`. */
@Composable
fun NcyDividedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

/** Hairline separator between rows inside an [NcyDividedCard]. */
@Composable
fun NcyRowDivider() {
    HorizontalDivider(
        thickness = Dp.Hairline,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** Mono uppercase section label, matching the desktop's group headings. */
@Composable
fun NcySectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 6.dp, bottom = 2.dp)
    )
}

/** A titled section: mono label above a bordered divided-list card. */
@Composable
fun NcySettingsSection(
    /** Null on a page whose title already names the section, so it is not said twice. */
    label: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { NcySectionLabel(it) }
        NcyDividedCard(content = content)
    }
}

/**
 * One option of a single-choice group. Selected options always show a check, so
 * every choice group in the app reads the same way, not only some of them.
 */
@Composable
fun NcyChoiceChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
    )
}

/** Free-form content block inside a section card (chips, pickers, inputs). */
@Composable
fun NcySettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        content = content
    )
}

/** Clickable row with a leading icon and title/subtitle. */
@Composable
fun NcySettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
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
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Single-field input bottom sheet (create/rename flows). Presents a title, one
 * text field, and a primary confirm button; disabled until non-blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcyInputSheet(
    title: String,
    label: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialValue: String = "",
    /** Extra controls between the field and the button, e.g. where a chat goes. */
    extra: (@Composable () -> Unit)? = null,
) {
    var value by remember { mutableStateOf(initialValue) }
    // The sheet exists to take a name, so the field takes focus (and the keyboard
    // opens) at once; it used to wait for a second tap.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (value.isNotBlank()) onConfirm(value.trim())
                }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            extra?.invoke()
            Button(
                onClick = { onConfirm(value.trim()) },
                enabled = value.trim().isNotEmpty(),
                shape = NcyButtonShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(confirmLabel)
            }
        }
    }
}

/** Full-width action row for a bottom sheet — leading icon + label. */
@Composable
fun NcySheetActionRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** Row with a leading icon, title/subtitle, and a trailing switch. */
@Composable
fun NcySettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
