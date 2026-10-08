package com.example.iagointelbras.feature.devices.camera.viewmodel

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.common.model.messageOrDefault
import com.example.iagointelbras.feature.devices.di.ApplicationScope
import com.example.iagointelbras.feature.devices.navigation.CameraDestination
import com.example.iagointelbras.feature.devices.navigation.toDevice
import com.example.iagointelbras.feature.devices.navigation.toDevice
import com.example.iagointelbras.domain.model.CameraSession
import com.example.iagointelbras.feature.devices.usecase.GetCameraQuota
import com.example.iagointelbras.feature.devices.usecase.StartCamera
import com.example.iagointelbras.feature.devices.usecase.StopCamera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCameraQuota: GetCameraQuota,
    private val startCamera: StartCamera,
    private val stopCamera: StopCamera,
    @param:ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope
) : ViewModel() {
    private val device = savedStateHandle.toRoute<CameraDestination>().toDevice()
    private val _state = MutableStateFlow(CameraState())
    val state = _state.asStateFlow()
    private val stoppedSessions = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var quotaJob: Job? = null
    private var startJob: Job? = null

    init { refreshQuota() }

    override fun onCleared() {
        state.value.session?.sessionId?.let(::stop)
    }

    fun refreshQuota() {
        if (_state.value.session != null || _state.value.startRequested || quotaJob?.isActive == true) return
        _state.update { it.copy(loading = true, error = null) }
        quotaJob = viewModelScope.launch {
            getCameraQuota().fold(
                onSuccess = { quota -> _state.update { it.copy(quota = quota, loading = false) } },
                onFailure = { failure -> _state.update { it.copy(loading = false, error = failure.messageOrDefault()) } }
            )
        }
    }

    fun start(channel: Int = 0, stream: Int = 0) {
        val quota = _state.value.quota?.takeIf(Double::isFinite)?.coerceAtMost(1.0)
        if (quota == null || quota <= 0.0) return _state.update { it.copy(error = UiText.Resource(R.string.camera_service_retry)) }
        if (_state.value.loading || _state.value.session != null || startJob?.isActive == true) return
        _state.update { it.copy(loading = true, error = null, startRequested = true) }
        startJob = viewModelScope.launch {
            startCamera(device, channel, stream, quota).fold(
                onSuccess = { session -> _state.update { it.copy(session = session, loading = false, startRequested = false) } },
                onFailure = { failure -> _state.update { it.copy(loading = false, error = failure.messageOrDefault(), startRequested = false) } }
            )
        }
    }

    fun stop(sessionId: String) {
        if (_state.value.session?.sessionId != sessionId || !stoppedSessions.add(sessionId)) return
        applicationScope.launch {
            stopCamera(sessionId)
        }
    }
}




