package com.ncy.notechaty.data.model

/**
 * Gson allocates Kotlin data classes without invoking their constructors, so a
 * missing/null JSON field can bypass `emptyList()` and other constructor defaults.
 * Normalize portable data at read boundaries before it reaches repository or UI code.
 */
fun Message.normalizedForCompatibility(): Message = copy(
    content = content.orEmpty().filterNotNull().map { it.normalizedForCompatibility() },
    type = type ?: MessageType.TEXT,
    imageFiles = imageFiles.orEmpty().filterNotNull(),
    tags = tags.orEmpty().filterNotNull(),
    checklistItems = checklistItems.orEmpty().filterNotNull(),
    reactions = reactions.orEmpty().filterNotNull(),
    fileAttachments = fileAttachments.orEmpty().filterNotNull(),
)

fun RichTextBlock.normalizedForCompatibility(): RichTextBlock = copy(
    type = type ?: BlockType.PARAGRAPH,
    spans = spans.orEmpty().filterNotNull(),
)

fun ChatMetadata.normalizedForCompatibility(): ChatMetadata = copy(
    tags = tags.orEmpty().filterNotNull(),
)

fun ChatData.normalizedForCompatibility(): ChatData = copy(
    chat = chat.normalizedForCompatibility(),
    messages = messages.orEmpty().filterNotNull().map { it.normalizedForCompatibility() },
)
