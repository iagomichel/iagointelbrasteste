package com.example.iagointelbras.network.model.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LockControlResponse(
    val status: String? = null
)
