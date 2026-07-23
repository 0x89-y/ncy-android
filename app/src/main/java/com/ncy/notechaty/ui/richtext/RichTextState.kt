package com.ncy.notechaty.ui.richtext

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.ncy.notechaty.data.model.BlockType
import com.ncy.notechaty.data.model.RichTextBlock
import com.ncy.notechaty.data.model.RichTextSpan

class RichTextState {
    var textFieldValue by mutableStateOf(TextFieldValue())
        private set

    var isBold by mutableStateOf(false)
    var isItalic by mutableStateOf(false)
    var isUnderline by mutableStateOf(false)
    var isStrikethrough by mutableStateOf(false)
    var currentColor by mutableStateOf<String?>(null)
    var currentBlockType by mutableStateOf(BlockType.PARAGRAPH)

    // Store spans with their character ranges
    private val spanRanges = mutableListOf<SpanRange>()

    data class SpanRange(
        val start: Int,
        val end: Int,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val color: String? = null,
        val link: String? = null
    )

    fun updateTextField(newValue: TextFieldValue) {
        textFieldValue = newValue
    }

    fun toggleBold() { isBold = !isBold }
    fun toggleItalic() { isItalic = !isItalic }
    fun toggleUnderline() { isUnderline = !isUnderline }
    fun toggleStrikethrough() { isStrikethrough = !isStrikethrough }

    fun setBlockType(type: BlockType) {
        currentBlockType = type
    }

    fun setColor(color: String?) {
        currentColor = color
    }

    fun insertLinePrefix(prefix: String) {
        val value = textFieldValue
        val lineStart = value.text.lastIndexOf('\n', (value.selection.start - 1).coerceAtLeast(0)) + 1
        textFieldValue = value.copy(
            text = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart),
            selection = TextRange(value.selection.start + prefix.length, value.selection.end + prefix.length)
        )
    }

    fun clear() {
        textFieldValue = TextFieldValue()
        isBold = false
        isItalic = false
        isUnderline = false
        isStrikethrough = false
        currentColor = null
        currentBlockType = BlockType.PARAGRAPH
        spanRanges.clear()
    }

    fun loadFromBlocks(blocks: List<RichTextBlock>) {
        val fullText = StringBuilder()
        spanRanges.clear()

        if (blocks.isNotEmpty()) {
            currentBlockType = blocks.first().type
        }

        for ((blockIndex, block) in blocks.withIndex()) {
            if (blockIndex > 0) fullText.append('\n')
            if (block.type == BlockType.BULLET_LIST) fullText.append("- ")
            for (span in block.spans) {
                if (span.inlineImage != null) continue
                val start = fullText.length
                fullText.append(span.text)
                val end = fullText.length
                if (start < end) {
                    spanRanges.add(
                        SpanRange(
                            start = start,
                            end = end,
                            bold = span.bold,
                            italic = span.italic,
                            underline = span.underline,
                            strikethrough = span.strikethrough,
                            color = span.color,
                            link = span.link
                        )
                    )
                }
            }
        }

        textFieldValue = TextFieldValue(
            text = fullText.toString(),
            selection = TextRange(fullText.length)
        )
    }

    fun toBlocks(): List<RichTextBlock> {
        val text = textFieldValue.text
        if (text.isEmpty()) return emptyList()

        // Split by lines for multi-block support
        val lines = text.split("\n")
        val blocks = mutableListOf<RichTextBlock>()
        var offset = 0

        for ((index, line) in lines.withIndex()) {
            if (line.isEmpty() && index < lines.size - 1) {
                offset += 1 // account for newline
                continue
            }

            val checklistPrefixLength = when {
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ") -> 2
                else -> 0
            }
            val storedLine = line.drop(checklistPrefixLength)
            val lineStart = offset + checklistPrefixLength
            val lineEnd = offset + line.length

            // Find spans that overlap this line
            val lineSpans = mutableListOf<RichTextSpan>()
            val relevantRanges = spanRanges.filter { it.start < lineEnd && it.end > lineStart }

            if (relevantRanges.isEmpty()) {
                // No specific formatting — use current formatting state
                lineSpans.add(
                    RichTextSpan(
                        text = storedLine,
                        bold = isBold,
                        italic = isItalic,
                        underline = isUnderline,
                        strikethrough = isStrikethrough,
                        color = currentColor
                    )
                )
            } else {
                var pos = lineStart
                for (range in relevantRanges.sortedBy { it.start }) {
                    val spanStart = maxOf(range.start, lineStart)
                    val spanEnd = minOf(range.end, lineEnd)

                    // Add unformatted text before this span
                    if (pos < spanStart) {
                        lineSpans.add(RichTextSpan(text = text.substring(pos, spanStart)))
                    }

                    lineSpans.add(
                        RichTextSpan(
                            text = text.substring(spanStart, spanEnd),
                            bold = range.bold,
                            italic = range.italic,
                            underline = range.underline,
                            strikethrough = range.strikethrough,
                            color = range.color,
                            link = range.link
                        )
                    )
                    pos = spanEnd
                }

                // Remaining text after last span
                if (pos < lineEnd) {
                    lineSpans.add(RichTextSpan(text = text.substring(pos, lineEnd)))
                }
            }

            blocks.add(
                RichTextBlock(
                    type = if (checklistPrefixLength > 0) BlockType.BULLET_LIST else currentBlockType,
                    spans = lineSpans
                )
            )

            offset = lineEnd + 1 // +1 for newline
        }

        return blocks.ifEmpty {
            listOf(
                RichTextBlock(
                    type = currentBlockType,
                    spans = listOf(RichTextSpan(text = text))
                )
            )
        }
    }
}
