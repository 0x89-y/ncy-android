package com.ncy.notechaty.di

import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.FileChatRepository
import com.ncy.notechaty.data.sync.transport.ClientSyncTransport
import com.ncy.notechaty.data.sync.transport.LanWebSocketTransport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: FileChatRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindClientSyncTransport(impl: LanWebSocketTransport): ClientSyncTransport
}
