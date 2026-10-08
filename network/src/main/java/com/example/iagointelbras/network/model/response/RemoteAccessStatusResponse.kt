package com.example.iagointelbras.network.model.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAccessStatusResponse(
    val status: String? = null,
    val data: RemoteAccessStatusData? = null
)
