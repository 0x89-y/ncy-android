package com.ncy.notechaty.ui.starred;

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
public final class StarredMessagesViewModel_Factory implements Factory<StarredMessagesViewModel> {
  private final Provider<ChatRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public StarredMessagesViewModel_Factory(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public StarredMessagesViewModel get() {
    return newInstance(repositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static StarredMessagesViewModel_Factory create(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new StarredMessagesViewModel_Factory(repositoryProvider, settingsRepositoryProvider);
  }

  public static StarredMessagesViewModel newInstance(ChatRepository repository,
      SettingsRepository settingsRepository) {
    return new StarredMessagesViewModel(repository, settingsRepository);
  }
}
