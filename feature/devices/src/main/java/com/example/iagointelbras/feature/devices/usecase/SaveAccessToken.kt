package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.repository.DeviceRepository

class SaveAccessToken(private val repository: DeviceRepository) {
    suspend operator fun invoke(token: String) =
        repository.saveToken(token.trim())
}
