package com.example.iagointelbras.domain.repository

import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.DevicePage

interface DeviceRepository {
    suspend fun isConnected(): Boolean
    suspend fun listDevices(page: Int, pageSize: Int, origin: DeviceOrigin): Result<DevicePage>
    suspend fun saveToken(token: String): Result<Unit>
    suspend fun clearToken(): Result<Unit>
}
