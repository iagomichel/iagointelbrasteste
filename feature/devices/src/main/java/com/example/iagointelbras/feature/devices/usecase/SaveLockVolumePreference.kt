package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.repository.LockSettingsRepository

class SaveLockVolumePreference(private val repository: LockSettingsRepository) {
    suspend operator fun invoke(device: Device, volume: Int) =
        repository.saveVolume(device, volume)
}
