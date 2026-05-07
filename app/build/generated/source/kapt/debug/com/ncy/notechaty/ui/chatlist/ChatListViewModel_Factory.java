package com.ncy.notechaty.ui.chatlist;

import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.ProfileRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import com.ncy.notechaty.security.ProfileLockState;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class ChatListViewModel_Factory implements Factory<ChatListViewModel> {
  private final Provider<ChatRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<ProfileLockState> profileLockStateProvider;

  private final Provider<PinManager> pinManagerProvider;

  private final Provider<ChatLockState> chatLockStateProvider;

  public ChatListViewModel_Factory(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ProfileLockState> profileLockStateProvider, Provider<PinManager> pinManagerProvider,
      Provider<ChatLockState> chatLockStateProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.profileLockStateProvider = profileLockStateProvider;
    this.pinManagerProvider = pinManagerProvider;
    this.chatLockStateProvider = chatLockStateProvider;
  }

  @Override
  public ChatListViewModel get() {
    return newInstance(repositoryProvider.get(), settingsRepositoryProvider.get(), profileRepositoryProvider.get(), profileLockStateProvider.get(), pinManagerProvider.get(), chatLockStateProvider.get());
  }

  public static ChatListViewModel_Factory create(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ProfileLockState> profileLockStateProvider, Provider<PinManager> pinManagerProvider,
      Provider<ChatLockState> chatLockStateProvider) {
    return new ChatListViewModel_Factory(repositoryProvider, settingsRepositoryProvider, profileRepositoryProvider, profileLockStateProvider, pinManagerProvider, chatLockStateProvider);
  }

  public static ChatListViewModel newInstance(ChatRepository repository,
      SettingsRepository settingsRepository, ProfileRepository profileRepository,
      ProfileLockState profileLockState, PinManager pinManager, ChatLockState chatLockState) {
    return new ChatListViewModel(repository, settingsRepository, profileRepository, profileLockState, pinManager, chatLockState);
  }
}
