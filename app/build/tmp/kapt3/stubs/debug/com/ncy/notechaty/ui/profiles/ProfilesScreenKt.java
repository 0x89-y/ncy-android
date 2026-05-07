package com.ncy.notechaty.ui.profiles;

import android.view.HapticFeedbackConstants;
import android.widget.Toast;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.vector.ImageVector;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.input.PasswordVisualTransformation;
import androidx.compose.foundation.lazy.grid.GridCells;
import com.ncy.notechaty.data.model.Profile;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000@\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0010\n\u0002\u0018\u0002\n\u0000\u001a2\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a:\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00032\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\b\b\u0002\u0010\r\u001a\u00020\u000eH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u000f\u0010\u0010\u001al\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a:\u0010\u001b\u001a\u00020\u00012\u0006\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u001e\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a2\u0010\u001f\u001a\u00020\u00012\u0006\u0010 \u001a\u00020\u00152\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u001e\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a&\u0010!\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\"\u001a\u00020\u00152\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a \u0010#\u001a\u00020\u00012\f\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\b\b\u0002\u0010%\u001a\u00020&H\u0007\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\'"}, d2 = {"IconPickerDialog", "", "currentKey", "", "onDismiss", "Lkotlin/Function0;", "onPick", "Lkotlin/Function1;", "ManageRow", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "label", "onClick", "contentColor", "Landroidx/compose/ui/graphics/Color;", "ManageRow-g2O1Hgs", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;Lkotlin/jvm/functions/Function0;J)V", "ProfileManageSheet", "profile", "Lcom/ncy/notechaty/data/model/Profile;", "canDelete", "", "onRename", "onDelete", "onSetPin", "onClearPin", "onPickIcon", "ProfileNameDialog", "title", "initialValue", "onConfirm", "ProfilePinSetupDialog", "isChange", "ProfileRow", "isActive", "ProfilesScreen", "onNavigateBack", "viewModel", "Lcom/ncy/notechaty/ui/profiles/ProfilesViewModel;", "app_debug"})
public final class ProfilesScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void ProfilesScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateBack, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.ui.profiles.ProfilesViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ProfileRow(com.ncy.notechaty.data.model.Profile profile, boolean isActive, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ProfileManageSheet(com.ncy.notechaty.data.model.Profile profile, boolean canDelete, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function0<kotlin.Unit> onRename, kotlin.jvm.functions.Function0<kotlin.Unit> onDelete, kotlin.jvm.functions.Function0<kotlin.Unit> onSetPin, kotlin.jvm.functions.Function0<kotlin.Unit> onClearPin, kotlin.jvm.functions.Function0<kotlin.Unit> onPickIcon) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void IconPickerDialog(java.lang.String currentKey, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onPick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ProfileNameDialog(java.lang.String title, java.lang.String initialValue, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onConfirm) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ProfilePinSetupDialog(boolean isChange, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onConfirm) {
    }
}