package com.example.iagointelbras.infrastructure.repository.remote

import com.example.iagointelbras.domain.repository.CameraRepository
import com.example.iagointelbras.domain.repository.DeviceRepository
import com.example.iagointelbras.domain.repository.LockRepository
import com.example.iagointelbras.domain.repository.TokenStore
import com.example.iagointelbras.infrastructure.repository.remote.camera.CameraRepositoryImpl
import com.example.iagointelbras.infrastructure.repository.remote.device.DeviceRepositoryImpl
import com.example.iagointelbras.infrastructure.repository.remote.lock.LockRepositoryImpl
import com.example.iagointelbras.network.api.CasaApi

internal class CasaRepositoryImpl(api: CasaApi, tokenStore: TokenStore) :
    DeviceRepository by DeviceRepositoryImpl(api, tokenStore, ApiResultHandler()),
    LockRepository by LockRepositoryImpl(api, ApiResultHandler()),
    CameraRepository by CameraRepositoryImpl(api, ApiResultHandler())
