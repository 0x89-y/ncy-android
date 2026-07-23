package com.ncy.notechaty.ui.richtext

import com.ncy.notechaty.data.model.BlockType
import com.ncy.notechaty.data.model.RichTextBlock
import com.ncy.notechaty.data.model.RichTextSpan
import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextStateTest {
    @Test
    fun checklistBlocksSurviveUnchangedEditRoundTrip() {
        val original = listOf("1", "2", "3").map { item ->
            RichTextBlock(
                type = BlockType.BULLET_LIST,
                spans = listOf(RichTextSpan(text = "[ ] $item")),
            )
        }
        val state = RichTextState()

        state.loadFromBlocks(original)

        assertEquals("- [ ] 1\n- [ ] 2\n- [ ] 3", state.textFieldValue.text)
        assertEquals(original, state.toBlocks())
    }

    @Test
    fun markdownTaskPrefixesBecomeInteractiveChecklistBlocks() {
        val state = RichTextState()
        state.updateTextField(androidx.compose.ui.text.input.TextFieldValue("- [ ] 1\n- [x] 2"))

        assertEquals(
            listOf(
                RichTextBlock(BlockType.BULLET_LIST, listOf(RichTextSpan(text = "[ ] 1"))),
                RichTextBlock(BlockType.BULLET_LIST, listOf(RichTextSpan(text = "[x] 2"))),
            ),
            state.toBlocks(),
        )
    }
}
