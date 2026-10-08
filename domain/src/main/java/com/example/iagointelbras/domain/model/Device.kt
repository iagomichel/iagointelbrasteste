package com.example.iagointelbras.domain.model

data class Device(
    val serial: String,
    val productId: String,
    val name: String,
    val model: String,
    val category: DeviceCategory,
    val origin: DeviceOrigin,
    val online: Boolean?,
    val apiNamespace: String? = null
)
