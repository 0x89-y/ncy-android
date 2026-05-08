package com.ncy.notechaty.data.sync.transport;

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
public final class SyncClient_Factory implements Factory<SyncClient> {
  @Override
  public SyncClient get() {
    return newInstance();
  }

  public static SyncClient_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SyncClient newInstance() {
    return new SyncClient();
  }

  private static final class InstanceHolder {
    private static final SyncClient_Factory INSTANCE = new SyncClient_Factory();
  }
}
