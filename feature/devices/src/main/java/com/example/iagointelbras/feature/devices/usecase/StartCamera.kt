package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.repository.CameraRepository

class StartCamera(private val repository: CameraRepository) {
    suspend operator fun invoke(device: Device, channel: Int, stream: Int, streamGb: Double) =
        repository.startCamera(device, channel, stream, streamGb)
}
