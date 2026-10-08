package com.example.iagointelbras.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CameraStartRequest(
    @param:Json(name = "ns") val namespace: String,
    @param:Json(name = "canalVideo") val videoChannel: Int,
    val streamId: Int,
    @param:Json(name = "stream_gb") val streamGb: Double
)
