package com.ncy.notechaty.ui.chatlist;

import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
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

  public ChatListViewModel_Factory(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public ChatListViewModel get() {
    return newInstance(repositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static ChatListViewModel_Factory create(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new ChatListViewModel_Factory(repositoryProvider, settingsRepositoryProvider);
  }

  public static ChatListViewModel newInstance(ChatRepository repository,
      SettingsRepository settingsRepository) {
    return new ChatListViewModel(repository, settingsRepository);
  }
}
