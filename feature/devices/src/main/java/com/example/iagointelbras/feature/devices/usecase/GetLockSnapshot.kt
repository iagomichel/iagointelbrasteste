package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.repository.LockRepository

class GetLockSnapshot(private val repository: LockRepository) {
    suspend operator fun invoke(device: Device) =
        repository.getLockSnapshot(device)
}
