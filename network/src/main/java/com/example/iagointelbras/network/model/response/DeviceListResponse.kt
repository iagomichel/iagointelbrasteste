package com.example.iagointelbras.network.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceListResponse(
    val status: String? = null,
    val data: List<DeviceResponse>? = null,
    @Json(name = "totalPaginas") val totalPages: Int? = null,
    @Json(name = "hasNextPage") val hasNextPage: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DeviceResponse(
    @Json(name = "ns") val namespace: String? = null,
    @Json(name = "idProduto") val productId: String? = null,
    @Json(name = "modelo") val model: String? = null,
    @Json(name = "nome") val name: String? = null,
    val status: String? = null,
    @Json(name = "origem") val origin: String? = null,
    @Json(name = "subdispositivo") val isSubdevice: Boolean? = null,
    @Json(name = "dispositivoPai") val parentDevice: String? = null,
    @Json(name = "idProdutoDispositivoPai") val parentProductId: String? = null
)
