package com.example.iagointelbras.feature.devices.lock.viewmodel

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.model.LockSnapshot
import com.example.iagointelbras.feature.devices.common.model.UiText

data class LockState(
    val device: Device? = null,
    val snapshot: LockSnapshot? = null,
    val history: List<LockEvent> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: UiText? = null,
    val loading: Boolean = true,
    val acting: Boolean = false,
    val error: UiText? = null
)
