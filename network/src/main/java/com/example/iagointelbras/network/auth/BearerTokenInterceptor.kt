package com.example.iagointelbras.network.auth

import com.example.iagointelbras.network.config.CasaHosts

import okhttp3.Interceptor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import java.io.IOException

class InvalidAccessTokenException : IOException()

fun String.requireValidAccessToken() {
    val value = trim()
    val token = if (value.startsWith("Bearer ", ignoreCase = true)) value.substringAfter(' ')
        .trim() else value
    if (token.isBlank() || token.any { it.code !in MIN_HEADER_CHAR..MAX_HEADER_CHAR }) {
        throw InvalidAccessTokenException()
    }
}

private const val MIN_HEADER_CHAR = 0x21
private const val MAX_HEADER_CHAR = 0x7e

fun interface AccessTokenProvider {
    fun readToken(): String?
}

class BearerTokenInterceptor(
    private val tokenProvider: AccessTokenProvider,
    private val allowedOrigin: HttpUrl = CasaHosts.API_BASE_URL.toHttpUrl()
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!original.url.hasSameOrigin(allowedOrigin)) {
            return chain.proceed(original.newBuilder().removeHeader("Authorization").build())
        }
        val token = tokenProvider.readToken()?.trim().orEmpty()
        if (token.isNotEmpty()) token.requireValidAccessToken()
        val requestBuilder = original.newBuilder().removeHeader("Authorization")
        if (token.isNotEmpty()) requestBuilder.header("Authorization", token.asBearerToken())
        val request = requestBuilder.header("Accept", "application/json").build()
        return chain.proceed(request)
    }

    private fun String.asBearerToken() =
        if (startsWith("Bearer ", ignoreCase = true)) this else "Bearer $this"

}

private fun HttpUrl.hasSameOrigin(other: HttpUrl) =
    scheme == other.scheme && host == other.host && port == other.port



