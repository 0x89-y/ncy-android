package com.ncy.notechaty.ui.chatconfig;

import androidx.lifecycle.SavedStateHandle;
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
public final class ChatConfigViewModel_Factory implements Factory<ChatConfigViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ChatRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public ChatConfigViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.repositoryProvider = repositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public ChatConfigViewModel get() {
    return newInstance(savedStateHandleProvider.get(), repositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static ChatConfigViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new ChatConfigViewModel_Factory(savedStateHandleProvider, repositoryProvider, settingsRepositoryProvider);
  }

  public static ChatConfigViewModel newInstance(SavedStateHandle savedStateHandle,
      ChatRepository repository, SettingsRepository settingsRepository) {
    return new ChatConfigViewModel(savedStateHandle, repository, settingsRepository);
  }
}
