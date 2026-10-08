package com.example.iagointelbras.infrastructure.mapper

import com.example.iagointelbras.domain.model.LockAction

internal fun String.toLockAction() = when (uppercase()) {
    "OPENED" -> LockAction.OPENED
    "CLOSED" -> LockAction.CLOSED
    else -> null
}
