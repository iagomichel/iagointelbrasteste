package com.example.iagointelbras.network.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceListRequest(
    @param:Json(name = "tamanhoPagina") val pageSize: Int,
    @param:Json(name = "pagina") val page: Int,
    @param:Json(name = "origem") val origin: String
)
