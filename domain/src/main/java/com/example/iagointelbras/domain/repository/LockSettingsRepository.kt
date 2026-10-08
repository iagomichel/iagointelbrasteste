package com.example.iagointelbras.domain.repository

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.LockEvent

interface LockSettingsRepository {
    suspend fun readVolume(device: Device): Int?
    suspend fun saveVolume(device: Device, volume: Int)
    suspend fun readActions(device: Device): List<LockEvent>
    suspend fun saveAction(device: Device, event: LockEvent)
}
