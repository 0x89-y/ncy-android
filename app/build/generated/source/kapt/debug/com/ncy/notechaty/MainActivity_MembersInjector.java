package com.ncy.notechaty;

import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<PinManager> pinManagerProvider;

  private final Provider<ChatLockState> chatLockStateProvider;

  public MainActivity_MembersInjector(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<PinManager> pinManagerProvider, Provider<ChatLockState> chatLockStateProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.pinManagerProvider = pinManagerProvider;
    this.chatLockStateProvider = chatLockStateProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<PinManager> pinManagerProvider, Provider<ChatLockState> chatLockStateProvider) {
    return new MainActivity_MembersInjector(settingsRepositoryProvider, pinManagerProvider, chatLockStateProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectPinManager(instance, pinManagerProvider.get());
    injectChatLockState(instance, chatLockStateProvider.get());
  }

  @InjectedFieldSignature("com.ncy.notechaty.MainActivity.settingsRepository")
  public static void injectSettingsRepository(MainActivity instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.ncy.notechaty.MainActivity.pinManager")
  public static void injectPinManager(MainActivity instance, PinManager pinManager) {
    instance.pinManager = pinManager;
  }

  @InjectedFieldSignature("com.ncy.notechaty.MainActivity.chatLockState")
  public static void injectChatLockState(MainActivity instance, ChatLockState chatLockState) {
    instance.chatLockState = chatLockState;
  }
}
