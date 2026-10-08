package com.example.iagointelbras.domain.repository

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.model.LockSnapshot

interface LockRepository {
    suspend fun getLockSnapshot(device: Device): Result<LockSnapshot>
    suspend fun getLockOpenStatus(device: Device): Result<Boolean?>
    suspend fun getRemoteAccessStatus(device: Device): Result<Boolean?>
    suspend fun setLockOpen(device: Device, open: Boolean): Result<Unit>
    suspend fun setLockVolume(device: Device, volume: Int): Result<Unit>
    suspend fun setRemoteEnabled(device: Device, enabled: Boolean): Result<Unit>
    suspend fun getLockHistory(device: Device, count: Int): Result<List<LockEvent>>
}
