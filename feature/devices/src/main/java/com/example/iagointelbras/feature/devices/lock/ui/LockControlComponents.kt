package com.example.iagointelbras.feature.devices.lock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.LockSnapshot
import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.lock.viewmodel.LockState

@Composable
internal fun LockIdentity(device: Device) {
    Column(Modifier.fillMaxWidth().padding(start = 2.dp, top = 4.dp, bottom = 16.dp)) {
        Text(device.model.ifBlank { stringResource(R.string.generic_device) }, style = MaterialTheme.typography.titleMedium, color = colorResource(R.color.device_identity_text), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(device.serial, style = MaterialTheme.typography.labelMedium, color = colorResource(R.color.device_serial_text), letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DeviceIdentity(device: Device?) {
    device ?: return
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(device.model.ifBlank { stringResource(R.string.generic_device) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorResource(R.color.device_lock_identity))
        Text(
            device.serial,
            style = MaterialTheme.typography.labelMedium,
            color = colorResource(R.color.device_serial_text),
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
internal fun LockControls(
    state: LockState,
    onRefresh: () -> Unit,
    onRemote: (Boolean) -> Unit,
    onVolume: (Int) -> Unit,
    onRequestOpen: (Boolean) -> Unit
) {
    val snapshot = state.snapshot
    val busy = state.acting || state.loading
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.lock_panel_background)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LockPanelHeader(snapshot?.open)
            LockDivider()
            LockActionButtons(
                enabled = snapshot?.remoteEnabled == true && !busy,
                onRequestOpen = onRequestOpen,
                onRefresh = onRefresh,
                acting = busy,
                modifier = Modifier.fillMaxWidth()
            )
            if (snapshot?.remoteEnabled == false) {
                Text(
                    stringResource(R.string.remote_access_disabled_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.lock_warning_text)
                )
            }
            LockDivider()
            RemoteAccessCard(snapshot?.remoteEnabled, busy, onRemote)
            LockDivider()
            VolumeCard(snapshot?.volume, busy, onVolume)
        }
    }
}

@Composable
private fun LockPanelHeader(open: Boolean?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorResource(R.color.lock_title_text))
            Text(stringResource(R.string.lock_control_subtitle), style = MaterialTheme.typography.bodySmall, color = colorResource(R.color.lock_outline_text))
            LockStatusChip(open)
        }
        Box(
            Modifier.padding(start = 10.dp).size(width = 96.dp, height = 124.dp)
                .background(colorResource(R.color.device_lock_tile), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.device_lock),
                contentDescription = stringResource(R.string.lock_illustration_description),
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun LockStatusChip(open: Boolean?) {
    val statusColor = colorResource(
        if (open == null) R.color.lock_unknown_status_text else R.color.lock_status_text
    )
    Surface(
        modifier = Modifier.padding(top = 8.dp),
        shape = RoundedCornerShape(50),
        color = colorResource(
            when (open) {
                true -> R.color.lock_open_status_background
                false -> R.color.lock_closed_status_background
                null -> R.color.lock_unknown_status_background
            }
        )
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                when (open) {
                    true -> Icons.Filled.LockOpen
                    false -> Icons.Filled.Lock
                    null -> Icons.AutoMirrored.Filled.HelpOutline
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                open?.let { if (it) stringResource(R.string.lock_open_status) else stringResource(R.string.lock_closed_status) }
                    ?: stringResource(R.string.status_unavailable),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LockDivider() {
    androidx.compose.material3.HorizontalDivider(color = colorResource(R.color.lock_divider))
}

@Composable
private fun LockActionButtons(
    enabled: Boolean,
    onRequestOpen: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    acting: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OpenLockButton(enabled, onRequestOpen)
        CloseLockButton(enabled, onRequestOpen)
        RefreshLockButton(acting, onRefresh)
    }
}

@Composable
private fun OpenLockButton(enabled: Boolean, onRequestOpen: (Boolean) -> Unit) {
    Button(
        onClick = { onRequestOpen(true) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.lock_open_action),
            contentColor = Color.White,
            disabledContainerColor = colorResource(R.color.lock_disabled_action),
            disabledContentColor = colorResource(R.color.lock_disabled_action_text)
        )
    ) {
        LockActionButtonContent(open = true)
    }
}

@Composable
private fun CloseLockButton(enabled: Boolean, onRequestOpen: (Boolean) -> Unit) {
    OutlinedButton(
        onClick = { onRequestOpen(false) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.lock_outline_text)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorResource(R.color.lock_outline_border))
    ) {
        LockActionButtonContent(open = false)
    }
}

@Composable
private fun LockActionButtonContent(open: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (open) Icons.Filled.LockOpen else Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            stringResource(if (open) R.string.open_lock else R.string.close_lock),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RefreshLockButton(acting: Boolean, onRefresh: () -> Unit) {
    OutlinedButton(
        onClick = onRefresh,
        enabled = !acting,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.lock_outline_text)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorResource(R.color.lock_outline_border))
    ) {
        Text(stringResource(R.string.refresh_status), maxLines = 1)
    }
}

@Composable
private fun RemoteAccessCard(enabled: Boolean?, acting: Boolean, onRemote: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = colorResource(R.color.lock_accent), modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(R.string.remote_access), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorResource(R.color.lock_section_title))
            Text(stringResource(R.string.remote_access_description), style = MaterialTheme.typography.bodySmall, color = colorResource(R.color.lock_outline_text))
        }
        Switch(
            checked = enabled == true,
            onCheckedChange = onRemote,
            enabled = enabled != null && !acting,
            modifier = Modifier.testTag("remote_access_toggle")
        )
    }
}

@Composable
private fun VolumeCard(volume: Int?, acting: Boolean, onVolume: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = colorResource(R.color.lock_accent), modifier = Modifier.size(22.dp))
            Text(stringResource(R.string.volume), modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colorResource(R.color.lock_section_title))
        }
        Text(
            volume?.let { stringResource(R.string.volume_level, it) } ?: stringResource(R.string.volume_unavailable),
            modifier = Modifier.padding(top = 4.dp),
            color = colorResource(R.color.lock_outline_text)
        )
        VolumeControl(volume, acting, onVolume)
    }
}

@Composable
private fun VolumeControl(volume: Int?, acting: Boolean, onVolumeChanged: (Int) -> Unit) {
    var value by remember(volume) { mutableFloatStateOf((volume ?: 1).toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = { value = it },
            valueRange = 0f..3f,
            steps = 2,
            enabled = !acting,
            onValueChangeFinished = { onVolumeChanged(value.toInt()) },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
internal fun LockConfirmation(open: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (open) R.string.confirm_open_title else R.string.confirm_close_title)) },
        text = { Text(stringResource(if (open) R.string.confirm_open_message else R.string.confirm_close_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Preview(showBackground = true, name = "Controles da fechadura")
@Composable
private fun LockControlsPreview() {
    val device = Device("LOCK-001", "lock", "Porta de entrada", "MFR 2030", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
    val state = LockState(device = device, snapshot = LockSnapshot(
        open = false,
        remoteEnabled = true,
        volume = 2
    ), loading = false)
    MaterialTheme {
        LockControls(state, {}, {}, {}, {})
    }
}


