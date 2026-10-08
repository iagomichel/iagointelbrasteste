package com.example.iagointelbras.feature.devices.di

import com.example.iagointelbras.domain.repository.CameraRepository
import com.example.iagointelbras.feature.devices.usecase.GetCameraQuota
import com.example.iagointelbras.feature.devices.usecase.StartCamera
import com.example.iagointelbras.feature.devices.usecase.StopCamera
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CameraUseCaseModule {
    @Provides
    fun provideStartCamera(repository: CameraRepository) = StartCamera(repository)

    @Provides
    fun provideStopCamera(repository: CameraRepository) = StopCamera(repository)

    @Provides
    fun provideGetCameraQuota(repository: CameraRepository) = GetCameraQuota(repository)
}
