package com.example.iagointelbras.feature.devices.camera.viewmodel

import com.example.iagointelbras.domain.model.CameraSession
import com.example.iagointelbras.feature.devices.common.model.UiText

data class CameraState(
    val quota: Double? = null,
    val session: CameraSession? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    val startRequested: Boolean = false
)
