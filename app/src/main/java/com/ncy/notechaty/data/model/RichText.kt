package com.ncy.notechaty.data.model

enum class BlockType {
    PARAGRAPH,
    HEADING1,
    HEADING2,
    HEADING3,
    BULLET_LIST,
    CODE_BLOCK
}

data class RichTextBlock(
    val type: BlockType = BlockType.PARAGRAPH,
    val spans: List<RichTextSpan> = emptyList()
)

data class RichTextSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: String? = null,
    val link: String? = null,
    val inlineImage: String? = null
)
