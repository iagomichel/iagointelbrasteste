package com.example.iagointelbras.domain.model

enum class FailureKind {
    INVALID_TOKEN,
    EXPIRED_TOKEN,
    NO_PERMISSION,
    CAMERA_SERVICE,
    NETWORK,
    INVALID_REQUEST,
    NOT_FOUND,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    UNKNOWN
}
