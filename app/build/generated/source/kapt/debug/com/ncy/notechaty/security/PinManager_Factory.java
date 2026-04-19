package com.ncy.notechaty.security;

import com.ncy.notechaty.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PinManager_Factory implements Factory<PinManager> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public PinManager_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public PinManager get() {
    return newInstance(settingsRepositoryProvider.get());
  }

  public static PinManager_Factory create(Provider<SettingsRepository> settingsRepositoryProvider) {
    return new PinManager_Factory(settingsRepositoryProvider);
  }

  public static PinManager newInstance(SettingsRepository settingsRepository) {
    return new PinManager(settingsRepository);
  }
}
