package com.ncy.notechaty.data.sync.event;

import com.ncy.notechaty.data.repository.ProfileRepository;
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
public final class SyncSnapshot_Factory implements Factory<SyncSnapshot> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  public SyncSnapshot_Factory(Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public SyncSnapshot get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static SyncSnapshot_Factory create(Provider<ProfileRepository> profileRepositoryProvider) {
    return new SyncSnapshot_Factory(profileRepositoryProvider);
  }

  public static SyncSnapshot newInstance(ProfileRepository profileRepository) {
    return new SyncSnapshot(profileRepository);
  }
}
