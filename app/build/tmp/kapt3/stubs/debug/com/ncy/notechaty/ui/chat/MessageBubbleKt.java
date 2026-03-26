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
import android.view.HapticFeedbackConstants;
import androidx.compose.ui.graphics.Brush;
import com.ncy.notechaty.data.model.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000z\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\u001a<\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0006\u0010\u0007\u001a\u00020\b2\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00030\nH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\f\u0010\r\u001a<\u0010\u000e\u001a\u00020\u00032\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\b\b\u0002\u0010\u0015\u001a\u00020\b2\u0006\u0010\u0016\u001a\u00020\u0017H\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u0018\u0010\u0019\u001a\u00da\u0001\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001e2\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00030 2\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00030 2\u000e\b\u0002\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00030 2\u000e\b\u0002\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00030 2\u0014\b\u0002\u0010$\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00030\n2\b\b\u0002\u0010%\u001a\u00020&2\b\b\u0002\u0010\'\u001a\u00020&2\u000e\b\u0002\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00030 2\u000e\b\u0002\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00030 2\n\b\u0002\u0010*\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010+\u001a\u00020&2\b\b\u0002\u0010,\u001a\u00020&2\b\b\u0002\u0010-\u001a\u00020&2\b\b\u0002\u0010.\u001a\u00020/H\u0007\u00f8\u0001\u0000\u00a2\u0006\u0002\b0\u001a2\u00101\u001a\u00020\u00032\f\u00102\u001a\b\u0012\u0004\u0012\u0002030\u00052\u0006\u0010\u001d\u001a\u00020\u001e2\b\b\u0002\u0010\u0015\u001a\u00020\bH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b4\u00105\u001aF\u00106\u001a\u00020\u00032\f\u00107\u001a\b\u0012\u0004\u0012\u0002080\u00052\u0006\u0010\u001d\u001a\u00020\u001e2\b\b\u0002\u0010\u0011\u001a\u00020\u00122\b\b\u0002\u0010\u0013\u001a\u00020\u00142\b\b\u0002\u0010\u0015\u001a\u00020\bH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b9\u0010:\u001a*\u0010;\u001a\u00020\u00102\f\u00107\u001a\b\u0012\u0004\u0012\u0002080\u00052\b\b\u0002\u0010<\u001a\u00020\bH\u0002\u00f8\u0001\u0000\u00a2\u0006\u0004\b=\u0010>\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006?"}, d2 = {"urlPattern", "Lkotlin/text/Regex;", "ChecklistContent", "", "items", "", "Lcom/ncy/notechaty/data/model/ChecklistItem;", "contentColor", "Landroidx/compose/ui/graphics/Color;", "onToggleItem", "Lkotlin/Function1;", "", "ChecklistContent-bw27NRU", "(Ljava/util/List;JLkotlin/jvm/functions/Function1;)V", "LinkedText", "annotatedString", "Landroidx/compose/ui/text/AnnotatedString;", "fontSize", "", "fontWeight", "Landroidx/compose/ui/text/font/FontWeight;", "textColor", "context", "Landroid/content/Context;", "LinkedText-42QJj7c", "(Landroidx/compose/ui/text/AnnotatedString;ILandroidx/compose/ui/text/font/FontWeight;JLandroid/content/Context;)V", "MessageBubble", "message", "Lcom/ncy/notechaty/data/model/Message;", "chatDir", "Ljava/io/File;", "onEdit", "Lkotlin/Function0;", "onDelete", "onToggleStar", "onSetReminder", "onToggleChecklistItem", "isSelectionMode", "", "isSelected", "onToggleSelect", "onEnterSelectionMode", "bubbleColor", "useGradient", "isSearchResult", "isCurrentSearchResult", "modifier", "Landroidx/compose/ui/Modifier;", "MessageBubble-UnDaBbg", "RichTextContent", "blocks", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "RichTextContent-mxwnekA", "(Ljava/util/List;Ljava/io/File;J)V", "RichTextSpans", "spans", "Lcom/ncy/notechaty/data/model/RichTextSpan;", "RichTextSpans-xwkQ0AY", "(Ljava/util/List;Ljava/io/File;ILandroidx/compose/ui/text/font/FontWeight;J)V", "buildAnnotatedSpan", "defaultColor", "buildAnnotatedSpan-4WTKRHQ", "(Ljava/util/List;J)Landroidx/compose/ui/text/AnnotatedString;", "app_debug"})
public final class MessageBubbleKt {
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex urlPattern = null;
}