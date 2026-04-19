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
public final class ChatLockState_Factory implements Factory<ChatLockState> {
  @Override
  public ChatLockState get() {
    return newInstance();
  }

  public static ChatLockState_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ChatLockState newInstance() {
    return new ChatLockState();
  }

  private static final class InstanceHolder {
    private static final ChatLockState_Factory INSTANCE = new ChatLockState_Factory();
  }
}
