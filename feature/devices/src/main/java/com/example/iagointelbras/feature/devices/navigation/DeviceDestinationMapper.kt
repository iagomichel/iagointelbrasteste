package com.example.iagointelbras.feature.devices.navigation

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin

fun Device.toLockDestination() = LockDestination(
    serial = serial,
    productId = productId,
    name = name,
    model = model,
    category = category.name,
    origin = origin.name,
    online = online,
    apiNamespace = apiNamespace
)

fun Device.toCameraDestination() = CameraDestination(
    serial = serial,
    productId = productId,
    name = name,
    model = model,
    category = category.name,
    origin = origin.name,
    online = online,
    apiNamespace = apiNamespace
)

internal fun LockDestination.toDevice() = Device(
    serial = serial,
    productId = productId,
    name = name,
    model = model,
    category = category.toDeviceCategory(),
    origin = origin.toDeviceOrigin(),
    online = online,
    apiNamespace = apiNamespace
)

internal fun CameraDestination.toDevice() = Device(
    serial = serial,
    productId = productId,
    name = name,
    model = model,
    category = category.toDeviceCategory(),
    origin = origin.toDeviceOrigin(),
    online = online,
    apiNamespace = apiNamespace
)

private fun String.toDeviceCategory() =
    DeviceCategory.entries.firstOrNull { it.name == this } ?: DeviceCategory.GENERIC

private fun String.toDeviceOrigin() =
    DeviceOrigin.entries.firstOrNull { it.name == this } ?: DeviceOrigin.UNKNOWN
