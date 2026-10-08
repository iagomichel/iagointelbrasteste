package com.example.iagointelbras.infrastructure.repository.remote.device

import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.DevicePage
import com.example.iagointelbras.domain.repository.DeviceRepository
import com.example.iagointelbras.domain.repository.TokenStore
import com.example.iagointelbras.infrastructure.mapper.toDomain
import com.example.iagointelbras.infrastructure.repository.remote.ApiResultHandler
import com.example.iagointelbras.network.api.CasaApi
import com.example.iagointelbras.network.auth.requireValidAccessToken
import com.example.iagointelbras.network.model.request.DeviceListRequest
import com.example.iagointelbras.network.model.response.DeviceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val api: CasaApi,
    private val tokenStore: TokenStore,
    private val resultHandler: ApiResultHandler
) : DeviceRepository {
    override suspend fun isConnected() = withContext(Dispatchers.IO) {
        !tokenStore.read().isNullOrBlank()
    }

    override suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        resultHandler.execute {
            require(token.isNotBlank())
            token.requireValidAccessToken()
            tokenStore.write(token)
        }
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        resultHandler.execute { tokenStore.clear() }
    }

    override suspend fun listDevices(page: Int, pageSize: Int, origin: DeviceOrigin) =
        resultHandler.execute {
            val response = api.listDevices(DeviceListRequest(pageSize, page, origin.apiValue))
            response.status?.takeIf { !it.equals(DEVICE_LIST_SUCCESS_STATUS, ignoreCase = true) }?.let {
                throw IllegalStateException(DEVICE_LIST_FAILURE)
            }
            val devices = response.data.orEmpty().mapNotNull(DeviceResponse::toDomain)
            val hasNext = response.hasNextPage
                ?: response.totalPages?.let { page < it }
                ?: (devices.size == pageSize)
            DevicePage(devices, page, pageSize, response.totalPages ?: (page + if (hasNext) 1 else 0), hasNext)
        }
}

private const val DEVICE_LIST_SUCCESS_STATUS = "sucesso"
private const val DEVICE_LIST_FAILURE = "Device list request failed"
