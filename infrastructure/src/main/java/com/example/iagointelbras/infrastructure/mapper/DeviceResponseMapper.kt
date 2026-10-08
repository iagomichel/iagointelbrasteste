package com.example.iagointelbras.infrastructure.mapper

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.network.model.response.DeviceResponse

internal fun DeviceResponse.toDomain(): Device? {
    val serial = namespace?.takeIf(String::isNotBlank) ?: return null
    val deviceModel = model.orEmpty()
    return Device(
        serial = serial,
        productId = productId.orEmpty(),
        name = name?.takeIf(String::isNotBlank) ?: deviceModel,
        model = deviceModel,
        category = deviceModel.toDeviceCategory(),
        origin = origin?.toDeviceOrigin() ?: DeviceOrigin.UNKNOWN,
        online = status?.toDeviceOnline(),
        apiNamespace = apiNamespace(serial)
    )
}

private fun String.toDeviceOrigin() = when (lowercase()) {
    "vinculado" -> DeviceOrigin.LINKED
    "compartilhado" -> DeviceOrigin.SHARED
    else -> DeviceOrigin.UNKNOWN
}

private fun DeviceResponse.apiNamespace(serial: String): String? {
    if (isSubdevice != true) return null
    val parts = listOf(
        serial,
        parentDevice,
        parentProductId
    )
    return parts.takeIf { it.all { part -> !part.isNullOrBlank() } }?.joinToString(NAMESPACE_SEPARATOR)
}

private fun String.toDeviceCategory() = when {
    startsWith("im", ignoreCase = true) -> DeviceCategory.CAMERA
    startsWith("mfr", ignoreCase = true) -> DeviceCategory.LOCK
    else -> DeviceCategory.GENERIC
}

private fun String.toDeviceOnline() = when (lowercase()) {
    "online" -> true
    "offline" -> false
    else -> null
}

private const val NAMESPACE_SEPARATOR = "_"
