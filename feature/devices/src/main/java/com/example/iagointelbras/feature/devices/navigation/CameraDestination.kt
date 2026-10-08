package com.example.iagointelbras.feature.devices.navigation

import kotlinx.serialization.Serializable

@Serializable
data class CameraDestination(
    val serial: String,
    val productId: String,
    val name: String,
    val model: String,
    val category: String,
    val origin: String,
    val online: Boolean?,
    val apiNamespace: String?
)
