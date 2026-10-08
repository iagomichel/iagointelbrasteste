package com.example.iagointelbras.feature.devices.connection.viewmodel

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.common.model.messageOrDefault
import com.example.iagointelbras.feature.devices.usecase.IsConnected
import com.example.iagointelbras.feature.devices.usecase.SaveAccessToken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val saveAccessToken: SaveAccessToken,
    private val isConnected: IsConnected
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectionState(loading = true))
    val state = _state.asStateFlow()
    private var connectJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val connected = isConnected()
                _state.update { it.copy(connected = connected, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.messageOrDefault()) }
            }
        }
    }

    fun updateToken(token: String) = _state.update { it.copy(token = token, error = null) }

    fun connect() {
        if (_state.value.loading || connectJob?.isActive == true) return
        val token = _state.value.token.trim()
        if (token.isBlank()) return _state.update { it.copy(error = UiText.Resource(R.string.enter_temporary_token)) }
        _state.update { it.copy(loading = true, error = null) }
        connectJob = viewModelScope.launch {
            saveAccessToken(token).fold(
                onSuccess = { _state.update { it.copy(token = "", loading = false, connected = true) } },
                onFailure = { failure -> _state.update { it.copy(loading = false, error = failure.messageOrDefault()) } }
            )
        }
    }
}




