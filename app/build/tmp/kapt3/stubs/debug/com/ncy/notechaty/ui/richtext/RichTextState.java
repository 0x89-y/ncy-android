package com.ncy.notechaty.ui.richtext;

import androidx.compose.ui.text.input.TextFieldValue;
import com.ncy.notechaty.data.model.BlockType;
import com.ncy.notechaty.data.model.RichTextBlock;
import com.ncy.notechaty.data.model.RichTextSpan;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u000f\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\r\u0018\u00002\u00020\u0001:\u0001=B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010,\u001a\u00020-J\u0014\u0010.\u001a\u00020-2\f\u0010/\u001a\b\u0012\u0004\u0012\u00020100J\u000e\u00102\u001a\u00020-2\u0006\u00103\u001a\u00020\u0004J\u0010\u00104\u001a\u00020-2\b\u00105\u001a\u0004\u0018\u00010\fJ\f\u00106\u001a\b\u0012\u0004\u0012\u00020100J\u0006\u00107\u001a\u00020-J\u0006\u00108\u001a\u00020-J\u0006\u00109\u001a\u00020-J\u0006\u0010:\u001a\u00020-J\u000e\u0010;\u001a\u00020-2\u0006\u0010<\u001a\u00020%R+\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0003\u001a\u00020\u00048F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\n\u0010\u000b\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR/\u0010\r\u001a\u0004\u0018\u00010\f2\b\u0010\u0003\u001a\u0004\u0018\u00010\f8F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0012\u0010\u000b\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011R+\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0003\u001a\u00020\u00138F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0018\u0010\u000b\u001a\u0004\b\u0014\u0010\u0015\"\u0004\b\u0016\u0010\u0017R+\u0010\u0019\u001a\u00020\u00132\u0006\u0010\u0003\u001a\u00020\u00138F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u001b\u0010\u000b\u001a\u0004\b\u0019\u0010\u0015\"\u0004\b\u001a\u0010\u0017R+\u0010\u001c\u001a\u00020\u00132\u0006\u0010\u0003\u001a\u00020\u00138F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u001e\u0010\u000b\u001a\u0004\b\u001c\u0010\u0015\"\u0004\b\u001d\u0010\u0017R+\u0010\u001f\u001a\u00020\u00132\u0006\u0010\u0003\u001a\u00020\u00138F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b!\u0010\u000b\u001a\u0004\b\u001f\u0010\u0015\"\u0004\b \u0010\u0017R\u0014\u0010\"\u001a\b\u0012\u0004\u0012\u00020$0#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R+\u0010&\u001a\u00020%2\u0006\u0010\u0003\u001a\u00020%8F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b+\u0010\u000b\u001a\u0004\b\'\u0010(\"\u0004\b)\u0010*\u00a8\u0006>"}, d2 = {"Lcom/ncy/notechaty/ui/richtext/RichTextState;", "", "()V", "<set-?>", "Lcom/ncy/notechaty/data/model/BlockType;", "currentBlockType", "getCurrentBlockType", "()Lcom/ncy/notechaty/data/model/BlockType;", "setCurrentBlockType", "(Lcom/ncy/notechaty/data/model/BlockType;)V", "currentBlockType$delegate", "Landroidx/compose/runtime/MutableState;", "", "currentColor", "getCurrentColor", "()Ljava/lang/String;", "setCurrentColor", "(Ljava/lang/String;)V", "currentColor$delegate", "", "isBold", "()Z", "setBold", "(Z)V", "isBold$delegate", "isItalic", "setItalic", "isItalic$delegate", "isStrikethrough", "setStrikethrough", "isStrikethrough$delegate", "isUnderline", "setUnderline", "isUnderline$delegate", "spanRanges", "", "Lcom/ncy/notechaty/ui/richtext/RichTextState$SpanRange;", "Landroidx/compose/ui/text/input/TextFieldValue;", "textFieldValue", "getTextFieldValue", "()Landroidx/compose/ui/text/input/TextFieldValue;", "setTextFieldValue", "(Landroidx/compose/ui/text/input/TextFieldValue;)V", "textFieldValue$delegate", "clear", "", "loadFromBlocks", "blocks", "", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "setBlockType", "type", "setColor", "color", "toBlocks", "toggleBold", "toggleItalic", "toggleStrikethrough", "toggleUnderline", "updateTextField", "newValue", "SpanRange", "app_debug"})
public final class RichTextState {
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState textFieldValue$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isBold$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isItalic$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isUnderline$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState isStrikethrough$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState currentColor$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState currentBlockType$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.ui.richtext.RichTextState.SpanRange> spanRanges = null;
    
