package com.example.iagointelbras.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LockStatusResponse(
    val status: String? = null,
    val data: LockStatusData? = null
)

@JsonClass(generateAdapter = true)
data class LockStatusData(
    @Json(name = "aberto") val isOpen: Boolean? = null
)
