package com.example.iagointelbras.network.api

import com.example.iagointelbras.network.model.request.CameraStartRequest
import com.example.iagointelbras.network.model.request.DeviceListRequest
import com.example.iagointelbras.network.model.request.LockControlRequest
import com.example.iagointelbras.network.model.request.LockHistoryRequest
import com.example.iagointelbras.network.model.request.LockRequest
import com.example.iagointelbras.network.model.request.LockVolumeChangeRequest
import com.example.iagointelbras.network.model.request.RemoteAccessRequest
import com.example.iagointelbras.network.model.request.StreamSessionRequest
import com.example.iagointelbras.network.model.response.CameraStartResponse
import com.example.iagointelbras.network.model.response.CameraQuotaResponse
import com.example.iagointelbras.network.model.response.DeviceListResponse
import com.example.iagointelbras.network.model.response.LockStatusResponse
import com.example.iagointelbras.network.model.response.LockControlResponse
import com.example.iagointelbras.network.model.response.LockHistoryResponse
import com.example.iagointelbras.network.model.response.LockVolumeChangeResponse
import com.example.iagointelbras.network.model.response.RemoteAccessStatusResponse
import com.example.iagointelbras.network.model.response.RemoteAccessChangeResponse
import com.example.iagointelbras.network.model.response.StopCameraResponse

import retrofit2.http.Body
import retrofit2.http.POST

interface CasaApi {
    @POST("produtos/listar-dispositivos/v1")
    suspend fun listDevices(@Body body: DeviceListRequest): DeviceListResponse

    @POST("cameras/criar-fluxo-video/v1")
    suspend fun startCamera(@Body body: CameraStartRequest): CameraStartResponse

    @POST("streaming/encerrar-sessao/v1")
    suspend fun stopCamera(@Body body: StreamSessionRequest): StopCameraResponse

    @POST("streaming/cota-disponivel/v1")
    suspend fun cameraQuota(@Body body: Map<String, @JvmSuppressWildcards Any>): CameraQuotaResponse

    @POST("fechaduras/status-abertura/v1")
    suspend fun lockStatus(@Body body: LockRequest): LockStatusResponse

    @POST("fechaduras/status-abrir-remoto/v1")
    suspend fun remoteStatus(@Body body: LockRequest): RemoteAccessStatusResponse

    @POST("fechaduras/controle-fechadura/v1")
    suspend fun controlLock(@Body body: LockControlRequest): LockControlResponse

    @POST("fechaduras/mudar-volume/v1")
    suspend fun changeVolume(@Body body: LockVolumeChangeRequest): LockVolumeChangeResponse

    @POST("fechaduras/habilitar-abrir-remoto/v1")
    suspend fun enableRemote(@Body body: RemoteAccessRequest): RemoteAccessChangeResponse

    @POST("fechaduras/historico-abertura/v1")
    suspend fun lockHistory(@Body body: LockHistoryRequest): LockHistoryResponse

}