    public RichTextState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.ui.text.input.TextFieldValue getTextFieldValue() {
        return null;
    }
    
    private final void setTextFieldValue(androidx.compose.ui.text.input.TextFieldValue p0) {
    }
    
    public final boolean isBold() {
        return false;
    }
    
    public final void setBold(boolean p0) {
    }
    
    public final boolean isItalic() {
        return false;
    }
    
    public final void setItalic(boolean p0) {
    }
    
    public final boolean isUnderline() {
        return false;
    }
    
    public final void setUnderline(boolean p0) {
    }
    
    public final boolean isStrikethrough() {
        return false;
    }
    
    public final void setStrikethrough(boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCurrentColor() {
        return null;
    }
    
    public final void setCurrentColor(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.BlockType getCurrentBlockType() {
        return null;
    }
    
    public final void setCurrentBlockType(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.BlockType p0) {
    }
    
    public final void updateTextField(@org.jetbrains.annotations.NotNull()
    androidx.compose.ui.text.input.TextFieldValue newValue) {
    }
    
    public final void toggleBold() {
    }
    
    public final void toggleItalic() {
    }
    
    public final void toggleUnderline() {
    }
    
    public final void toggleStrikethrough() {
    }
    
    public final void setBlockType(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.BlockType type) {
    }
    
    public final void setColor(@org.jetbrains.annotations.Nullable()
    java.lang.String color) {
    }
    
    public final void clear() {
    }
    
    public final void loadFromBlocks(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.RichTextBlock> blocks) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.RichTextBlock> toBlocks() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u001b\b\u0086\b\u0018\u00002\u00020\u0001BU\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0006\u0012\b\b\u0002\u0010\b\u001a\u00020\u0006\u0012\b\b\u0002\u0010\t\u001a\u00020\u0006\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\u0002\u0010\rJ\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010\u001f\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J\u000b\u0010 \u001a\u0004\u0018\u00010\u000bH\u00c6\u0003J]\u0010!\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u00062\b\b\u0002\u0010\t\u001a\u00020\u00062\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u000bH\u00c6\u0001J\u0013\u0010\"\u001a\u00020\u00062\b\u0010#\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010$\u001a\u00020\u0003H\u00d6\u0001J\t\u0010%\u001a\u00020\u000bH\u00d6\u0001R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0013\u0010\n\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000fR\u0013\u0010\f\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0011R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0013R\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u000fR\u0011\u0010\b\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u000f\u00a8\u0006&"}, d2 = {"Lcom/ncy/notechaty/ui/richtext/RichTextState$SpanRange;", "", "start", "", "end", "bold", "", "italic", "underline", "strikethrough", "color", "", "link", "(IIZZZZLjava/lang/String;Ljava/lang/String;)V", "getBold", "()Z", "getColor", "()Ljava/lang/String;", "getEnd", "()I", "getItalic", "getLink", "getStart", "getStrikethrough", "getUnderline", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
    public static final class SpanRange {
        private final int start = 0;
        private final int end = 0;
        private final boolean bold = false;
        private final boolean italic = false;
        private final boolean underline = false;
        private final boolean strikethrough = false;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String color = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String link = null;
        
        public SpanRange(int start, int end, boolean bold, boolean italic, boolean underline, boolean strikethrough, @org.jetbrains.annotations.Nullable()
        java.lang.String color, @org.jetbrains.annotations.Nullable()
        java.lang.String link) {
            super();
        }
        
        public final int getStart() {
            return 0;
        }
        
        public final int getEnd() {
            return 0;
        }
        
        public final boolean getBold() {
            return false;
        }
        
        public final boolean getItalic() {
            return false;
        }
        
        public final boolean getUnderline() {
            return false;
        }
        
        public final boolean getStrikethrough() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getColor() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getLink() {
            return null;
        }
        
        public final int component1() {
            return 0;
        }
        
        public final int component2() {
            return 0;
        }
        
        public final boolean component3() {
            return false;
        }
        
        public final boolean component4() {
            return false;
        }
        
        public final boolean component5() {
            return false;
        }
        
        public final boolean component6() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component7() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component8() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ncy.notechaty.ui.richtext.RichTextState.SpanRange copy(int start, int end, boolean bold, boolean italic, boolean underline, boolean strikethrough, @org.jetbrains.annotations.Nullable()
        java.lang.String color, @org.jetbrains.annotations.Nullable()
        java.lang.String link) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}