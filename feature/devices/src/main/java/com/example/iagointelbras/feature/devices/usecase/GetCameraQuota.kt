package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.repository.CameraRepository

class GetCameraQuota(private val repository: CameraRepository) {
    suspend operator fun invoke() = repository.availableQuota()
}
