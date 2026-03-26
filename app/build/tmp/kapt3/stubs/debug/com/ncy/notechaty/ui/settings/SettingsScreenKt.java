package com.ncy.notechaty.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.vector.ImageVector;
import android.view.HapticFeedbackConstants;
import com.ncy.notechaty.data.model.AccentColor;
import com.ncy.notechaty.data.model.ThemeMode;
import com.ncy.notechaty.util.FileUtils;
import java.io.File;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a.\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u001a.\u0010\t\u001a\u00020\u00012\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\b\b\u0002\u0010\f\u001a\u00020\rH\u0007\u001a\u0015\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0002\u00a2\u0006\u0002\u0010\u0012\u00a8\u0006\u0013"}, d2 = {"SettingsRow", "", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "title", "", "subtitle", "onClick", "Lkotlin/Function0;", "SettingsScreen", "onNavigateBack", "onOpenLicenses", "viewModel", "Lcom/ncy/notechaty/ui/settings/SettingsViewModel;", "accentPreviewColor", "Landroidx/compose/ui/graphics/Color;", "accent", "Lcom/ncy/notechaty/data/model/AccentColor;", "(Lcom/ncy/notechaty/data/model/AccentColor;)J", "app_debug"})
public final class SettingsScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    public static final void SettingsScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNavigateBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onOpenLicenses, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.ui.settings.SettingsViewModel viewModel) {
    }
    
    private static final long accentPreviewColor(com.ncy.notechaty.data.model.AccentColor accent) {
        return 0L;
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SettingsRow(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, java.lang.String subtitle, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
}