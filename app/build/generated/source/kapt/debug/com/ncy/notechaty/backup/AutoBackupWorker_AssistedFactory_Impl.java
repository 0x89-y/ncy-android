package com.ncy.notechaty.backup;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AutoBackupWorker_AssistedFactory_Impl implements AutoBackupWorker_AssistedFactory {
  private final AutoBackupWorker_Factory delegateFactory;

  AutoBackupWorker_AssistedFactory_Impl(AutoBackupWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public AutoBackupWorker create(Context arg0, WorkerParameters arg1) {
    return delegateFactory.get(arg0, arg1);
  }

  public static Provider<AutoBackupWorker_AssistedFactory> create(
      AutoBackupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new AutoBackupWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<AutoBackupWorker_AssistedFactory> createFactoryProvider(
      AutoBackupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new AutoBackupWorker_AssistedFactory_Impl(delegateFactory));
  }
}
