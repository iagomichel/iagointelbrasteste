package com.example.iagointelbras.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LockHistoryEventResponse(
    @Json(name = "tempoLocal") val localTime: String? = null,
    @Json(name = "nome") val name: String? = null,
    @Json(name = "tipo") val type: String? = null
)
