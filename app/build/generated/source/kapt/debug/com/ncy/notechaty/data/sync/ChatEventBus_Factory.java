package com.ncy.notechaty.data.sync;

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
public final class ChatEventBus_Factory implements Factory<ChatEventBus> {
  @Override
  public ChatEventBus get() {
    return newInstance();
  }

  public static ChatEventBus_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ChatEventBus newInstance() {
    return new ChatEventBus();
  }

  private static final class InstanceHolder {
    private static final ChatEventBus_Factory INSTANCE = new ChatEventBus_Factory();
  }
}
