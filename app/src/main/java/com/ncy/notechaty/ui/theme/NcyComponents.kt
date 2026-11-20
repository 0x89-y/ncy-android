package com.ncy.notechaty.ui.theme

/*
 * Shared Notebook building blocks: hairline-divided lists on paper, mono section
 * labels, settings rows, chips, the ink button and sheets. Screens build from
 * these so the app keeps one voice.
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Button corner. */
val NcyButtonShape = RoundedCornerShape(14.dp)

/** Dialog corner. */
val NcyDialogShape = RoundedCornerShape(22.dp)

/** Corner for sheets anchored to the bottom edge. */
val NcySheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/** Side inset every list row and label lines up with. */
val NcyGutter = 20.dp

// ---------------------------------------------------------------------------
//  Dialogs and sheets
// ---------------------------------------------------------------------------

/** AlertDialog on the sheet surface with the Notebook corner. Same params as AlertDialog. */
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.primary,
    )
}

/** Serif title at the top of a bottom sheet. */
@Composable
fun NcySheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
    )
}

/**
 * Single-field input bottom sheet (create/rename flows): a title, one text field
 * and the ink confirm button, disabled until the field has text.
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
    // opens) at once.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = NcySheetShape) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NcyGutter)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                shape = NcyButtonShape,
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
            NcyInkButton(
                text = confirmLabel,
                onClick = { onConfirm(value.trim()) },
                enabled = value.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Full-width action row for a bottom sheet: leading icon, label, optional hint. */
@Composable
fun NcySheetActionRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    hint: String? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint ?: color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        if (hint != null) {
            Text(hint, style = NcyTheme.type.time, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
//  Lists
// ---------------------------------------------------------------------------

/** Bordered, rounded container that clips its rows: for cards inside a page. */
@Composable
fun NcyDividedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, NcyTheme.colors.lineStrong, shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

/** Rows running edge to edge on the paper, closed off by a hairline at each end. */
@Composable
fun NcyFlushList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NcyRowDivider()
        content()
        NcyRowDivider()
    }
}

/** [NcySectionLabel] above a list, lined up with the rows' text. */
@Composable
fun NcyFlushSectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = NcyGutter, end = 8.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcySectionLabel(text, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Hairline between rows; [inset] indents it past a leading icon or tile. */
@Composable
fun NcyRowDivider(inset: Dp = 0.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = inset),
        thickness = 1.dp,
        color = NcyTheme.colors.line,
    )
}

/** Mono uppercase section label. */
@Composable
fun NcySectionLabel(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Text(
        text = text.uppercase(),
        style = NcyTheme.type.label,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Big serif page title, e.g. "Chats" or "Settings". */
@Composable
fun NcyPageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = NcyTheme.type.pageTitle,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(horizontal = NcyGutter),
    )
}

/** A titled settings section: mono label above rows on the paper. */
@Composable
fun NcySettingsSection(
    /** Null on a page whose title already names the section, so it is not said twice. */
    label: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        if (label != null) NcyFlushSectionLabel(label)
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** Free-form content block inside a section (chips, pickers, inputs). */
@Composable
fun NcySettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NcyGutter, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun SettingsRowBody(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    destructive: Boolean,
    trailing: @Composable RowScope.() -> Unit,
    modifier: Modifier,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = NcyGutter, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}

/** Clickable row: leading icon, title and subtitle, then a value or any trailing content. */
@Composable
fun NcySettingsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    /** Short current value shown in mono at the end, e.g. "ON" or "100 MB". */
    value: String? = null,
    destructive: Boolean = false,
) {
    SettingsRowBody(
        icon = icon, title = title, subtitle = subtitle, destructive = destructive,
        trailing = {
            if (value != null) {
                Spacer(Modifier.width(12.dp))
                Text(value.uppercase(), style = NcyTheme.type.time, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        },
        modifier = Modifier.clickable(onClick = onClick, role = Role.Button),
    )
}

/** Row with a trailing switch; the whole row toggles it. */
@Composable
fun NcySettingsToggleRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRowBody(
        icon = icon, title = title, subtitle = subtitle, destructive = false,
        trailing = {
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, enabled = enabled, onCheckedChange = null)
        },
        modifier = Modifier.clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) },
    )
}

// ---------------------------------------------------------------------------
//  Chips, buttons, segmented control
// ---------------------------------------------------------------------------

/**
 * Pill chip. A selected chip takes the accent tint and bolder text, so the choice
 * reads by fill and weight, not by colour alone.
 */
@Composable
fun NcyPill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: Int? = null,
) {
    val c = NcyTheme.colors
    val fg = if (selected) c.onAccentSoft else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(c.accentSelected).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier.border(1.dp, c.lineStrong, CircleShape)
            )
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = fg,
            maxLines = 1,
        )
        if (count != null) {
            Text(
                count.toString(),
                style = NcyTheme.type.time,
                color = if (selected) c.onAccentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One option of a single-choice group. */
@Composable
fun NcyChoiceChip(selected: Boolean, onClick: () -> Unit, label: String) {
    NcyPill(selected = selected, onClick = onClick, label = label)
}

/** The primary action in ink. */
@Composable
fun NcyInkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = NcyTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = NcyButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.ink,
            contentColor = c.onInk,
            disabledContainerColor = c.lineStrong,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Secondary action: hairline border on paper. */
@Composable
fun NcyLineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = NcyButtonShape,
        border = BorderStroke(1.dp, NcyTheme.colors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Compact segmented control, e.g. Auto / Light / Dark. */
@Composable
fun <T> NcySegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = NcyTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, c.lineStrong, shape)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .then(if (on) Modifier.background(c.accentSoft) else Modifier)
                    .selectable(selected = on, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (on) c.onAccentSoft else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** Small "online" / "away" status with its dot. */
@Composable
fun NcyPresence(online: Boolean, text: String, modifier: Modifier = Modifier) {
    val color = if (online) NcyTheme.colors.online else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .then(if (online) Modifier.background(color) else Modifier.border(1.5.dp, color, CircleShape))
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = NcyTheme.type.time, color = color)
    }
}

/** Vertical breathing room between page sections. */
@Composable
fun NcyGap(height: Dp = 8.dp) = Spacer(Modifier.height(height))
