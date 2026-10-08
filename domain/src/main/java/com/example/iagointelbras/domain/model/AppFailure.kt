package com.example.iagointelbras.domain.model

data class AppFailure(val kind: FailureKind, val code: Int? = null) : Exception()
