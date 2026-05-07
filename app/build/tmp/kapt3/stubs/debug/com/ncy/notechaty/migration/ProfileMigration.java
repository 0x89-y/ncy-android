package com.ncy.notechaty.migration;

import android.content.Context;
import com.ncy.notechaty.data.model.Profile;
import com.ncy.notechaty.data.repository.ProfileRepository;
import java.io.File;
import java.util.UUID;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/ncy/notechaty/migration/ProfileMigration;", "", "()V", "DEFAULT_PROFILE_NAME", "", "MARKER_NAME", "runIfNeeded", "", "context", "Landroid/content/Context;", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "app_debug"})
public final class ProfileMigration {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String MARKER_NAME = ".migrated_v2";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String DEFAULT_PROFILE_NAME = "Personal";
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.migration.ProfileMigration INSTANCE = null;
    
    private ProfileMigration() {
        super();
    }
    
    /**
     * Migrate the legacy flat layout (`NoteChaty/chat_*`) into the profile-scoped layout
     * (`NoteChaty/profile_<id>/chat_*`). Idempotent — checks a marker file before running.
     * Must run BEFORE any ViewModel reads chats.
     */
    public final void runIfNeeded(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository profileRepository) {
    }
}