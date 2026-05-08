package com.ncy.notechaty.data.repository;

import android.content.Context;
import com.ncy.notechaty.data.sync.ChatEventBus;
import com.ncy.notechaty.data.sync.DeviceIdentity;
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

  public FileChatRepository_Factory(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider,
      Provider<DeviceIdentity> deviceIdentityProvider, Provider<ChatEventBus> eventBusProvider) {
    this.contextProvider = contextProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.chatLockStateProvider = chatLockStateProvider;
    this.deviceIdentityProvider = deviceIdentityProvider;
    this.eventBusProvider = eventBusProvider;
  }

  @Override
  public FileChatRepository get() {
    return newInstance(contextProvider.get(), profileRepositoryProvider.get(), chatLockStateProvider.get(), deviceIdentityProvider.get(), eventBusProvider.get());
  }

  public static FileChatRepository_Factory create(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider,
      Provider<DeviceIdentity> deviceIdentityProvider, Provider<ChatEventBus> eventBusProvider) {
    return new FileChatRepository_Factory(contextProvider, profileRepositoryProvider, chatLockStateProvider, deviceIdentityProvider, eventBusProvider);
  }

  public static FileChatRepository newInstance(Context context, ProfileRepository profileRepository,
      ChatLockState chatLockState, DeviceIdentity deviceIdentity, ChatEventBus eventBus) {
    return new FileChatRepository(context, profileRepository, chatLockState, deviceIdentity, eventBus);
  }
}
