package com.ncy.notechaty.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<Context> contextProvider;

  public ProfileRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(contextProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<Context> contextProvider) {
    return new ProfileRepository_Factory(contextProvider);
  }

  public static ProfileRepository newInstance(Context context) {
    return new ProfileRepository(context);
  }
}
