package com.example.iagointelbras.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StreamSessionRequest(
    @param:Json(name = "session_id") val sessionId: String
)

