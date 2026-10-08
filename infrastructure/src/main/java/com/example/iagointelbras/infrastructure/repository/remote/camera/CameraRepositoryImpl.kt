package com.example.iagointelbras.infrastructure.repository.remote.camera

import com.example.iagointelbras.domain.model.AppFailure
import com.example.iagointelbras.domain.model.CameraSession
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.FailureKind
import com.example.iagointelbras.domain.repository.CameraRepository
import com.example.iagointelbras.network.api.CasaApi
import com.example.iagointelbras.network.model.request.CameraStartRequest
import com.example.iagointelbras.network.model.request.StreamSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.iagointelbras.infrastructure.repository.remote.ApiResultHandler

@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val api: CasaApi,
    private val resultHandler: ApiResultHandler
) : CameraRepository {
    override suspend fun startCamera(device: Device, channel: Int, stream: Int, streamGb: Double) =
        resultHandler.execute {
            require(streamGb > 0.0)
            val response = api.startCamera(CameraStartRequest(device.serial, channel, stream, streamGb))
            val sessionId = response.data?.sessionId ?: response.sessionId
                ?: throw AppFailure(FailureKind.CAMERA_SERVICE)
            val monitorUrl = response.data?.monitorUrl ?: response.data?.url
                ?: response.monitorUrl ?: response.url
                ?: throw AppFailure(FailureKind.CAMERA_SERVICE)
            CameraSession(sessionId, monitorUrl)
        }

    override suspend fun stopCamera(sessionId: String) = resultHandler.execute {
        api.stopCamera(StreamSessionRequest(sessionId))
        Unit
    }

    override suspend fun availableQuota() = withContext(Dispatchers.IO) {
        resultHandler.execute {
            api.cameraQuota(emptyMap()).available
                ?: throw AppFailure(FailureKind.CAMERA_SERVICE)
        }
    }
}
