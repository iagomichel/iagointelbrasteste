package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.repository.LockRepository

class SetLockVolume(private val repository: LockRepository) {
    suspend operator fun invoke(device: Device, volume: Int) =
        repository.setLockVolume(device, volume)
}
