package com.ncy.notechaty.data.repository;

import android.content.Context;
import com.ncy.notechaty.data.sync.ChatEventBus;
import com.ncy.notechaty.data.sync.DeviceIdentity;
import com.ncy.notechaty.data.sync.protection.ProtectionKeyStore;
import com.ncy.notechaty.security.ChatLockState;
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
public final class FileChatRepository_Factory implements Factory<FileChatRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<ChatLockState> chatLockStateProvider;

  private final Provider<DeviceIdentity> deviceIdentityProvider;

  private final Provider<ChatEventBus> eventBusProvider;

  private final Provider<ProtectionKeyStore> keyStoreProvider;

  public FileChatRepository_Factory(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider,
      Provider<DeviceIdentity> deviceIdentityProvider, Provider<ChatEventBus> eventBusProvider,
      Provider<ProtectionKeyStore> keyStoreProvider) {
    this.contextProvider = contextProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.chatLockStateProvider = chatLockStateProvider;
    this.deviceIdentityProvider = deviceIdentityProvider;
    this.eventBusProvider = eventBusProvider;
    this.keyStoreProvider = keyStoreProvider;
  }

  @Override
  public FileChatRepository get() {
    return newInstance(contextProvider.get(), profileRepositoryProvider.get(), chatLockStateProvider.get(), deviceIdentityProvider.get(), eventBusProvider.get(), keyStoreProvider.get());
  }

  public static FileChatRepository_Factory create(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider,
      Provider<DeviceIdentity> deviceIdentityProvider, Provider<ChatEventBus> eventBusProvider,
      Provider<ProtectionKeyStore> keyStoreProvider) {
    return new FileChatRepository_Factory(contextProvider, profileRepositoryProvider, chatLockStateProvider, deviceIdentityProvider, eventBusProvider, keyStoreProvider);
  }

  public static FileChatRepository newInstance(Context context, ProfileRepository profileRepository,
      ChatLockState chatLockState, DeviceIdentity deviceIdentity, ChatEventBus eventBus,
      ProtectionKeyStore keyStore) {
    return new FileChatRepository(context, profileRepository, chatLockState, deviceIdentity, eventBus, keyStore);
  }
}
