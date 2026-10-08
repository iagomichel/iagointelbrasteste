package com.example.iagointelbras.feature.devices.connection.viewmodel

import com.example.iagointelbras.feature.devices.common.model.UiText

data class ConnectionState(
    val token: String = "",
    val connected: Boolean = false,
    val loading: Boolean = false,
    val error: UiText? = null
)
