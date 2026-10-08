package com.example.iagointelbras.domain.repository

interface TokenStore {
    fun read(): String?
    fun write(token: String)
    fun clear()
}
