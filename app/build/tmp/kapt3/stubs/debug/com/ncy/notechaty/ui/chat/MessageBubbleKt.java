package com.ncy.notechaty.ui.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.compose.foundation.ExperimentalFoundationApi;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.SpanStyle;
import androidx.compose.ui.text.font.FontStyle;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextDecoration;
import androidx.compose.ui.graphics.Brush;
import com.ncy.notechaty.data.model.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000f\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\u001a<\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u000e\u0010\u000f\u001a\u00b0\u0001\u0010\u0010\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00030\u00162\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00030\u00162\u000e\b\u0002\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00030\u00162\u000e\b\u0002\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00030\u00162\b\b\u0002\u0010\u001a\u001a\u00020\u001b2\b\b\u0002\u0010\u001c\u001a\u00020\u001b2\u000e\b\u0002\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00030\u00162\u000e\b\u0002\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00030\u00162\n\b\u0002\u0010\u001f\u001a\u0004\u0018\u00010\u000b2\b\b\u0002\u0010 \u001a\u00020\u001b2\b\b\u0002\u0010!\u001a\u00020\"H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0002\b#\u001a2\u0010$\u001a\u00020\u00032\f\u0010%\u001a\b\u0012\u0004\u0012\u00020\'0&2\u0006\u0010\u0013\u001a\u00020\u00142\b\b\u0002\u0010\n\u001a\u00020\u000bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b(\u0010)\u001aF\u0010*\u001a\u00020\u00032\f\u0010+\u001a\b\u0012\u0004\u0012\u00020,0&2\u0006\u0010\u0013\u001a\u00020\u00142\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000bH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b-\u0010.\u001a*\u0010/\u001a\u00020\u00052\f\u0010+\u001a\b\u0012\u0004\u0012\u00020,0&2\b\b\u0002\u00100\u001a\u00020\u000bH\u0002\u00f8\u0001\u0000\u00a2\u0006\u0004\b1\u00102\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u00063"}, d2 = {"urlPattern", "Lkotlin/text/Regex;", "LinkedText", "", "annotatedString", "Landroidx/compose/ui/text/AnnotatedString;", "fontSize", "", "fontWeight", "Landroidx/compose/ui/text/font/FontWeight;", "textColor", "Landroidx/compose/ui/graphics/Color;", "context", "Landroid/content/Context;", "LinkedText-42QJj7c", "(Landroidx/compose/ui/text/AnnotatedString;ILandroidx/compose/ui/text/font/FontWeight;JLandroid/content/Context;)V", "MessageBubble", "message", "Lcom/ncy/notechaty/data/model/Message;", "chatDir", "Ljava/io/File;", "onEdit", "Lkotlin/Function0;", "onDelete", "onToggleStar", "onSetReminder", "isSelectionMode", "", "isSelected", "onToggleSelect", "onEnterSelectionMode", "bubbleColor", "useGradient", "modifier", "Landroidx/compose/ui/Modifier;", "MessageBubble-yW_z194", "RichTextContent", "blocks", "", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "RichTextContent-mxwnekA", "(Ljava/util/List;Ljava/io/File;J)V", "RichTextSpans", "spans", "Lcom/ncy/notechaty/data/model/RichTextSpan;", "RichTextSpans-xwkQ0AY", "(Ljava/util/List;Ljava/io/File;ILandroidx/compose/ui/text/font/FontWeight;J)V", "buildAnnotatedSpan", "defaultColor", "buildAnnotatedSpan-4WTKRHQ", "(Ljava/util/List;J)Landroidx/compose/ui/text/AnnotatedString;", "app_debug"})
public final class MessageBubbleKt {
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex urlPattern = null;
}