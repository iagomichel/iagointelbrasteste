package com.example.iagointelbras.domain.repository

import com.example.iagointelbras.domain.model.CameraSession
import com.example.iagointelbras.domain.model.Device

interface CameraRepository {
    suspend fun startCamera(device: Device, channel: Int, stream: Int, streamGb: Double): Result<CameraSession>
    suspend fun stopCamera(sessionId: String): Result<Unit>
    suspend fun availableQuota(): Result<Double>
}
