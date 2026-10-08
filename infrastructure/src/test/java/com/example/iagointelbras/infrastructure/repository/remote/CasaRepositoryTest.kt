package com.example.iagointelbras.infrastructure.repository.remote

import com.example.iagointelbras.network.api.CasaApi
import com.example.iagointelbras.network.auth.AccessTokenProvider
import com.example.iagointelbras.network.auth.BearerTokenInterceptor

import com.example.iagointelbras.domain.model.AppFailure
import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.FailureKind
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.domain.repository.TokenStore

import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CasaRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: CasaRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        repository = repositoryFor(server)
    }

    private fun repositoryFor(server: MockWebServer): CasaRepositoryImpl {
        val tokenProvider = AccessTokenProvider { "temporary-token" }
        val client = OkHttpClient.Builder().addInterceptor(BearerTokenInterceptor(tokenProvider, server.url("/"))).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(CasaApi::class.java)
        return CasaRepositoryImpl(api, FakeTokenStore())
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun listDevicesSendsPagingFilterAndMapsItems() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"status":"sucesso","data":[{"ns":"SN-9","idProduto":"lock-2","nome":"Porta","modelo":"MFR 7000","origem":"vinculado","status":"online"}]}"""))

        val result = repository.listDevices(2, 15, DeviceOrigin.SHARED).getOrThrow()
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        val body = request.body.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("/produtos/listar-dispositivos/v1", request.path)
        assertEquals("Bearer temporary-token", request.getHeader("Authorization"))
        assertTrue(body.contains("\"pagina\":2"))
        assertTrue(body.contains("\"origem\":\"compartilhados\""))
        assertEquals("SN-9", result.devices.single().serial)
        assertEquals("Porta", result.devices.single().name)
        assertEquals(DeviceOrigin.LINKED, result.devices.single().origin)
    }

    @Test
    fun mapsDeviceCategoryAvailabilityAndSubdeviceNamespace() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{"ns":"child-sn","modelo":"MFR 2030","nome":"Porta","idProduto":"child-product","origem":"compartilhado","subdispositivo":true,"status":"online","dispositivoPai":"hub-sn","idProdutoDispositivoPai":"hub-product"},{"ns":"camera-sn","modelo":"iM4 Dual","idProduto":"camera-product","subdispositivo":false,"status":"offline"},{"ns":"other-sn","modelo":"sensor","origem":"linked","status":"conectado"}]}"""))

        val devices = repository.listDevices(1, 20, DeviceOrigin.ALL).getOrThrow().devices

        assertEquals("child-sn_hub-sn_hub-product", devices[0].apiNamespace)
        assertEquals(DeviceCategory.LOCK, devices[0].category)
        assertEquals(true, devices[0].online)
        assertEquals(DeviceCategory.CAMERA, devices[1].category)
        assertEquals(DeviceOrigin.UNKNOWN, devices[1].origin)
        assertEquals(false, devices[1].online)
        assertEquals(DeviceOrigin.SHARED, devices[0].origin)
        assertEquals(DeviceCategory.GENERIC, devices[2].category)
        assertEquals(DeviceOrigin.UNKNOWN, devices[2].origin)
        assertEquals(null, devices[2].online)
    }

    @Test
    fun requestsStreamingQuotaWithSwaggerPathBodyAndBearerToken() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"disponivel":1.5}"""))

        val quota = repository.availableQuota().getOrThrow()
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!

        assertEquals("POST", request.method)
        assertEquals("/streaming/cota-disponivel/v1", request.path)
        assertEquals("Bearer temporary-token", request.getHeader("Authorization"))
        assertEquals("{}", request.body.readUtf8())
        assertEquals(1.5, quota, 0.0)
    }

    @Test
    fun reportsStreamingQuotaForbiddenWithoutExposingServerDetails() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Forbidden"}"""))

        val failure = repository.availableQuota().exceptionOrNull() as AppFailure

        assertEquals(403, failure.code)
        assertEquals(FailureKind.CAMERA_SERVICE, failure.kind)
        assertEquals(null, failure.message)
    }

    @Test
    fun unauthorizedResponseDoesNotExposeServerBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"msg":"authorization secret details"}"""))

        val error = repository.listDevices(1, 20, DeviceOrigin.ALL).exceptionOrNull()

        assertTrue(error is AppFailure)
        assertEquals(401, (error as AppFailure).code)
        assertEquals(FailureKind.INVALID_TOKEN, (error as AppFailure).kind)
        assertEquals(null, error.message)
    }

    @Test
    fun expiredTokenHasDedicatedFailureKind() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Token expirado"}"""))

        val failure = repository.listDevices(1, 20, DeviceOrigin.ALL).exceptionOrNull() as AppFailure

        assertEquals(FailureKind.EXPIRED_TOKEN, failure.kind)
    }

    @Test
    fun forbiddenDeviceRequestMapsToPermissionFailure() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Forbidden"}"""))

        val failure = repository.listDevices(1, 20, DeviceOrigin.ALL).exceptionOrNull() as AppFailure

        assertEquals(FailureKind.NO_PERMISSION, failure.kind)
    }

    @Test
    fun mapsServiceResponseCodesToSafeFailureKinds() = runBlocking {
        val cases = listOf(
            400 to FailureKind.INVALID_REQUEST,
            404 to FailureKind.NOT_FOUND,
            429 to FailureKind.RATE_LIMITED,
            503 to FailureKind.SERVICE_UNAVAILABLE
        )

        cases.forEach { (status, expected) ->
            server.enqueue(MockResponse().setResponseCode(status).setBody("""{"detail":"private"}"""))
            val failure = repository.listDevices(1, 20, DeviceOrigin.ALL).exceptionOrNull() as AppFailure
            assertEquals(expected, failure.kind)
            assertEquals(null, failure.message)
        }
    }

    @Test
    fun lockActionPostsDeviceIdentityAndDesiredState() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"sucesso"}"""))

        repository.setLockOpen(sampleDevice, true).getOrThrow()
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        val body = request.body.readUtf8()

        assertEquals("/fechaduras/controle-fechadura/v1", request.path)
        assertTrue(body.contains("\"ns\":\"SN-9\""))
        assertTrue(body.contains("\"idProduto\":\"lock-2\""))
        assertTrue(body.contains("\"aberto\":true"))
    }

    @Test
    fun lockSnapshotRequestsStatusInParallel() = runBlocking {
        repeat(2) { server.enqueue(MockResponse().setBody("{}").setBodyDelay(250, TimeUnit.MILLISECONDS)) }
        val startedAt = System.nanoTime()

        repository.getLockSnapshot(sampleDevice).getOrThrow()

        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        assertTrue(elapsedMillis < 650)
        repeat(2) { assertTrue(server.takeRequest(1, TimeUnit.SECONDS) != null) }
    }

    @Test
    fun lockSnapshotReturnsStatusesWithoutRequestingVolume() = runBlocking {
        val partialServer = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = when (request.path) {
                    "/fechaduras/status-abertura/v1" -> MockResponse().setBody("""{"aberto":true}""")
                    "/fechaduras/status-abrir-remoto/v1" -> MockResponse().setBody("""{"habilitado":true}""")
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }

        val snapshot = try {
            repositoryFor(partialServer).getLockSnapshot(sampleDevice).getOrThrow()
        } finally {
            partialServer.shutdown()
        }

        assertEquals(true, snapshot.open)
        assertEquals(true, snapshot.remoteEnabled)
        assertEquals(null, snapshot.volume)
    }

    @Test
    fun lockSnapshotDoesNotCallVolumeEndpoint() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path) {
                "/fechaduras/status-abertura/v1" -> MockResponse().setBody("""{"aberto":false}""")
                "/fechaduras/status-abrir-remoto/v1" -> MockResponse().setBody("""{"habilitado":true}""")
                else -> MockResponse().setResponseCode(404)
            }
        }
        val device = sampleDevice.copy(apiNamespace = "child-hub-parent")

        val snapshot = repository.getLockSnapshot(device).getOrThrow()
        val requests = List(2) { server.takeRequest(1, TimeUnit.SECONDS)!! }

        assertEquals(null, snapshot.volume)
        assertTrue(requests.none { it.path == "/fechaduras/volume/v1" })
    }

    @Test
    fun mapsAllLockHistoryEventsUsingDocumentedFields() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"status":"sucesso","data":[{"tempoLocal":"20261008T180244","nome":"APP","tipo":"usuarioRemoto"},{"tempoLocal":"20261008T172338","nome":"APP","tipo":"usuarioRemoto"}]}"""))

        val events = repository.getLockHistory(sampleDevice, 25).getOrThrow()
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        val body = request.body.readUtf8()

        assertEquals("/fechaduras/historico-abertura/v1", request.path)
        assertTrue(body.contains("\"ns\":\"SN-9\""))
        assertTrue(body.contains("\"quantidade\":25"))
        assertEquals(2, events.size)
        assertEquals("20261008T180244", events.first().timestamp)
        assertEquals("Ação remota", events.first().description)
        assertEquals(APPLICATION_HISTORY_METHOD, events.first().method)
        assertEquals("20261008T172338", events[1].timestamp)
    }

    @Test
    fun acceptsVideoUrlReturnedByServer() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"session_id":"s-1","monitor_url":"https://attacker.example/player"}"""))

        val session = repository.startCamera(sampleDevice, 0, 1, 0.5).getOrThrow()

        assertEquals("https://attacker.example/player", session.monitorUrl)
    }

    @Test
    fun prefersMonitorUrlWhenResponseContainsMultipleUrls() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"session_id":"s-1","monitor_url":"https://open-casainteligente.intelbras.com.br/player","url":"https://attacker.example/player","quota_gb":0.4}"""
            )
        )

        val session = repository.startCamera(sampleDevice, 0, 1, 0.5).getOrThrow()

        assertEquals("s-1", session.sessionId)
        assertEquals("https://open-casainteligente.intelbras.com.br/player", session.monitorUrl)
    }

    @Test
    fun acceptsVideoUrlReturnedOnNonDefaultPort() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"session_id":"s-1","monitor_url":"https://open-casainteligente.intelbras.com.br:8443/player"}"""))

        val session = repository.startCamera(sampleDevice, 0, 1, 0.5).getOrThrow()

        assertEquals("https://open-casainteligente.intelbras.com.br:8443/player", session.monitorUrl)
    }

    @Test
    fun reportsCameraServiceFailureWhenSessionResponseIsIncomplete() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"session_id":"s-1"}"""))

        val failure = repository.startCamera(sampleDevice, 0, 1, 0.5).exceptionOrNull() as AppFailure

        assertEquals(FailureKind.CAMERA_SERVICE, failure.kind)
        assertEquals(null, failure.message)
    }

    @Test
    fun bearerTokenIsNotSentToOtherHosts() {
        val externalServer = MockWebServer()
        externalServer.start()
        externalServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val client = OkHttpClient.Builder().addInterceptor(BearerTokenInterceptor(AccessTokenProvider { "private-token" })).build()
        val api = Retrofit.Builder().baseUrl(externalServer.url("/")).client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build())).build()
            .create(CasaApi::class.java)

        try {
            runBlocking { api.cameraQuota(emptyMap()) }
            assertTrue(externalServer.takeRequest(1, TimeUnit.SECONDS)!!.getHeader("Authorization").isNullOrBlank())
        } finally {
            externalServer.shutdown()
        }
    }

    private class FakeTokenStore : TokenStore {
        override fun read() = null
        override fun write(token: String) = Unit
        override fun clear() = Unit
    }

    private companion object {
        val sampleDevice = Device("SN-9", "lock-2", "Porta", "MFR 7000", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
    }
}



