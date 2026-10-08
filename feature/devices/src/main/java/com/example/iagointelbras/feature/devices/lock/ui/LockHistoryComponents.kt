package com.example.iagointelbras.feature.devices.lock.ui

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.common.ui.ErrorMessage
import com.example.iagointelbras.feature.devices.common.ui.LoadingState
import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.domain.model.LockEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
@Composable
internal fun LockHistory(history: List<LockEvent>, loading: Boolean, error: UiText?, onRetry: () -> Unit) {
    if (history.isEmpty() && !loading && error == null) return
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorResource(R.color.device_lock_tile)
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = colorResource(R.color.lock_accent),
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Text(
                stringResource(R.string.lock_history),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.lock_history_title)
            )
        }
        if (loading && history.isNotEmpty()) HistoryRefreshIndicator()
        LockHistoryStatus(history.isEmpty(), loading, error, onRetry)
        LockHistoryEvents(history)
    }
}

@Composable
private fun HistoryRefreshIndicator() {
    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
}

@Composable
private fun LockHistoryStatus(empty: Boolean, loading: Boolean, error: UiText?, onRetry: () -> Unit) {
    when {
        error != null -> {
            ErrorMessage(error)
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        loading && empty -> LoadingState(stringResource(R.string.loading_lock_history))
    }
}

@Composable
private fun LockHistoryEvents(history: List<LockEvent>) {
    history.forEach { event -> LockHistoryEventCard(event) }
}

@Composable
private fun LockHistoryEventCard(event: LockEvent) {
    Card(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(event.displayDescription(), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(event.timestamp.toHistoryTime(), event.displayMethod()).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LockEvent.displayDescription() = when (action) {
    LockAction.OPENED -> stringResource(R.string.history_action_opened)
    LockAction.CLOSED -> stringResource(R.string.history_action_closed)
    null -> description
}

@Composable
private fun LockEvent.displayMethod() = if (method == APPLICATION_HISTORY_METHOD) {
    stringResource(R.string.history_method_app)
} else {
    method
}

private fun String.toHistoryTime() = runCatching {
    runCatching { Instant.parse(this).atZone(ZoneId.systemDefault()) }
        .getOrElse { LocalDateTime.parse(this, API_HISTORY_TIMESTAMP_FORMAT).atZone(ZoneId.systemDefault()) }
        .format(HISTORY_DISPLAY_TIMESTAMP_FORMAT)
}.getOrElse { this }

private val API_HISTORY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
private val HISTORY_DISPLAY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")

@Preview(showBackground = true, name = "Histórico de abertura")
@Composable
private fun LockHistoryPreview() {
    val events = listOf(
        LockEvent("20261008T180244", "Ação remota", APPLICATION_HISTORY_METHOD, LockAction.OPENED),
        LockEvent("20261008T172338", "Ação remota", APPLICATION_HISTORY_METHOD, LockAction.CLOSED)
    )
    MaterialTheme {
        LockHistory(events, loading = false, error = null, onRetry = {})
    }
}
