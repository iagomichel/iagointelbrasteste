package com.example.iagointelbras.feature.devices.lock.viewmodel

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.common.model.messageOrDefault
import com.example.iagointelbras.feature.devices.navigation.LockDestination
import com.example.iagointelbras.feature.devices.navigation.toDevice
import com.example.iagointelbras.feature.devices.navigation.toDevice
import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.model.LockSnapshot
import com.example.iagointelbras.feature.devices.usecase.GetLockHistory
import com.example.iagointelbras.feature.devices.usecase.GetLockOpenStatus
import com.example.iagointelbras.feature.devices.usecase.GetLockSnapshot
import com.example.iagointelbras.feature.devices.usecase.GetSavedLockActions
import com.example.iagointelbras.feature.devices.usecase.GetSavedLockVolume
import com.example.iagointelbras.feature.devices.usecase.GetRemoteAccessStatus
import com.example.iagointelbras.feature.devices.usecase.SaveLockAction
import com.example.iagointelbras.feature.devices.usecase.SaveLockVolumePreference
import com.example.iagointelbras.feature.devices.usecase.SetLockOpen
import com.example.iagointelbras.feature.devices.usecase.SetLockVolume
import com.example.iagointelbras.feature.devices.usecase.SetRemoteEnabled

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class LockViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getLockSnapshot: GetLockSnapshot,
    private val getLockOpenStatus: GetLockOpenStatus,
    private val getRemoteAccessStatus: GetRemoteAccessStatus,
    private val getSavedLockVolume: GetSavedLockVolume,
    private val getSavedLockActions: GetSavedLockActions,
    private val saveLockVolumePreference: SaveLockVolumePreference,
    private val saveLockAction: SaveLockAction,
    private val setLockOpen: SetLockOpen,
    private val setLockVolume: SetLockVolume,
    private val setRemoteEnabled: SetRemoteEnabled,
    private val getLockHistory: GetLockHistory
) : ViewModel() {
    private val device = savedStateHandle.toRoute<LockDestination>().toDevice()
    private val _state = MutableStateFlow(LockState(device = device))
    val state = _state.asStateFlow()
    private var historyJob: Job? = null
    private var snapshotJob: Job? = null

    init {
        if (device.category == DeviceCategory.LOCK) {
            refresh()
            loadHistory()
        } else {
            _state.update { it.copy(loading = false) }
        }
    }

    fun refresh() = refreshSnapshotInBackground()

    fun setOpen(open: Boolean) {
        val current = _state.value
        if (current.snapshot?.remoteEnabled != true) {
            _state.update { it.copy(error = UiText.Resource(R.string.remote_access_required)) }
            return
        }
        if (current.acting || current.loading) return
        snapshotJob?.cancel()
        when (current.snapshot.open) {
            open -> {
                finishOpenActionError(UiText.Resource(if (open) R.string.lock_already_open else R.string.lock_already_closed))
                return
            }
            null -> {
                finishOpenActionError(UiText.Resource(R.string.lock_status_verification_failed))
                return
            }
            else -> Unit
        }
        val previousOpen = current.snapshot.open
        _state.update { state ->
            state.copy(
                snapshot = state.snapshot?.copy(open = open),
                acting = true,
                loading = false,
                error = null
            )
        }
        viewModelScope.launch {
            sendOpenCommand(device, open, previousOpen)
        }
    }

    fun setVolume(volume: Int) {
        perform(expectedVolume = volume) { setLockVolume(device, volume) }
    }

    fun setRemote(enabled: Boolean) {
        perform(expectedRemote = enabled) { setRemoteEnabled(device, enabled) }
    }

    fun refreshHistory() {
        loadHistory()
    }

    private suspend fun sendOpenCommand(target: Device, open: Boolean, previousOpen: Boolean?) {
        setLockOpen(target, open).fold(
            onSuccess = { completeOpenCommand(target, open) },
            onFailure = { rollbackOpenCommand(previousOpen, it) }
        )
    }

    private suspend fun completeOpenCommand(target: Device, open: Boolean) {
        val event = LockEvent(
            Instant.now().toString(),
            "",
            APPLICATION_HISTORY_METHOD,
            if (open) LockAction.OPENED else LockAction.CLOSED
        )
        val storageError = saveOpenAction(target, event)
        _state.update { state ->
            state.copy(
                history = listOf(event) + state.history,
                historyError = storageError,
                loading = false
            )
        }
        loadHistory(storageError).join()
        refreshOpenStatusInBackground(open).join()
        _state.update { it.copy(acting = false) }
    }

    private suspend fun saveOpenAction(target: Device, event: LockEvent): UiText? = try {
        saveLockAction(target, event)
        null
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        UiText.Resource(R.string.local_storage_error)
    }

    private fun rollbackOpenCommand(previousOpen: Boolean?, failure: Throwable) = _state.update { state ->
        state.copy(
            snapshot = state.snapshot?.copy(open = previousOpen),
            acting = false,
            error = failure.messageOrDefault()
        )
    }

    private fun finishOpenActionError(message: UiText) = _state.update {
        it.copy(acting = false, error = message)
    }

    private fun loadHistory(initialStorageError: UiText? = null): Job {
        historyJob?.cancel()
        return viewModelScope.launch {
            _state.update { it.copy(historyLoading = true, historyError = initialStorageError) }
            var storageError = initialStorageError
            val savedActions = try {
                getSavedLockActions(device)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                storageError = UiText.Resource(R.string.local_storage_error)
                emptyList()
            }
            val confirmedActions = savedActions.filter { it.isTokenLockAction() }
            if (confirmedActions.isNotEmpty()) _state.update { it.copy(history = confirmedActions) }
            getLockHistory(device).fold(
                onSuccess = { events ->
                    snapshotJob?.join()
                    val updatedEvents = events.withSavedActions(confirmedActions, _state.value.snapshot?.open)
                    _state.update { state ->
                        val visibleEvents = if (storageError == null) updatedEvents else {
                            (state.history + updatedEvents).distinctBy { Triple(it.timestamp, it.method, it.action) }
                        }
                        state.copy(history = visibleEvents, historyLoading = false, historyError = storageError)
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(historyLoading = false, historyError = storageError ?: failure.messageOrDefault())
                    }
                }
            )
        }.also { historyJob = it }
    }

    private fun List<LockEvent>.withSavedActions(
        savedActions: List<LockEvent>,
        currentOpen: Boolean?
    ): List<LockEvent> {
        val remoteEvents = filter { it.isDisplayableHistoryEvent() }
        var previousOpen = currentOpen
        val mergedEvents = remoteEvents.map { remote ->
            val saved = savedActions.firstOrNull { it.timestamp.isCloseTo(remote.timestamp) }
            val action = saved?.action ?: remote.action ?: previousOpen?.toHistoryAction()
            previousOpen = action?.previousOpenState() ?: previousOpen
            remote.copy(
                description = saved?.description.orEmpty(),
                method = saved?.method ?: remote.method,
                action = action
            )
        }
        val localOnlyEvents = savedActions.filterNot { saved ->
            remoteEvents.any { remote -> remote.isDuplicateOf(saved) }
        }
        return localOnlyEvents + mergedEvents
    }

    private fun LockEvent.isDisplayableHistoryEvent() = timestamp.isNotBlank() &&
        (method.isNotBlank() || description.isNotBlank() || action != null)

    private fun LockEvent.isTokenLockAction() = method == APPLICATION_HISTORY_METHOD &&
        (action == LockAction.OPENED || action == LockAction.CLOSED)

    private fun LockEvent.isDuplicateOf(saved: LockEvent) = timestamp.isCloseTo(saved.timestamp)

    private fun Boolean.toHistoryAction() = if (this) LockAction.OPENED else LockAction.CLOSED

    private fun LockAction.previousOpenState() = when (this) {
        LockAction.OPENED -> false
        LockAction.CLOSED -> true
    }

    private fun String.isCloseTo(timestamp: String) = runCatching {
        val differenceSeconds = parseHistoryTimestamp(this).epochSecond - parseHistoryTimestamp(timestamp).epochSecond
        differenceSeconds in -HISTORY_MATCH_WINDOW_SECONDS..HISTORY_MATCH_WINDOW_SECONDS
    }.getOrElse { this == timestamp }

    private fun parseHistoryTimestamp(timestamp: String) = runCatching {
        Instant.parse(timestamp)
    }.getOrElse {
        LocalDateTime.parse(timestamp, API_HISTORY_TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC)
    }

    private fun perform(
        expectedVolume: Int? = null,
        expectedRemote: Boolean? = null,
        action: suspend () -> Result<Unit>
    ) {
        if (isActionInProgress()) return
        snapshotJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(acting = true, error = null) }
            action().fold(
                onSuccess = { completeAction(expectedVolume, expectedRemote) },
                onFailure = ::finishActionError
            )
        }
    }

    private fun isActionInProgress() = _state.value.acting || _state.value.loading

    private suspend fun completeAction(volume: Int?, remote: Boolean?) {
        val storageError = persistVolume(volume)
        applyRemoteValue(remote)
        _state.update { it.copy(acting = false, error = storageError ?: it.error) }
        val confirmationJob = when {
            remote != null -> refreshRemoteStatusInBackground(remote)
            else -> null
        }
        storageError?.let { error ->
            confirmationJob?.invokeOnCompletion {
                _state.update { it.copy(error = error) }
            }
        }
    }

    private suspend fun persistVolume(volume: Int?): UiText? {
        if (volume == null) return null
        savedStateHandle["volume"] = volume
        return try {
            saveLockVolumePreference(device, volume)
            _state.update { state ->
                val snapshot = state.snapshot ?: LockSnapshot(null, null, null)
                state.copy(snapshot = snapshot.copy(volume = volume))
            }
            null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            UiText.Resource(R.string.local_storage_error)
        }
    }

    private fun applyRemoteValue(remote: Boolean?) {
        if (remote == null) return
        _state.update { state ->
            val snapshot = state.snapshot ?: LockSnapshot(null, null, null)
            state.copy(snapshot = snapshot.copy(remoteEnabled = remote))
        }
    }

    private fun finishActionError(failure: Throwable) = _state.update {
        it.copy(acting = false, error = failure.messageOrDefault())
    }

    private fun refreshSnapshotInBackground(
        expectedOpen: Boolean? = null,
        expectedVolume: Int? = null,
        expectedRemote: Boolean? = null,
        showLoading: Boolean = true
    ): Job {
        snapshotJob?.takeIf { it.isActive }?.let { return it }
        return viewModelScope.launch {
            refreshSnapshot(
                expectedOpen = expectedOpen,
                expectedVolume = expectedVolume,
                expectedRemote = expectedRemote,
                showLoading = showLoading
            )
        }.also { snapshotJob = it }
    }

    private fun refreshOpenStatusInBackground(expected: Boolean) = refreshFieldInBackground(
        expected = expected,
        request = { getLockOpenStatus(device) },
        applyValue = { snapshot, value -> snapshot.copy(open = value ?: snapshot.open) },
        error = UiText.Resource(R.string.command_status_not_confirmed)
    )

    private fun refreshRemoteStatusInBackground(expected: Boolean) = refreshFieldInBackground(
        expected = expected,
        request = { getRemoteAccessStatus(device) },
        applyValue = { snapshot, value -> snapshot.copy(remoteEnabled = value ?: snapshot.remoteEnabled) },
        error = null,
        attempts = 1
    )

    private fun <T> refreshFieldInBackground(
        expected: T,
        request: suspend () -> Result<T?>,
        applyValue: (LockSnapshot, T?) -> LockSnapshot,
        error: UiText?,
        attempts: Int = 4
    ): Job = viewModelScope.launch {
        repeat(attempts) { attempt ->
            if (attempt > 0) delay(500.milliseconds)
            val result = request()
            val value = result.getOrNull()
            if (result.isSuccess && value == expected) {
                updateFieldSnapshot(value, applyValue, null)
                return@launch
            }
            if (attempt == attempts - 1) {
                val failure = if (error == null) null else result.exceptionOrNull()?.messageOrDefault() ?: error
                updateFieldSnapshot(expected, applyValue, failure)
            }
        }
    }.also { snapshotJob = it }

    private fun <T> updateFieldSnapshot(
        value: T?,
        applyValue: (LockSnapshot, T?) -> LockSnapshot,
        error: UiText?
    ) = _state.update { state ->
        val snapshot = state.snapshot ?: LockSnapshot(null, null, null)
        state.copy(snapshot = applyValue(snapshot, value), loading = false, error = error)
    }

    private suspend fun refreshSnapshot(
        expectedOpen: Boolean? = null,
        expectedVolume: Int? = null,
        expectedRemote: Boolean? = null,
        showLoading: Boolean = true
    ) {
        if (showLoading) _state.update { it.copy(loading = true) }
        restoreSavedVolume()
        repeat(4) { attempt ->
            if (attempt > 0) delay(500.milliseconds)
            val result = getLockSnapshot(device)
            val snapshot = result.getOrNull()
            if (snapshot == null) {
                if (attempt == 3) applySnapshotFailure(device, result.exceptionOrNull())
                return@repeat
            }
            if (applySnapshot(snapshot, device, expectedOpen, expectedVolume, expectedRemote, attempt == 3)) return
        }
    }

    private suspend fun restoreSavedVolume() {
        if (_state.value.snapshot?.volume != null) return
        val (volume, storageError) = readSavedVolume(device)
        _state.update { state ->
            val snapshot = state.snapshot ?: LockSnapshot(null, null, null)
            state.copy(
                snapshot = snapshot.copy(volume = volume),
                error = storageError ?: state.error
            )
        }
    }

    private suspend fun applySnapshot(
        snapshot: LockSnapshot,
        target: Device,
        expectedOpen: Boolean?,
        expectedVolume: Int?,
        expectedRemote: Boolean?,
        finalAttempt: Boolean
    ): Boolean {
        val (savedVolume, storageError) = readSavedVolume(target)
        val volume = snapshot.volume ?: _state.value.snapshot?.volume ?: savedVolume
        val volumeConfirmed = expectedVolume == null || snapshot.volume == expectedVolume ||
            (snapshot.volume == null && savedVolume == expectedVolume)
        val remoteConfirmed = expectedRemote == null || snapshot.remoteEnabled == expectedRemote
        val confirmed = (expectedOpen == null || snapshot.open == expectedOpen) && volumeConfirmed && remoteConfirmed
        if (!confirmed && !finalAttempt) return false
        _state.update { state ->
            state.copy(
                snapshot = snapshot.withFallback(state.snapshot, savedVolume).copy(
                    volume = if (expectedVolume != null && snapshot.volume != expectedVolume) expectedVolume else volume,
                    remoteEnabled = if (expectedRemote != null && !remoteConfirmed) expectedRemote
                        else snapshot.remoteEnabled ?: state.snapshot?.remoteEnabled
                ),
                loading = false,
                error = snapshotConfirmationError(confirmed, expectedRemote) ?: storageError
            )
        }
        return true
    }

    private fun snapshotConfirmationError(confirmed: Boolean, expectedRemote: Boolean?) = when {
        confirmed -> null
        expectedRemote != null -> UiText.Resource(R.string.remote_status_not_confirmed)
        else -> UiText.Resource(R.string.command_status_not_confirmed)
    }

    private suspend fun applySnapshotFailure(target: Device, failure: Throwable?) {
        val (savedVolume, storageError) = readSavedVolume(target)
        _state.update { state ->
            val snapshot = state.snapshot ?: savedVolume?.let { LockSnapshot(null, null, it) }
            state.copy(
                loading = false,
                error = failure?.messageOrDefault() ?: storageError,
                snapshot = snapshot?.copy(volume = state.snapshot?.volume ?: savedVolume)
            )
        }
    }

    private suspend fun readSavedVolume(target: Device): Pair<Int?, UiText?> {
        savedStateHandle.get<Int>("volume")?.let { return it to null }
        return try {
            getSavedLockVolume(target) to null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null to UiText.Resource(R.string.local_storage_error)
        }
    }

    private fun LockSnapshot.withFallback(previous: LockSnapshot?, savedVolume: Int?) = copy(
        open = open ?: previous?.open,
        remoteEnabled = remoteEnabled ?: previous?.remoteEnabled,
        volume = volume ?: previous?.volume ?: savedVolume
    )
}

private const val HISTORY_MATCH_WINDOW_SECONDS = 10L
private val API_HISTORY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")




