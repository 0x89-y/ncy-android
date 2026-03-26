package com.ncy.notechaty.ui.settings;

import android.content.Context;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<ChatRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<Context> contextProvider;

  public SettingsViewModel_Factory(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(repositoryProvider.get(), settingsRepositoryProvider.get(), contextProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<Context> contextProvider) {
    return new SettingsViewModel_Factory(repositoryProvider, settingsRepositoryProvider, contextProvider);
  }

  public static SettingsViewModel newInstance(ChatRepository repository,
      SettingsRepository settingsRepository, Context context) {
    return new SettingsViewModel(repository, settingsRepository, context);
  }
}
