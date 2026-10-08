package com.example.iagointelbras.feature.devices.common.model

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.domain.model.AppFailure
import com.example.iagointelbras.domain.model.FailureKind

import androidx.annotation.StringRes

sealed interface UiText {
    data class Resource(@field:StringRes val id: Int) : UiText
}

fun Throwable.toUiText(): UiText = UiText.Resource(
    when (this) {
        is AppFailure -> kind.resourceId()
        else -> R.string.operation_failed
    }
)

internal fun Throwable.messageOrDefault() = toUiText()

private fun FailureKind.resourceId() = when (this) {
    FailureKind.INVALID_TOKEN -> R.string.invalid_token
    FailureKind.EXPIRED_TOKEN -> R.string.expired_token
    FailureKind.NO_PERMISSION -> R.string.no_permission
    FailureKind.CAMERA_SERVICE -> R.string.camera_service_retry
    FailureKind.NETWORK -> R.string.network_error
    FailureKind.INVALID_REQUEST -> R.string.invalid_request
    FailureKind.NOT_FOUND -> R.string.resource_not_found
    FailureKind.RATE_LIMITED -> R.string.rate_limited
    FailureKind.SERVICE_UNAVAILABLE -> R.string.service_unavailable
    FailureKind.UNKNOWN -> R.string.operation_failed
}



