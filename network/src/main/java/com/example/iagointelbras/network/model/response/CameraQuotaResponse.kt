package com.example.iagointelbras.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CameraQuotaResponse(
    @Json(name = "disponivel") val available: Double? = null
)
