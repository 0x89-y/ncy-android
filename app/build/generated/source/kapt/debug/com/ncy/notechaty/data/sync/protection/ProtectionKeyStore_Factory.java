package com.ncy.notechaty.data.sync.protection;

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
public final class ProtectionKeyStore_Factory implements Factory<ProtectionKeyStore> {
  @Override
  public ProtectionKeyStore get() {
    return newInstance();
  }

  public static ProtectionKeyStore_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProtectionKeyStore newInstance() {
    return new ProtectionKeyStore();
  }

  private static final class InstanceHolder {
    private static final ProtectionKeyStore_Factory INSTANCE = new ProtectionKeyStore_Factory();
  }
}
