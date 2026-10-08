package com.example.iagointelbras.infrastructure.repository.remote

import com.example.iagointelbras.network.api.CasaApi

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.infrastructure.repository.remote.camera.CameraRepositoryImpl
import com.example.iagointelbras.infrastructure.repository.remote.lock.LockRepositoryImpl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(AndroidJUnit4::class)
class CasaRepositorySecurityTest {
    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun acceptsVideoUrlUsingNonDefaultPort() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"session_id":"session-1","monitor_url":"https://open-casainteligente.intelbras.com.br:8443/player"}"""
            )
        )

        val session = cameraRepository().startCamera(sampleCamera, 0, 0, 0.5).getOrThrow()

        assertEquals("https://open-casainteligente.intelbras.com.br:8443/player", session.monitorUrl)
    }

    @Test
    fun mapsLockActionWhenGenericDescriptionPrecedesStateField() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"data":[{"timestamp":"2026-10-07T10:00:00Z","acao":"evento de fechadura","aberto":true},{"timestamp":"2026-10-07T10:05:00Z","acao":"evento de fechadura","statusAbertura":false}]}"""
            )
        )

        val events = lockRepository().getLockHistory(sampleLock, 50).getOrThrow()

        assertEquals(listOf(LockAction.OPENED, LockAction.CLOSED), events.map { it.action })
    }

    private fun cameraRepository() = CameraRepositoryImpl(api(), ApiResultHandler())

    private fun lockRepository() = LockRepositoryImpl(api(), ApiResultHandler())

    private fun api() = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .build()
        .create(CasaApi::class.java)

    private companion object {
        val sampleCamera = Device(
            "camera-1",
            "product-1",
            "Sala",
            "iM4",
            DeviceCategory.CAMERA,
            DeviceOrigin.LINKED,
            true
        )
        val sampleLock = Device(
            "lock-1",
            "product-2",
            "Entrada",
            "MFR",
            DeviceCategory.LOCK,
            DeviceOrigin.LINKED,
            true
        )

    }
}



