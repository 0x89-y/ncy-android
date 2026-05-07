package com.ncy.notechaty.data.repository;

import android.content.Context;
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

  public FileChatRepository_Factory(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider) {
    this.contextProvider = contextProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.chatLockStateProvider = chatLockStateProvider;
  }

  @Override
  public FileChatRepository get() {
    return newInstance(contextProvider.get(), profileRepositoryProvider.get(), chatLockStateProvider.get());
  }

  public static FileChatRepository_Factory create(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<ChatLockState> chatLockStateProvider) {
    return new FileChatRepository_Factory(contextProvider, profileRepositoryProvider, chatLockStateProvider);
  }

  public static FileChatRepository newInstance(Context context, ProfileRepository profileRepository,
      ChatLockState chatLockState) {
    return new FileChatRepository(context, profileRepository, chatLockState);
  }
}
