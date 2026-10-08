package com.example.iagointelbras.feature.devices.di

import com.example.iagointelbras.domain.repository.LockRepository
import com.example.iagointelbras.domain.repository.LockSettingsRepository
import com.example.iagointelbras.feature.devices.usecase.GetLockHistory
import com.example.iagointelbras.feature.devices.usecase.GetLockOpenStatus
import com.example.iagointelbras.feature.devices.usecase.GetLockSnapshot
import com.example.iagointelbras.feature.devices.usecase.GetRemoteAccessStatus
import com.example.iagointelbras.feature.devices.usecase.GetSavedLockActions
import com.example.iagointelbras.feature.devices.usecase.GetSavedLockVolume
import com.example.iagointelbras.feature.devices.usecase.SaveLockAction
import com.example.iagointelbras.feature.devices.usecase.SaveLockVolumePreference
import com.example.iagointelbras.feature.devices.usecase.SetLockOpen
import com.example.iagointelbras.feature.devices.usecase.SetLockVolume
import com.example.iagointelbras.feature.devices.usecase.SetRemoteEnabled
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LockUseCaseModule {
    @Provides
    fun provideGetLockSnapshot(repository: LockRepository) = GetLockSnapshot(repository)

    @Provides
    fun provideGetLockOpenStatus(repository: LockRepository) = GetLockOpenStatus(repository)

    @Provides
    fun provideGetRemoteAccessStatus(repository: LockRepository) = GetRemoteAccessStatus(repository)

    @Provides
    fun provideGetSavedLockVolume(repository: LockSettingsRepository) = GetSavedLockVolume(repository)

    @Provides
    fun provideGetSavedLockActions(repository: LockSettingsRepository) = GetSavedLockActions(repository)

    @Provides
    fun provideSaveLockVolumePreference(repository: LockSettingsRepository) = SaveLockVolumePreference(repository)

    @Provides
    fun provideSaveLockAction(repository: LockSettingsRepository) = SaveLockAction(repository)

    @Provides
    fun provideSetLockOpen(repository: LockRepository) = SetLockOpen(repository)

    @Provides
    fun provideSetLockVolume(repository: LockRepository) = SetLockVolume(repository)

    @Provides
    fun provideSetRemoteEnabled(repository: LockRepository) = SetRemoteEnabled(repository)

    @Provides
    fun provideGetLockHistory(repository: LockRepository) = GetLockHistory(repository)
}
