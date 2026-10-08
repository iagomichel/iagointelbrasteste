package com.example.iagointelbras.feature.devices.common.ui

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun LoadingState(message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text(message, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(message, modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ErrorPanel(message: UiText, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
        ErrorMessage(message)
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
internal fun ErrorMessage(message: UiText) {
    Text(message.resolve(), modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun UiText.resolve() = when (this) {
    is UiText.Resource -> stringResource(id)
}

internal fun DeviceOrigin.labelResource() = when (this) {
    DeviceOrigin.ALL -> R.string.origin_all
    DeviceOrigin.LINKED -> R.string.origin_linked
    DeviceOrigin.SHARED -> R.string.origin_shared
    DeviceOrigin.UNKNOWN -> R.string.origin_unknown
}

@Composable
internal fun DeviceCategory.label() = stringResource(
    when (this) {
        DeviceCategory.CAMERA -> R.string.category_camera
        DeviceCategory.LOCK -> R.string.category_lock
        DeviceCategory.LIGHT -> R.string.category_light
        DeviceCategory.SENSOR -> R.string.category_sensor
        DeviceCategory.GENERIC -> R.string.category_generic
    }
)

@Preview(showBackground = true, name = "Estados compartilhados")
@Composable
private fun DeviceUiStatesPreview() {
    MaterialTheme {
        Column {
            LoadingState(stringResource(R.string.loading_devices))
            EmptyState(stringResource(R.string.no_devices_title), stringResource(R.string.no_devices_message))
        }
    }
}
