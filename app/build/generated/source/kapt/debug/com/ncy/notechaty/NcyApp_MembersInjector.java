package com.ncy.notechaty;

import androidx.hilt.work.HiltWorkerFactory;
import com.ncy.notechaty.data.repository.ProfileRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class NcyApp_MembersInjector implements MembersInjector<NcyApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public NcyApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  public static MembersInjector<NcyApp> create(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new NcyApp_MembersInjector(workerFactoryProvider, settingsRepositoryProvider, profileRepositoryProvider);
  }

  @Override
  public void injectMembers(NcyApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectProfileRepository(instance, profileRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.ncy.notechaty.NcyApp.workerFactory")
  public static void injectWorkerFactory(NcyApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.ncy.notechaty.NcyApp.settingsRepository")
  public static void injectSettingsRepository(NcyApp instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.ncy.notechaty.NcyApp.profileRepository")
  public static void injectProfileRepository(NcyApp instance, ProfileRepository profileRepository) {
    instance.profileRepository = profileRepository;
  }
}
