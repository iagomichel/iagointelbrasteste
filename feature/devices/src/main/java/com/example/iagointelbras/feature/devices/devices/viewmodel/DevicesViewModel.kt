package com.example.iagointelbras.feature.devices.devices.viewmodel

import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.common.model.messageOrDefault
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.DevicePage
import com.example.iagointelbras.feature.devices.usecase.ClearAccessToken
import com.example.iagointelbras.feature.devices.usecase.GetDevices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val getDevices: GetDevices,
    private val clearAccessToken: ClearAccessToken
) : ViewModel() {
    private val _state = MutableStateFlow(DevicesState(loading = true))
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private val pagesByOrigin = mutableMapOf<DeviceOrigin, DevicePage>()

    init {
        loadFirstPage()
    }

    fun selectOrigin(origin: DeviceOrigin) {
        if (_state.value.origin == origin) return
        val cachedPage = pagesByOrigin[origin]
        loadJob?.cancel()
        _state.update { state ->
            state.copy(
                origin = origin,
                devices = cachedPage?.devices ?: state.devices,
                page = cachedPage?.page ?: 0,
                canLoadMore = cachedPage?.hasNextPage ?: false,
                loading = cachedPage == null,
                loadingMore = false,
                error = null
            )
        }
        loadFirstPage(origin)
    }

    fun loadMore() {
        val state = _state.value
        if (state.loading || state.loadingMore || !state.canLoadMore) return
        load(state.page + 1, append = true)
    }

    fun retry() = loadFirstPage()

    fun disconnect(onDisconnected: () -> Unit) {
        viewModelScope.launch {
            clearAccessToken().onSuccess { onDisconnected() }
                .onFailure { failure -> _state.update { state -> state.copy(error = failure.messageOrDefault()) } }
        }
    }

    private fun loadFirstPage(origin: DeviceOrigin = _state.value.origin) = load(1, append = false, origin)

    private fun load(page: Int, append: Boolean, origin: DeviceOrigin = _state.value.origin) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = !append, loadingMore = append, error = null) }
            getDevices(page, _state.value.pageSize, origin).fold(
                onSuccess = { result -> updatePage(result, append, origin) },
                onFailure = { failure ->
                    _state.update { current ->
                        if (current.origin != origin) current
                        else current.copy(loading = false, loadingMore = false, error = failure.messageOrDefault())
                    }
                }
            )
        }
    }

    private fun updatePage(result: DevicePage, append: Boolean, origin: DeviceOrigin) {
        val state = _state.value
        val cachedDevices = pagesByOrigin[origin]?.devices.orEmpty()
        val devices = when {
            append && state.origin == origin -> state.devices + result.devices
            append -> cachedDevices + result.devices
            else -> result.devices
        }
        pagesByOrigin[origin] = result.copy(devices = devices)
        _state.update { current ->
            if (current.origin != origin) return@update current
            current.copy(
                devices = devices,
                page = result.page,
                loading = false,
                loadingMore = false,
                canLoadMore = result.hasNextPage
            )
        }
    }
}




