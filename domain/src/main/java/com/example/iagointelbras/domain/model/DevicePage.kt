package com.example.iagointelbras.domain.model

data class DevicePage(
    val devices: List<Device>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasNextPage: Boolean = page < totalPages
)
