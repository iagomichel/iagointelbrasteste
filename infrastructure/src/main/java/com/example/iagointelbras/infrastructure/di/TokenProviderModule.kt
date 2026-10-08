package com.example.iagointelbras.infrastructure.di

import com.example.iagointelbras.domain.repository.TokenStore
import com.example.iagointelbras.network.auth.AccessTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TokenProviderModule {
    @Provides
    @Singleton
    fun provideAccessTokenProvider(tokenStore: TokenStore): AccessTokenProvider =
        AccessTokenProvider(tokenStore::read)
}
