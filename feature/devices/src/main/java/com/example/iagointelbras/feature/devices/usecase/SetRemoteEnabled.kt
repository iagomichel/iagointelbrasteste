package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.repository.LockRepository

class SetRemoteEnabled(private val repository: LockRepository) {
    suspend operator fun invoke(device: Device, enabled: Boolean) =
        repository.setRemoteEnabled(device, enabled)
}
