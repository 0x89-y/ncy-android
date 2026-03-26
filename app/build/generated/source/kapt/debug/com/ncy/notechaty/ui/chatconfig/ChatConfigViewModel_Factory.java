package com.ncy.notechaty.ui.chatconfig;

import androidx.lifecycle.SavedStateHandle;
import com.ncy.notechaty.data.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatConfigViewModel_Factory implements Factory<ChatConfigViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ChatRepository> repositoryProvider;

  public ChatConfigViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> repositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ChatConfigViewModel get() {
    return newInstance(savedStateHandleProvider.get(), repositoryProvider.get());
  }

  public static ChatConfigViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ChatRepository> repositoryProvider) {
    return new ChatConfigViewModel_Factory(savedStateHandleProvider, repositoryProvider);
  }

  public static ChatConfigViewModel newInstance(SavedStateHandle savedStateHandle,
      ChatRepository repository) {
    return new ChatConfigViewModel(savedStateHandle, repository);
  }
}
