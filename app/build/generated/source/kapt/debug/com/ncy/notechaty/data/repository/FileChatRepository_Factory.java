package com.ncy.notechaty.data.repository;

import android.content.Context;
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

  public FileChatRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FileChatRepository get() {
    return newInstance(contextProvider.get());
  }

  public static FileChatRepository_Factory create(Provider<Context> contextProvider) {
    return new FileChatRepository_Factory(contextProvider);
  }

  public static FileChatRepository newInstance(Context context) {
    return new FileChatRepository(context);
  }
}
