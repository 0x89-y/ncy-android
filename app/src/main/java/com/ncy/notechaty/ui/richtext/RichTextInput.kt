package com.ncy.notechaty.ui.richtext

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.model.RichTextBlock

@Composable
fun RichTextInput(
    editingMessage: Message?,
    onSend: (List<RichTextBlock>) -> Unit,
    onCancelEdit: () -> Unit,
    onVoiceClick: () -> Unit,
    onImageClick: () -> Unit,
    onFileClick: () -> Unit,
    onSendChecklist: (List<String>) -> Unit = {},
    onCameraClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = remember { RichTextState() }
    val view = LocalView.current
    var showToolbar by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var checklistMode by remember { mutableStateOf(false) }
    var checklistItems by remember { mutableStateOf(listOf("")) }
    val checklistFocusRequesters = remember(checklistItems.size) {
        List(checklistItems.size) { FocusRequester() }
    }

    // Load editing message content
    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            if (editingMessage.type == MessageType.CHECKLIST) {
                checklistItems = editingMessage.checklistItems.map { it.text }.ifEmpty { listOf("") }
                checklistMode = true
            } else {
                state.loadFromBlocks(editingMessage.content)
                showToolbar = true
            }
        } else {
            if (checklistMode) {
                checklistMode = false
                checklistItems = listOf("")
            }
        }
    }

    // Auto-focus last checklist item when a new one is added
    LaunchedEffect(checklistItems.size) {
        if (checklistMode && checklistItems.isNotEmpty()) {
            checklistFocusRequesters.last().requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Editing indicator (hidden when checklist edit is active — checklist banner covers it)
        if (editingMessage != null && !checklistMode) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.editing_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onCancelEdit()
                            state.clear()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel_edit),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Checklist mode indicator
        if (checklistMode) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (editingMessage != null) stringResource(R.string.editing_checklist) else stringResource(R.string.checklist),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (editingMessage != null) {
                                onCancelEdit()
                                // LaunchedEffect(editingMessage) resets checklistMode + checklistItems
                            } else {
                                checklistMode = false
                                checklistItems = listOf("")
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel_checklist),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Formatting toolbar
        if (showToolbar && !checklistMode) {
            FormattingToolbar(state = state)
        }

        // Input row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editingMessage == null) {
                    Box {
                        FilledIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showAttachMenu = true
                            },
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.camera)) },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                onClick = { showAttachMenu = false; onCameraClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.gallery)) },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                onClick = { showAttachMenu = false; onImageClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.file)) },
                                leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                onClick = { showAttachMenu = false; onFileClick() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                // Main input field area (rounded filled)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    if (checklistMode) {
                        Column(
                            modifier = Modifier
                                .heightIn(min = 44.dp, max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            checklistItems.forEachIndexed { index, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val isLast = index == checklistItems.lastIndex
                                    TextField(
                                        value = item,
                                        onValueChange = { newText ->
                                            checklistItems = checklistItems.toMutableList().also { it[index] = newText }
                                        },
                                        placeholder = {
                                            Text(
                                                if (index == 0 && checklistItems.size == 1) stringResource(R.string.add_item) else stringResource(R.string.item, index + 1),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                checklistItems = checklistItems + ""
                                            },
                                            onDone = {
                                                if (item.isNotBlank()) {
                                                    checklistItems = checklistItems + ""
                                                }
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(checklistFocusRequesters[index]),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = MaterialTheme.colorScheme.primary
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge
                                    )
                                    if (checklistItems.size > 1) {
                                        IconButton(
                                            onClick = {
                                                checklistItems = checklistItems.toMutableList().also { it.removeAt(index) }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.remove_item),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Text field (no border, transparent background)
                            TextField(
                                value = state.textFieldValue,
                                onValueChange = { state.updateTextField(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp, max = 150.dp),
                                placeholder = {
                                    Text(
                                        stringResource(R.string.message),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                maxLines = 6
                            )
                            // Formatting lives in the field it formats, not among the
                            // attachments in the + menu.
                            IconButton(
                                onClick = { showToolbar = !showToolbar },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.TextFormat,
                                    contentDescription = if (showToolbar) stringResource(R.string.hide_formatting) else stringResource(R.string.formatting),
                                    tint = if (showToolbar) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send or Add button (circular) with animated transition
                val showSend = checklistMode || state.textFieldValue.text.isNotEmpty()
                AnimatedContent(
                    targetState = showSend,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.7f) + fadeIn())
                            .togetherWith(scaleOut(targetScale = 0.7f) + fadeOut())
                    },
                    label = "send_mic_toggle"
                ) { isSend ->
                    if (isSend) {
                        FilledIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (checklistMode) {
                                    val items = checklistItems.filter { it.isNotBlank() }
                                    if (items.isNotEmpty()) {
                                        onSendChecklist(items)
                                        checklistMode = false
                                        checklistItems = listOf("")
                                    }
                                } else {
                                    val blocks = state.toBlocks()
                                    onSend(blocks)
                                    state.clear()
                                    showToolbar = false
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.send),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (editingMessage == null) {
                        // An empty composer offers a voice note where Send will be,
                        // as messengers do; it used to sit at the bottom of the + menu.
                        FilledTonalIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onVoiceClick()
                            },
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice_message),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }
            }
        }
    }
}
