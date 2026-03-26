package com.ncy.notechaty.di

import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.FileChatRepository
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
}
