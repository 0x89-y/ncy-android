package com.ncy.notechaty.security;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ProfileLockState_Factory implements Factory<ProfileLockState> {
  @Override
  public ProfileLockState get() {
    return newInstance();
  }

  public static ProfileLockState_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProfileLockState newInstance() {
    return new ProfileLockState();
  }

  private static final class InstanceHolder {
    private static final ProfileLockState_Factory INSTANCE = new ProfileLockState_Factory();
  }
}
