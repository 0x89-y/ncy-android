package com.ncy.notechaty.ui.chatlist;

import androidx.compose.foundation.ExperimentalFoundationApi;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import android.view.HapticFeedbackConstants;
import androidx.compose.ui.layout.ContentScale;
import androidx.compose.ui.text.font.FontStyle;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.input.PasswordVisualTransformation;
import androidx.compose.ui.text.style.TextOverflow;
import androidx.fragment.app.FragmentActivity;
import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.model.ChatSortMode;
import com.ncy.notechaty.data.model.Profile;
import com.ncy.notechaty.security.BiometricHelper;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000L\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\u001a*\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a<\u0010\u0007\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001aj\u0010\u0010\u001a\u00020\u00012\u0018\u0010\u0011\u001a\u0014\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u000e\b\u0002\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u000e\b\u0002\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u000e\b\u0002\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\b\b\u0002\u0010\u0017\u001a\u00020\u0018H\u0007\u001aH\u0010\u0019\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\r2\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0012\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\r0\u00052\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a*\u0010\u001d\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001aH\u0010\u001e\u001a\u00020\u00012\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010\u001a\u001a\u00020\r2\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0012\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\r0\u00052\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a\u0010\u0010!\u001a\u00020\u00062\u0006\u0010\"\u001a\u00020#H\u0002\u00a8\u0006$"}, d2 = {"AddProfileDialog", "", "onDismiss", "Lkotlin/Function0;", "onCreate", "Lkotlin/Function1;", "", "ChatListItem", "chat", "Lcom/ncy/notechaty/data/model/Chat;", "baseDir", "Ljava/io/File;", "isUnlocked", "", "onClick", "onLongClick", "ChatListScreen", "onChatClick", "Lkotlin/Function2;", "onSettingsClick", "onStarredClick", "onSearchClick", "onManageProfilesClick", "viewModel", "Lcom/ncy/notechaty/ui/chatlist/ChatListViewModel;", "ChatUnlockDialog", "useBiometric", "onVerify", "onUnlocked", "CreateChatDialog", "ProfileUnlockDialog", "profile", "Lcom/ncy/notechaty/data/model/Profile;", "formatTimestamp", "millis", "", "app_debug"})
public final class ChatListScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ChatListScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.String, ? super java.lang.String, kotlin.Unit> onChatClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSettingsClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onStarredClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSearchClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onManageProfilesClick, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.ui.chatlist.ChatListViewModel viewModel) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.foundation.ExperimentalFoundationApi.class})
    @androidx.compose.runtime.Composable()
    private static final void ChatListItem(com.ncy.notechaty.data.model.Chat chat, java.io.File baseDir, boolean isUnlocked, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, kotlin.jvm.functions.Function0<kotlin.Unit> onLongClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CreateChatDialog(kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onCreate) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ChatUnlockDialog(com.ncy.notechaty.data.model.Chat chat, boolean useBiometric, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, java.lang.Boolean> onVerify, kotlin.jvm.functions.Function0<kotlin.Unit> onUnlocked) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ProfileUnlockDialog(com.ncy.notechaty.data.model.Profile profile, boolean useBiometric, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, java.lang.Boolean> onVerify, kotlin.jvm.functions.Function0<kotlin.Unit> onUnlocked) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddProfileDialog(kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onCreate) {
    }
    
    private static final java.lang.String formatTimestamp(long millis) {
        return null;
    }
}