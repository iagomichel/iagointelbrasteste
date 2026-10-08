package com.example.iagointelbras.feature.devices.di

import com.example.iagointelbras.domain.repository.DeviceRepository
import com.example.iagointelbras.feature.devices.usecase.ClearAccessToken
import com.example.iagointelbras.feature.devices.usecase.IsConnected
import com.example.iagointelbras.feature.devices.usecase.SaveAccessToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ConnectionUseCaseModule {
    @Provides
    fun provideSaveAccessToken(repository: DeviceRepository) = SaveAccessToken(repository)

    @Provides
    fun provideIsConnected(repository: DeviceRepository) = IsConnected(repository)

    @Provides
    fun provideClearAccessToken(repository: DeviceRepository) = ClearAccessToken(repository)
}
