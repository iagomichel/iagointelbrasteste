package com.example.iagointelbras.feature.devices

import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.common.model.toUiText

import com.example.iagointelbras.domain.model.AppFailure
import com.example.iagointelbras.domain.model.FailureKind

import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {
    @Test
    fun mapsFailureKindsToLocalizedResources() {
        val expected = mapOf(
            FailureKind.INVALID_TOKEN to R.string.invalid_token,
            FailureKind.EXPIRED_TOKEN to R.string.expired_token,
            FailureKind.NO_PERMISSION to R.string.no_permission,
            FailureKind.CAMERA_SERVICE to R.string.camera_service_retry,
            FailureKind.NETWORK to R.string.network_error,
            FailureKind.INVALID_REQUEST to R.string.invalid_request,
            FailureKind.NOT_FOUND to R.string.resource_not_found,
            FailureKind.RATE_LIMITED to R.string.rate_limited,
            FailureKind.SERVICE_UNAVAILABLE to R.string.service_unavailable,
            FailureKind.UNKNOWN to R.string.operation_failed
        )

        expected.forEach { (kind, resource) ->
            assertEquals(UiText.Resource(resource), AppFailure(kind).toUiText())
        }
    }

    @Test
    fun hidesUnexpectedExceptionDetails() {
        assertEquals(
            UiText.Resource(R.string.operation_failed),
            IllegalStateException("private response body").toUiText()
        )
    }
}



