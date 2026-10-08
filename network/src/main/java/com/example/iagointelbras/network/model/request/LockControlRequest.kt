package com.example.iagointelbras.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LockControlRequest(
    @param:Json(name = "ns") val namespace: String,
    @param:Json(name = "idProduto") val productId: String,
    @param:Json(name = "aberto") val isOpen: Boolean
)
