package com.ncy.notechaty.ui.navigation;

import androidx.compose.animation.EnterTransition;
import androidx.compose.animation.ExitTransition;
import androidx.compose.runtime.Composable;
import androidx.navigation.NavHostController;
import androidx.navigation.NavType;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\r\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\b\b\u0002\u0010\u000f\u001a\u00020\u0004J\u000e\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/ui/navigation/Routes;", "", "()V", "CHAT", "", "CHAT_CONFIG", "CHAT_LIST", "LICENSES", "PROFILES", "SEARCH", "SETTINGS", "STARRED", "SYNC", "chat", "chatId", "name", "chatConfig", "app_debug"})
public final class Routes {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHAT_LIST = "chat_list";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHAT = "chat/{chatId}?name={name}";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHAT_CONFIG = "chat_config/{chatId}";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SETTINGS = "settings";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String LICENSES = "licenses";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String STARRED = "starred";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SEARCH = "search";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String PROFILES = "profiles";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SYNC = "sync";
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.ui.navigation.Routes INSTANCE = null;
    
    private Routes() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String chat(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String chatConfig(@org.jetbrains.annotations.NotNull()
    java.lang.String chatId) {
        return null;
    }
}