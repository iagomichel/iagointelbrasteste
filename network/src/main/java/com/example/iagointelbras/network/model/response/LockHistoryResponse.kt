package com.example.iagointelbras.network.model.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LockHistoryResponse(
    val status: String? = null,
    val data: List<LockHistoryEventResponse>? = null
)
