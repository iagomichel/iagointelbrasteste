package com.example.iagointelbras.feature.devices.lock.ui

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.ui.EmptyState
import com.example.iagointelbras.feature.devices.common.ui.ErrorMessage
import com.example.iagointelbras.feature.devices.common.ui.LoadingState
import com.example.iagointelbras.feature.devices.lock.viewmodel.LockState
import com.example.iagointelbras.feature.devices.lock.viewmodel.LockViewModel
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.LockSnapshot
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable
fun LockScreen(viewModel: LockViewModel, onBack: () -> Unit, onCamera: (Device) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LockContent(
        state = state,
        onRefresh = viewModel::refresh,
        onOpen = viewModel::setOpen,
        onVolume = viewModel::setVolume,
        onRemote = viewModel::setRemote,
        onBack = onBack,
        onCamera = { state.device?.let(onCamera) },
        onHistoryRetry = viewModel::refreshHistory
    )
}

@Composable
fun LockContent(
    state: LockState,
    onRefresh: () -> Unit,
    onOpen: (Boolean) -> Unit,
    onVolume: (Int) -> Unit,
    onRemote: (Boolean) -> Unit,
    onBack: () -> Unit,
    onCamera: () -> Unit,
    onHistoryRetry: () -> Unit = {}
) {
    var pendingAction by remember { mutableStateOf<Boolean?>(null) }
    val device = state.device
    Scaffold(
        containerColor = colorResource(R.color.device_detail_background),
        topBar = { LockTopBar(device?.name, onBack) }
    ) { padding -> LockBody(state, padding, onRefresh, onRemote, onVolume, onCamera, onHistoryRetry) { pendingAction = it } }
    pendingAction?.let { open ->
        LockConfirmation(open, onDismiss = { pendingAction = null }) {
            pendingAction = null
            onOpen(open)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LockTopBar(deviceName: String?, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(deviceName?.takeIf(String::isNotBlank) ?: stringResource(R.string.generic_device), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.device_detail_top_bar))
    )
}

@Composable
private fun LockBody(
    state: LockState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onRemote: (Boolean) -> Unit,
    onVolume: (Int) -> Unit,
    onCamera: () -> Unit,
    onHistoryRetry: () -> Unit,
    onRequestOpen: (Boolean) -> Unit
) {
    val device = state.device
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (device?.category == DeviceCategory.LOCK) LockIdentity(device) else DeviceIdentity(device)
            state.error?.let { ErrorMessage(it) }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
            when (device?.category) {
                DeviceCategory.LOCK -> LockControls(state, onRefresh, onRemote, onVolume, onRequestOpen)
                DeviceCategory.CAMERA -> Button(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.watch_live))
                }
                else -> EmptyState(stringResource(R.string.device_details_title), stringResource(R.string.device_details_message))
            }
            if (device?.category == DeviceCategory.LOCK) {
                LockHistory(state.history, state.historyLoading, state.historyError, onHistoryRetry)
            }
        }
    }
}


@Preview(showBackground = true, name = "Fechadura")
@Composable
private fun LockContentPreview() {
    val device = Device("LOCK-001", "lock", "Porta de entrada", "MFR 2030", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
    MaterialTheme {
        LockContent(LockState(device = device, snapshot = LockSnapshot(
            open = false,
            remoteEnabled = true,
            volume = 2
        ), loading = false), {}, {}, {}, {}, {}, {})
    }
}
