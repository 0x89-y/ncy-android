package com.ncy.notechaty.backup;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.ncy.notechaty.data.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
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
public final class AutoBackupWorker_Factory {
  private final Provider<ChatRepository> repositoryProvider;

  public AutoBackupWorker_Factory(Provider<ChatRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public AutoBackupWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, repositoryProvider.get());
  }

  public static AutoBackupWorker_Factory create(Provider<ChatRepository> repositoryProvider) {
    return new AutoBackupWorker_Factory(repositoryProvider);
  }

  public static AutoBackupWorker newInstance(Context appContext, WorkerParameters workerParams,
      ChatRepository repository) {
    return new AutoBackupWorker(appContext, workerParams, repository);
  }
}
