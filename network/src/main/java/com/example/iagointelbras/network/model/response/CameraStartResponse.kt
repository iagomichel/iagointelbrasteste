package com.example.iagointelbras.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CameraStartResponse(
    val data: CameraStartData? = null,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "monitor_url") val monitorUrl: String? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class CameraStartData(
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "monitor_url") val monitorUrl: String? = null,
    val url: String? = null
)
