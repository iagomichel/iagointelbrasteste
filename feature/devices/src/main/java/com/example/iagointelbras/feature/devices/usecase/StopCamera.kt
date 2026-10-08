package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.repository.CameraRepository

class StopCamera(private val repository: CameraRepository) {
    suspend operator fun invoke(sessionId: String) =
        repository.stopCamera(sessionId)
}
