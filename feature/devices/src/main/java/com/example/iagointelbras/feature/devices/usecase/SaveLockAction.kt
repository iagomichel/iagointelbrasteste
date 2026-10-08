package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.repository.LockSettingsRepository

class SaveLockAction(private val repository: LockSettingsRepository) {
    suspend operator fun invoke(device: Device, event: LockEvent) =
        repository.saveAction(device, event)
}
