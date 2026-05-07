package com.ncy.notechaty.ui.profiles;

import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.Profile;
import com.ncy.notechaty.data.repository.ProfileRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0007J\u000e\u0010\u0011\u001a\u00020\u000f2\u0006\u0010\u0012\u001a\u00020\u0007J\u000e\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0007J\u0016\u0010\u0014\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00072\u0006\u0010\u0012\u001a\u00020\u0007J\u0016\u0010\u0015\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00072\u0006\u0010\u0016\u001a\u00020\u0007J\u0016\u0010\u0017\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u0007R\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\t\u00a8\u0006\u0019"}, d2 = {"Lcom/ncy/notechaty/ui/profiles/ProfilesViewModel;", "Landroidx/lifecycle/ViewModel;", "profileRepository", "Lcom/ncy/notechaty/data/repository/ProfileRepository;", "(Lcom/ncy/notechaty/data/repository/ProfileRepository;)V", "activeProfileId", "Lkotlinx/coroutines/flow/StateFlow;", "", "getActiveProfileId", "()Lkotlinx/coroutines/flow/StateFlow;", "profiles", "", "Lcom/ncy/notechaty/data/model/Profile;", "getProfiles", "clearProfilePin", "", "id", "createProfile", "name", "deleteProfile", "renameProfile", "setProfileIcon", "iconKey", "setProfilePin", "pin", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ProfilesViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ProfileRepository profileRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> profiles = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> activeProfileId = null;
    
    @javax.inject.Inject()
    public ProfilesViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ProfileRepository profileRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.data.model.Profile>> getProfiles() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getActiveProfileId() {
        return null;
    }
    
    public final void createProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void renameProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void deleteProfile(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void setProfilePin(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String pin) {
    }
    
    public final void clearProfilePin(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void setProfileIcon(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String iconKey) {
    }
}