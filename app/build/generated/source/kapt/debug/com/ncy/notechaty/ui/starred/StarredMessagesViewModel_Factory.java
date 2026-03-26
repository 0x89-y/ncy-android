package com.ncy.notechaty.ui.starred;

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
public final class StarredMessagesViewModel_Factory implements Factory<StarredMessagesViewModel> {
  private final Provider<ChatRepository> repositoryProvider;

  public StarredMessagesViewModel_Factory(Provider<ChatRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public StarredMessagesViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static StarredMessagesViewModel_Factory create(
      Provider<ChatRepository> repositoryProvider) {
    return new StarredMessagesViewModel_Factory(repositoryProvider);
  }

  public static StarredMessagesViewModel newInstance(ChatRepository repository) {
    return new StarredMessagesViewModel(repository);
  }
}
