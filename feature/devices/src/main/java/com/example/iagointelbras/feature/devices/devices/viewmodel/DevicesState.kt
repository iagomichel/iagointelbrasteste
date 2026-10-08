package com.example.iagointelbras.feature.devices.devices.viewmodel

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.feature.devices.common.model.UiText

data class DevicesState(
    val devices: List<Device> = emptyList(),
    val origin: DeviceOrigin = DeviceOrigin.ALL,
    val page: Int = 0,
    val pageSize: Int = 20,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: UiText? = null
)
