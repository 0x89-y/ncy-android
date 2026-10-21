package com.ncy.notechaty.ui.richtext

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ncy.notechaty.data.model.BlockType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormattingToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showBlockTypePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FormatToggle(
                icon = Icons.Default.FormatBold,
                label = stringResource(R.string.bold),
                isActive = state.isBold,
                onClick = { state.toggleBold() }
            )
            FormatToggle(
                icon = Icons.Default.FormatItalic,
                label = stringResource(R.string.italic),
                isActive = state.isItalic,
                onClick = { state.toggleItalic() }
            )
            FormatToggle(
                icon = Icons.Default.FormatUnderlined,
                label = stringResource(R.string.underline),
                isActive = state.isUnderline,
                onClick = { state.toggleUnderline() }
            )
            FormatToggle(
                icon = Icons.Default.FormatStrikethrough,
                label = stringResource(R.string.strikethrough),
                isActive = state.isStrikethrough,
                onClick = { state.toggleStrikethrough() }
            )

            Spacer(modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Block type selector
            FormatToggle(
                icon = Icons.Default.Title,
                label = stringResource(R.string.heading),
                isActive = state.currentBlockType in listOf(BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3),
                onClick = { showBlockTypePicker = !showBlockTypePicker }
            )
            FormatToggle(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = stringResource(R.string.list),
                isActive = state.currentBlockType == BlockType.BULLET_LIST,
                onClick = {
                    state.setBlockType(
                        if (state.currentBlockType == BlockType.BULLET_LIST) BlockType.PARAGRAPH
                        else BlockType.BULLET_LIST
                    )
                }
            )
            FormatToggle(
                icon = Icons.Default.Checklist,
                label = stringResource(R.string.checklist),
                isActive = false,
                onClick = { state.insertLinePrefix("- [ ] ") }
            )
            FormatToggle(
                icon = Icons.Default.Code,
                label = stringResource(R.string.code),
                isActive = state.currentBlockType == BlockType.CODE_BLOCK,
                onClick = {
                    state.setBlockType(
                        if (state.currentBlockType == BlockType.CODE_BLOCK) BlockType.PARAGRAPH
                        else BlockType.CODE_BLOCK
                    )
                }
            )

            Spacer(modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Color picker
            FormatToggle(
                icon = Icons.Default.FormatColorText,
                label = stringResource(R.string.color),
                isActive = state.currentColor != null,
                onClick = { showColorPicker = !showColorPicker }
            )
        }

        // Block type picker dropdown
        if (showBlockTypePicker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = state.currentBlockType == BlockType.PARAGRAPH,
                    onClick = { state.setBlockType(BlockType.PARAGRAPH); showBlockTypePicker = false },
                    label = { Text(stringResource(R.string.normal)) }
                )
                FilterChip(
                    selected = state.currentBlockType == BlockType.HEADING1,
                    onClick = { state.setBlockType(BlockType.HEADING1); showBlockTypePicker = false },
                    label = { Text(stringResource(R.string.h1), fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = state.currentBlockType == BlockType.HEADING2,
                    onClick = { state.setBlockType(BlockType.HEADING2); showBlockTypePicker = false },
                    label = { Text(stringResource(R.string.h2), fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = state.currentBlockType == BlockType.HEADING3,
                    onClick = { state.setBlockType(BlockType.HEADING3); showBlockTypePicker = false },
                    label = { Text(stringResource(R.string.h3), fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Color picker
        if (showColorPicker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colors = listOf(
                    null to stringResource(R.string.color_default),
                    "#FF0000" to stringResource(R.string.color_red),
                    "#0000FF" to stringResource(R.string.color_blue),
                    "#008000" to stringResource(R.string.color_green),
                    "#FFA500" to stringResource(R.string.color_orange),
                    "#800080" to stringResource(R.string.color_purple),
                    "#FF1493" to stringResource(R.string.color_pink),
                    "#008B8B" to stringResource(R.string.color_teal)
                )
                for ((hex, name) in colors) {
                    FilterChip(
                        selected = state.currentColor == hex,
                        onClick = {
                            state.setColor(hex)
                            showColorPicker = false
                        },
                        label = { Text(name) },
                        leadingIcon = if (hex != null) {
                            {
                                Surface(
                                    modifier = Modifier.size(12.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = Color(android.graphics.Color.parseColor(hex))
                                ) {}
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatToggle(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp)
        )
    }
}
