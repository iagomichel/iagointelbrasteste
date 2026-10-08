package com.example.iagointelbras.infrastructure.repository.remote

import com.example.iagointelbras.domain.model.AppFailure
import com.example.iagointelbras.domain.model.FailureKind
import com.example.iagointelbras.network.auth.InvalidAccessTokenException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiResultHandler @Inject constructor() {
    suspend fun <T> execute(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error.toAppFailure())
    }

    private fun Throwable.toAppFailure(): Throwable = when (this) {
        is AppFailure -> this
        is HttpException -> AppFailure(failureKind(), code())
        is InvalidAccessTokenException -> AppFailure(FailureKind.INVALID_TOKEN, HTTP_UNAUTHORIZED)
        is IOException -> AppFailure(FailureKind.NETWORK)
        is IllegalArgumentException -> AppFailure(FailureKind.INVALID_REQUEST)
        else -> AppFailure(FailureKind.UNKNOWN)
    }

    private fun HttpException.failureKind() = when {
        hasExpiredTokenMessage() -> FailureKind.EXPIRED_TOKEN
        code() == HTTP_BAD_REQUEST -> FailureKind.INVALID_REQUEST
        code() == HTTP_UNAUTHORIZED -> FailureKind.INVALID_TOKEN
        code() == HTTP_FORBIDDEN && !isCameraRequest() -> FailureKind.NO_PERMISSION
        code() == HTTP_FORBIDDEN || code() == HTTP_PAYMENT_REQUIRED -> FailureKind.CAMERA_SERVICE
        code() == HTTP_NOT_FOUND -> FailureKind.NOT_FOUND
        code() == HTTP_TOO_MANY_REQUESTS -> FailureKind.RATE_LIMITED
        code() in HTTP_SERVER_ERROR..HTTP_SERVER_ERROR_MAX -> FailureKind.SERVICE_UNAVAILABLE
        else -> FailureKind.UNKNOWN
    }

    private fun HttpException.hasExpiredTokenMessage() = runCatching {
        response()?.errorBody()?.string()?.contains(EXPIRED_TOKEN_MESSAGE, ignoreCase = true) == true
    }.getOrDefault(false)

    private fun HttpException.isCameraRequest() = response()?.raw()?.request?.url?.encodedPath?.let { path ->
        CAMERA_PATH_PREFIXES.any(path::startsWith)
    } == true

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_PAYMENT_REQUIRED = 402
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
        const val HTTP_SERVER_ERROR_MAX = 599
        const val EXPIRED_TOKEN_MESSAGE = "token expirado"
        val CAMERA_PATH_PREFIXES = listOf("/cameras/", "/streaming/")
    }
}
