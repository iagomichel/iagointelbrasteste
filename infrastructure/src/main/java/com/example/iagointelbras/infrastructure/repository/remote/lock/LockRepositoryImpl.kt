package com.example.iagointelbras.infrastructure.repository.remote.lock

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.model.LockSnapshot
import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.repository.LockRepository
import com.example.iagointelbras.network.api.CasaApi
import com.example.iagointelbras.network.model.request.LockControlRequest
import com.example.iagointelbras.network.model.request.LockHistoryRequest
import com.example.iagointelbras.network.model.request.LockRequest
import com.example.iagointelbras.network.model.request.LockVolumeChangeRequest
import com.example.iagointelbras.network.model.request.RemoteAccessRequest
import com.example.iagointelbras.infrastructure.repository.remote.ApiResultHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockRepositoryImpl @Inject constructor(
    private val api: CasaApi,
    private val resultHandler: ApiResultHandler
) : LockRepository {
    override suspend fun getLockSnapshot(device: Device) = resultHandler.execute {
        val request = device.toLockRequest()
        supervisorScope {
            val status = async { api.lockStatus(request) }
            val remote = async { api.remoteStatus(request) }
            LockSnapshot(
                status.await().data?.isOpen,
                remote.await().data?.enabled,
                null
            )
        }
    }

    override suspend fun getLockOpenStatus(device: Device) = resultHandler.execute {
        api.lockStatus(device.toLockRequest()).data?.isOpen
    }

    override suspend fun getRemoteAccessStatus(device: Device) = resultHandler.execute {
        api.remoteStatus(device.toLockRequest()).data?.enabled
    }

    override suspend fun setLockOpen(device: Device, open: Boolean) = resultHandler.execute {
        api.controlLock(LockControlRequest(device.requestNamespace(), device.productId, open))
        Unit
    }

    override suspend fun setLockVolume(device: Device, volume: Int) = resultHandler.execute {
        require(volume in MIN_LOCK_VOLUME..MAX_LOCK_VOLUME)
        api.changeVolume(LockVolumeChangeRequest(device.requestNamespace(), device.productId, volume))
        Unit
    }

    override suspend fun setRemoteEnabled(device: Device, enabled: Boolean) = resultHandler.execute {
        api.enableRemote(RemoteAccessRequest(device.requestNamespace(), device.productId, enabled))
        Unit
    }

    override suspend fun getLockHistory(device: Device, count: Int) = resultHandler.execute {
        api.lockHistory(LockHistoryRequest(device.requestNamespace(), count))
            .data.orEmpty()
            .map { event ->
                LockEvent(
                    timestamp = event.localTime.orEmpty(),
                    description = if (event.type.equals("usuarioRemoto", ignoreCase = true)) {
                        "Ação remota"
                    } else {
                        event.type.orEmpty()
                    },
                    method = if (event.name.equals("APP", ignoreCase = true)) {
                        APPLICATION_HISTORY_METHOD
                    } else {
                        event.name.orEmpty()
                    }
                )
            }
    }

    private fun Device.toLockRequest() = LockRequest(requestNamespace(), productId)

    private fun Device.requestNamespace() = apiNamespace?.takeIf(String::isNotBlank) ?: serial

    private companion object {
        const val MIN_LOCK_VOLUME = 0
        const val MAX_LOCK_VOLUME = 3
    }
}
