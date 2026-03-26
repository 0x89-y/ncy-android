package com.ncy.notechaty.ui.voicemessage;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.core.content.ContextCompat;
import java.io.File;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000$\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\u001a0\u0010\u0000\u001a\u00020\u00012\u0018\u0010\u0002\u001a\u0014\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0005\u00a8\u0006\u000b"}, d2 = {"VoiceRecorder", "", "onRecorded", "Lkotlin/Function2;", "Ljava/io/File;", "", "onCancel", "Lkotlin/Function0;", "formatDuration", "", "ms", "app_debug"})
public final class VoiceRecorderKt {
    
    @androidx.compose.runtime.Composable()
    public static final void VoiceRecorder(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.io.File, ? super java.lang.Long, kotlin.Unit> onRecorded, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onCancel) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String formatDuration(long ms) {
        return null;
    }
}