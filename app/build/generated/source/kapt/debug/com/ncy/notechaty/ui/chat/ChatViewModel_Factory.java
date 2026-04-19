package com.ncy.notechaty.ui.chat;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.repository.SettingsRepository;
import com.ncy.notechaty.security.ChatLockState;
import com.ncy.notechaty.security.PinManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ChatRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ChatLockState> chatLockStateProvider;

  private final Provider<PinManager> pinManagerProvider;

  private final Provider<Context> appContextProvider;

  public ChatViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider, Provider<PinManager> pinManagerProvider,
      Provider<Context> appContextProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.repositoryProvider = repositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.chatLockStateProvider = chatLockStateProvider;
    this.pinManagerProvider = pinManagerProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(savedStateHandleProvider.get(), repositoryProvider.get(), settingsRepositoryProvider.get(), chatLockStateProvider.get(), pinManagerProvider.get(), appContextProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> repositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider, Provider<PinManager> pinManagerProvider,
      Provider<Context> appContextProvider) {
    return new ChatViewModel_Factory(savedStateHandleProvider, repositoryProvider, settingsRepositoryProvider, chatLockStateProvider, pinManagerProvider, appContextProvider);
  }

  public static ChatViewModel newInstance(SavedStateHandle savedStateHandle,
      ChatRepository repository, SettingsRepository settingsRepository, ChatLockState chatLockState,
      PinManager pinManager, Context appContext) {
    return new ChatViewModel(savedStateHandle, repository, settingsRepository, chatLockState, pinManager, appContext);
  }
}
