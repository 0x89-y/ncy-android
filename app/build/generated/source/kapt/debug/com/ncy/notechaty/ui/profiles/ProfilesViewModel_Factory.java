package com.ncy.notechaty.ui.profiles;

import com.ncy.notechaty.data.repository.ProfileRepository;
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
public final class ProfilesViewModel_Factory implements Factory<ProfilesViewModel> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  public ProfilesViewModel_Factory(Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public ProfilesViewModel get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static ProfilesViewModel_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new ProfilesViewModel_Factory(profileRepositoryProvider);
  }

  public static ProfilesViewModel newInstance(ProfileRepository profileRepository) {
    return new ProfilesViewModel(profileRepository);
  }
}
