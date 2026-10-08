package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.repository.DeviceRepository

class IsConnected(private val repository: DeviceRepository) {
    suspend operator fun invoke() = repository.isConnected()
}
