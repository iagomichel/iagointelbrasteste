package com.example.iagointelbras.infrastructure.di

import com.example.iagointelbras.domain.repository.CameraRepository
import com.example.iagointelbras.domain.repository.DeviceRepository
import com.example.iagointelbras.domain.repository.LockRepository
import com.example.iagointelbras.domain.repository.LockSettingsRepository
import com.example.iagointelbras.domain.repository.TokenStore
import com.example.iagointelbras.infrastructure.repository.local.LockSettingsRepositoryImpl
import com.example.iagointelbras.infrastructure.repository.remote.camera.CameraRepositoryImpl
import com.example.iagointelbras.infrastructure.repository.remote.device.DeviceRepositoryImpl
import com.example.iagointelbras.infrastructure.repository.remote.lock.LockRepositoryImpl
import com.example.iagointelbras.infrastructure.storage.SecureTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    @Singleton
    abstract fun bindTokenStore(store: SecureTokenStore): TokenStore

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(repository: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindLockRepository(repository: LockRepositoryImpl): LockRepository

    @Binds
    @Singleton
    abstract fun bindLockSettingsRepository(repository: LockSettingsRepositoryImpl): LockSettingsRepository

    @Binds
    @Singleton
    abstract fun bindCameraRepository(repository: CameraRepositoryImpl): CameraRepository
}
