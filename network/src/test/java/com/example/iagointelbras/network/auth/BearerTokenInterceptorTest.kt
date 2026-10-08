package com.example.iagointelbras.network.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BearerTokenInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun startServer() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun addsBearerHeaderToAllowedHost() {
        val response = execute("test-token")

        assertEquals("Bearer test-token", response.getHeader("Authorization"))
        assertEquals("application/json", response.getHeader("Accept"))
    }

    @Test
    fun doesNotDuplicateBearerPrefix() {
        val response = execute("Bearer test-token")

        assertEquals("Bearer test-token", response.getHeader("Authorization"))
    }

    @Test
    fun doesNotSendTokenToAnotherHost() {
        val allowedOrigin = server.url("/").newBuilder().host("different.example").build()
        val response = execute("test-token", allowedOrigin = allowedOrigin)

        assertEquals(null, response.getHeader("Authorization"))
        assertEquals(null, response.getHeader("Accept"))
    }

    @Test
    fun stripsExistingAuthorizationFromAnotherHost() {
        server.enqueue(MockResponse())
        val allowedOrigin = server.url("/").newBuilder().host("different.example").build()
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(AccessTokenProvider { "test-token" }, allowedOrigin))
            .build()
        val request = Request.Builder()
            .url(server.url("/"))
            .header("Authorization", "Bearer unrelated-token")
            .build()

        client.newCall(request).execute().use { }

        assertEquals(null, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun replacesCallerAuthorizationWithStoredToken() {
        val response = execute("trusted-token", existingAuthorization = "Bearer caller-token")

        assertEquals("Bearer trusted-token", response.getHeader("Authorization"))
    }

    @Test
    fun stripsCallerAuthorizationWhenStoredTokenIsMissing() {
        val response = execute(null, existingAuthorization = "Bearer caller-token")

        assertEquals(null, response.getHeader("Authorization"))
    }

    @Test
    fun doesNotSendTokenToHttpWhenProductionOriginIsHttps() {
        server.enqueue(MockResponse())
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(AccessTokenProvider { "test-token" }))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use { }

        assertEquals(null, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun doesNotSendTokenToDifferentPortOnSameHost() {
        val allowedOrigin = server.url("/").newBuilder().port(server.port + 1).build()
        val response = execute("test-token", allowedOrigin = allowedOrigin)

        assertEquals(null, response.getHeader("Authorization"))
        assertEquals(null, response.getHeader("Accept"))
    }

    @Test
    fun rejectsHeaderControlCharactersWithoutExposingToken() {
        val secret = "private-token\r\nInjected: value"
        val failure = runCatching { execute(secret) }.exceptionOrNull()

        assertTrue(failure is InvalidAccessTokenException)
        assertFalse(failure?.message.orEmpty().contains(secret))
    }

    @Test
    fun omitsAuthorizationWhenTokenIsMissing() {
        val response = execute(null)

        assertEquals(null, response.getHeader("Authorization"))
    }

    private fun execute(
        token: String?,
        allowedOrigin: HttpUrl = server.url("/"),
        existingAuthorization: String? = null
    ): RecordedRequest {
        server.enqueue(MockResponse())
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(AccessTokenProvider { token }, allowedOrigin))
            .build()
        val request = Request.Builder().url(server.url("/")).apply {
            existingAuthorization?.let { header("Authorization", it) }
        }.build()
        client.newCall(request).execute().use { }
        return server.takeRequest()
    }
}



