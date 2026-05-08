package com.ncy.notechaty.data.sync;

import com.ncy.notechaty.data.repository.ChatRepository;
import com.ncy.notechaty.data.sync.event.SyncSnapshot;
import com.ncy.notechaty.data.sync.peer.SyncPeerStore;
import com.ncy.notechaty.data.sync.transport.SyncClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SyncManager_Factory implements Factory<SyncManager> {
  private final Provider<SyncPeerStore> peerStoreProvider;

  private final Provider<DeviceIdentity> deviceIdentityProvider;

  private final Provider<ChatEventBus> eventBusProvider;

  private final Provider<SyncClient> syncClientProvider;

  private final Provider<SyncSnapshot> snapshotProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  public SyncManager_Factory(Provider<SyncPeerStore> peerStoreProvider,
      Provider<DeviceIdentity> deviceIdentityProvider, Provider<ChatEventBus> eventBusProvider,
      Provider<SyncClient> syncClientProvider, Provider<SyncSnapshot> snapshotProvider,
      Provider<ChatRepository> chatRepositoryProvider) {
    this.peerStoreProvider = peerStoreProvider;
    this.deviceIdentityProvider = deviceIdentityProvider;
    this.eventBusProvider = eventBusProvider;
    this.syncClientProvider = syncClientProvider;
    this.snapshotProvider = snapshotProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public SyncManager get() {
    return newInstance(peerStoreProvider.get(), deviceIdentityProvider.get(), eventBusProvider.get(), syncClientProvider.get(), snapshotProvider.get(), chatRepositoryProvider.get());
  }

  public static SyncManager_Factory create(Provider<SyncPeerStore> peerStoreProvider,
      Provider<DeviceIdentity> deviceIdentityProvider, Provider<ChatEventBus> eventBusProvider,
      Provider<SyncClient> syncClientProvider, Provider<SyncSnapshot> snapshotProvider,
      Provider<ChatRepository> chatRepositoryProvider) {
    return new SyncManager_Factory(peerStoreProvider, deviceIdentityProvider, eventBusProvider, syncClientProvider, snapshotProvider, chatRepositoryProvider);
  }

  public static SyncManager newInstance(SyncPeerStore peerStore, DeviceIdentity deviceIdentity,
      ChatEventBus eventBus, SyncClient syncClient, SyncSnapshot snapshot,
      ChatRepository chatRepository) {
    return new SyncManager(peerStore, deviceIdentity, eventBus, syncClient, snapshot, chatRepository);
  }
}
