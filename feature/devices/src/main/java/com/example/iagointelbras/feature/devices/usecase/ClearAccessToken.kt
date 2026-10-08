package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.repository.DeviceRepository

class ClearAccessToken(private val repository: DeviceRepository) {
    suspend operator fun invoke() = repository.clearToken()
}
