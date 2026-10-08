package com.example.iagointelbras.domain.model

data class LockEvent(
    val timestamp: String,
    val description: String,
    val method: String,
    val action: LockAction? = null
)
