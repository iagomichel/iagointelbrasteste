package com.example.iagointelbras.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LockHistoryRequest(
    @param:Json(name = "ns") val namespace: String,
    @param:Json(name = "quantidade") val quantity: Int
)
