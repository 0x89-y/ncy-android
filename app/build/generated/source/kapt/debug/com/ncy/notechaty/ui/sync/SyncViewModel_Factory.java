package com.ncy.notechaty.ui.sync;

import com.ncy.notechaty.data.sync.SyncManager;
import com.ncy.notechaty.data.sync.discovery.MdnsDiscovery;
import com.ncy.notechaty.data.sync.peer.SyncPeerStore;
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
public final class SyncViewModel_Factory implements Factory<SyncViewModel> {
  private final Provider<SyncPeerStore> peerStoreProvider;

  private final Provider<SyncManager> syncManagerProvider;

  private final Provider<MdnsDiscovery> mdnsDiscoveryProvider;

  public SyncViewModel_Factory(Provider<SyncPeerStore> peerStoreProvider,
      Provider<SyncManager> syncManagerProvider, Provider<MdnsDiscovery> mdnsDiscoveryProvider) {
    this.peerStoreProvider = peerStoreProvider;
    this.syncManagerProvider = syncManagerProvider;
    this.mdnsDiscoveryProvider = mdnsDiscoveryProvider;
  }

  @Override
  public SyncViewModel get() {
    return newInstance(peerStoreProvider.get(), syncManagerProvider.get(), mdnsDiscoveryProvider.get());
  }

  public static SyncViewModel_Factory create(Provider<SyncPeerStore> peerStoreProvider,
      Provider<SyncManager> syncManagerProvider, Provider<MdnsDiscovery> mdnsDiscoveryProvider) {
    return new SyncViewModel_Factory(peerStoreProvider, syncManagerProvider, mdnsDiscoveryProvider);
  }

  public static SyncViewModel newInstance(SyncPeerStore peerStore, SyncManager syncManager,
      MdnsDiscovery mdnsDiscovery) {
    return new SyncViewModel(peerStore, syncManager, mdnsDiscovery);
  }
}
