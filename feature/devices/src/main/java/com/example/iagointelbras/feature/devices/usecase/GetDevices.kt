package com.example.iagointelbras.feature.devices.usecase

import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.repository.DeviceRepository

class GetDevices(private val repository: DeviceRepository) {
    suspend operator fun invoke(page: Int, pageSize: Int, origin: DeviceOrigin) =
        repository.listDevices(page, pageSize, origin)
}
