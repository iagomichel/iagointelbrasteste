package com.example.iagointelbras.network.di

import com.example.iagointelbras.network.api.CasaApi
import com.example.iagointelbras.network.auth.AccessTokenProvider
import com.example.iagointelbras.network.factory.CasaApiFactory
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideCasaApi(tokenProvider: AccessTokenProvider, moshi: Moshi): CasaApi =
        CasaApiFactory.create(tokenProvider, moshi)
}
