package com.example.iagointelbras.feature.devices.di

import com.example.iagointelbras.domain.repository.DeviceRepository
import com.example.iagointelbras.feature.devices.usecase.GetDevices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DeviceUseCaseModule {
    @Provides
    fun provideGetDevices(repository: DeviceRepository) = GetDevices(repository)
}
