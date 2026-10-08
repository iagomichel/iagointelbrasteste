package com.example.iagointelbras.domain.model

enum class DeviceOrigin(val apiValue: String) {
    ALL("todos"),
    LINKED("vinculados"),
    SHARED("compartilhados"),
    UNKNOWN("")
}
