package com.example.iagointelbras.feature.devices

import com.example.iagointelbras.feature.devices.camera.viewmodel.CameraViewModel
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.connection.viewmodel.ConnectionViewModel
import com.example.iagointelbras.feature.devices.devices.viewmodel.DevicesViewModel
import com.example.iagointelbras.feature.devices.lock.viewmodel.LockViewModel

import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.model.CameraSession
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.DevicePage
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.model.LockSnapshot
import com.example.iagointelbras.domain.repository.CameraRepository
import com.example.iagointelbras.domain.repository.DeviceRepository
import com.example.iagointelbras.domain.repository.LockRepository
import com.example.iagointelbras.domain.repository.LockSettingsRepository
import com.example.iagointelbras.domain.repository.TokenStore
import com.example.iagointelbras.feature.devices.usecase.ClearAccessToken
import com.example.iagointelbras.feature.devices.usecase.GetCameraQuota
import com.example.iagointelbras.feature.devices.usecase.GetDevices
import com.example.iagointelbras.feature.devices.usecase.GetLockHistory
import com.example.iagointelbras.feature.devices.usecase.GetLockOpenStatus
import com.example.iagointelbras.feature.devices.usecase.GetLockSnapshot
import com.example.iagointelbras.feature.devices.usecase.GetSavedLockActions
import com.example.iagointelbras.feature.devices.usecase.GetSavedLockVolume
import com.example.iagointelbras.feature.devices.usecase.GetRemoteAccessStatus
import com.example.iagointelbras.feature.devices.usecase.IsConnected
import com.example.iagointelbras.feature.devices.usecase.SaveAccessToken
import com.example.iagointelbras.feature.devices.usecase.SaveLockAction
import com.example.iagointelbras.feature.devices.usecase.SaveLockVolumePreference
import com.example.iagointelbras.feature.devices.usecase.SetLockOpen
import com.example.iagointelbras.feature.devices.usecase.SetLockVolume
import com.example.iagointelbras.feature.devices.usecase.SetRemoteEnabled
import com.example.iagointelbras.feature.devices.usecase.StartCamera
import com.example.iagointelbras.feature.devices.usecase.StopCamera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun connectionViewModelStoresTrimmedTokenAndUpdatesState() {
        val fake = FakeCasaRepository()
        val viewModel = ConnectionViewModel(SaveAccessToken(fake), IsConnected(fake))

        viewModel.updateToken("  access-value  ")
        viewModel.connect()

        assertEquals("access-value", fake.token)
        assertTrue(viewModel.state.value.connected)
        assertEquals("", viewModel.state.value.token)
    }

    @Test
    fun connectionViewModelIgnoresRepeatedConnectWhileSavingToken() {
        val fake = FakeCasaRepository().apply { blockTokenSave = true }
        val viewModel = ConnectionViewModel(SaveAccessToken(fake), IsConnected(fake))
        viewModel.updateToken("access-value")

        viewModel.connect()
        viewModel.connect()

        assertEquals(1, fake.tokenSaveCalls)
        fake.tokenSaveGate.complete(Unit)
        assertTrue(viewModel.state.value.connected)
    }

    @Test
    fun connectionViewModelLoadsExistingConnectionAsynchronously() {
        val fake = FakeCasaRepository().apply { write("saved-token") }
        val viewModel = ConnectionViewModel(SaveAccessToken(fake), IsConnected(fake))

        assertTrue(viewModel.state.value.connected)
    }

    @Test
    fun devicesViewModelFiltersAndLoadsNextPage() {
        val fake = FakeCasaRepository().apply {
            pages[1] = DevicePage(listOf(sampleDevice), 1, 1, 2)
            pages[2] = DevicePage(listOf(sampleDevice.copy(serial = "SN-10")), 2, 1, 2)
        }
        val viewModel = DevicesViewModel(GetDevices(fake), ClearAccessToken(fake))

        viewModel.selectOrigin(DeviceOrigin.SHARED)
        viewModel.loadMore()

        assertEquals(DeviceOrigin.SHARED, fake.lastOrigin)
        assertEquals(listOf("SN-9", "SN-10"), viewModel.state.value.devices.map(Device::serial))
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun devicesViewModelDoesNotApplyResultsFromCancelledFilterRequest() {
        val linkedGate = CompletableDeferred<Unit>()
        val sharedDevice = sampleDevice.copy(serial = "SHARED-1", origin = DeviceOrigin.SHARED)
        val fake = FakeCasaRepository().apply {
            blockedOrigin = DeviceOrigin.LINKED
            devicePageGate = linkedGate
            originPages[DeviceOrigin.SHARED] = DevicePage(listOf(sharedDevice), 1, 20, 1)
        }
        val viewModel = DevicesViewModel(GetDevices(fake), ClearAccessToken(fake))

        viewModel.selectOrigin(DeviceOrigin.LINKED)
        viewModel.selectOrigin(DeviceOrigin.SHARED)
        linkedGate.complete(Unit)

        assertEquals(DeviceOrigin.SHARED, viewModel.state.value.origin)
        assertEquals(listOf(sharedDevice), viewModel.state.value.devices)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun devicesViewModelIgnoresFailureFromCancelledFilterRequest() {
        val linkedGate = CompletableDeferred<Unit>()
        val sharedDevice = sampleDevice.copy(serial = "SHARED-1", origin = DeviceOrigin.SHARED)
        val fake = FakeCasaRepository().apply {
            blockedOrigin = DeviceOrigin.LINKED
            staleFailureOrigin = DeviceOrigin.LINKED
            devicePageGate = linkedGate
            originPages[DeviceOrigin.SHARED] = DevicePage(listOf(sharedDevice), 1, 20, 1)
        }
        val viewModel = DevicesViewModel(GetDevices(fake), ClearAccessToken(fake))

        viewModel.selectOrigin(DeviceOrigin.LINKED)
        viewModel.selectOrigin(DeviceOrigin.SHARED)
        linkedGate.complete(Unit)

        assertEquals(DeviceOrigin.SHARED, viewModel.state.value.origin)
        assertEquals(listOf(sharedDevice), viewModel.state.value.devices)
        assertEquals(null, viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun lockViewModelRefreshesAndSendsSelectedAction() {
        val fake = FakeCasaRepository()
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(true, fake.lastOpen)
        assertEquals(true, viewModel.state.value.snapshot?.open)
    }

    @Test
    fun lockViewModelDoesNotStartDuplicateSnapshotRefreshes() {
        val fake = FakeCasaRepository().apply { blockSnapshot = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.refresh()

        assertEquals(1, fake.snapshotCalls)
        fake.snapshotGate.complete(Unit)
    }

    @Test
    fun lockViewModelAddsSuccessfulOpenActionToHistoryWhenApiHistoryIsEmpty() {
        val viewModel = lockViewModel(lockSavedState(), FakeCasaRepository())

        viewModel.setOpen(true)

        assertEquals(LockAction.OPENED, viewModel.state.value.history.first().action)
    }

    @Test
    fun lockViewModelAddsSuccessfulCloseActionToHistoryWhenApiHistoryIsEmpty() {
        val fake = FakeCasaRepository().apply { initialOpen = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(false)

        assertEquals(LockAction.CLOSED, viewModel.state.value.history.first().action)
    }

    @Test
    fun lockViewModelKeepsConfirmedActionWhenLocalHistoryCannotBeSaved() {
        val fake = FakeCasaRepository().apply { failActionSave = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(true, viewModel.state.value.snapshot?.open)
        assertEquals(LockAction.OPENED, viewModel.state.value.history.first().action)
        assertEquals(UiText.Resource(R.string.local_storage_error), viewModel.state.value.historyError)
        assertFalse(viewModel.state.value.acting)
    }

    @Test
    fun lockViewModelKeepsConfirmedVolumeWhenLocalPreferenceCannotBeSaved() {
        val fake = FakeCasaRepository().apply { failVolumeSave = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setVolume(3)

        assertEquals(3, viewModel.state.value.snapshot?.volume)
        assertEquals(UiText.Resource(R.string.local_storage_error), viewModel.state.value.error)
        assertFalse(viewModel.state.value.acting)
    }

    @Test
    fun lockViewModelFinishesVolumeActionBeforeBackgroundStatusConfirmation() {
        val fake = FakeCasaRepository()
        val viewModel = lockViewModel(lockSavedState(), fake)
        fake.blockSnapshot = true

        viewModel.setVolume(3)

        assertEquals(3, viewModel.state.value.snapshot?.volume)
        assertFalse(viewModel.state.value.acting)
        assertEquals(1, fake.snapshotCalls)
        assertEquals(1, fake.volumeStatusCalls)
        fake.snapshotGate.complete(Unit)
    }

    @Test
    fun lockViewModelRestoresLastActionAfterLeavingAndReturningToScreen() {
        val fake = FakeCasaRepository()
        lockViewModel(lockSavedState(), fake).setOpen(true)
        val returnedViewModel = lockViewModel(lockSavedState(), fake)

        assertEquals(LockAction.OPENED, returnedViewModel.state.value.history.first().action)
    }

    @Test
    fun lockViewModelKeepsMultipleActionsInsteadOfReplacingEarlierHistory() {
        val fake = FakeCasaRepository()
        val viewModel = lockViewModel(lockSavedState(), fake)
        viewModel.setOpen(true)
        viewModel.setOpen(false)
        val returnedViewModel = lockViewModel(lockSavedState(), fake)

        assertEquals(
            listOf(LockAction.CLOSED, LockAction.OPENED),
            returnedViewModel.state.value.history.take(2).map(LockEvent::action)
        )
    }

    @Test
    fun lockViewModelKeepsOlderRemoteActionWhenItMatchesTheNewActionType() {
        val olderEvent = LockEvent(
            Instant.now().minusSeconds(3_600).toString(),
            "",
            APPLICATION_HISTORY_METHOD,
            LockAction.OPENED
        )
        val fake = FakeCasaRepository().apply { remoteHistory = listOf(olderEvent) }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(2, viewModel.state.value.history.size)
        assertEquals(listOf(LockAction.OPENED, LockAction.OPENED), viewModel.state.value.history.map(LockEvent::action))
    }

    @Test
    fun lockViewModelDeduplicatesLocalActionReturnedByRemoteHistory() {
        val fake = FakeCasaRepository()
        val viewModel = lockViewModel(lockSavedState(), fake)
        viewModel.setOpen(true)
        fake.remoteHistory = listOf(fake.savedActions.single())

        viewModel.refreshHistory()

        assertEquals(1, viewModel.state.value.history.size)
    }

    @Test
    fun lockViewModelHidesUnrecognizedHistoryWhenAccountHasNoSavedActions() {
        val fake = FakeCasaRepository().apply {
            initialOpen = true
            remoteHistory = listOf(
                LockEvent("", "usuarioRemoto", "usuarioRemoto"),
                LockEvent("2026-10-06T12:00:00Z", "usuarioRemoto", "usuarioRemoto", LockAction.OPENED)
            )
        }

        val viewModel = lockViewModel(lockSavedState(), fake)

        assertTrue(viewModel.state.value.history.isEmpty())
    }

    @Test
    fun lockViewModelDisplaysEveryTimestampedRemoteHistoryEvent() {
        val fake = FakeCasaRepository().apply {
            remoteHistory = listOf(
                LockEvent("20261008T180244", "Ação remota", APPLICATION_HISTORY_METHOD),
                LockEvent("20261008T172338", "Ação remota", APPLICATION_HISTORY_METHOD),
                LockEvent("20261008T172259", "Ação remota", APPLICATION_HISTORY_METHOD)
            )
        }

        val viewModel = lockViewModel(lockSavedState(), fake)

        assertEquals(3, viewModel.state.value.history.size)
        assertEquals(
            listOf(LockAction.OPENED, LockAction.CLOSED, LockAction.OPENED),
            viewModel.state.value.history.map(LockEvent::action)
        )
    }

    @Test
    fun lockViewModelTracksHistoryLoadingUntilTheRequestCompletes() {
        val fake = FakeCasaRepository().apply { blockHistory = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        assertTrue(viewModel.state.value.historyLoading)
        fake.historyGate.complete(Unit)
        assertFalse(viewModel.state.value.historyLoading)
    }

    @Test
    fun lockViewModelShowsAcceptedOpenCommandBeforeBackgroundStatusConfirmation() {
        val fake = FakeCasaRepository().apply { staleSnapshotsAfterOpen = 1 }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(true, viewModel.state.value.snapshot?.open)
        assertEquals(1, fake.snapshotCalls)
        assertEquals(1, fake.openStatusCalls)
    }

    @Test
    fun lockViewModelUpdatesStatusImmediatelyWhileOpenCommandIsPending() {
        val fake = FakeCasaRepository().apply { blockOpenCommand = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(true, viewModel.state.value.snapshot?.open)
        assertTrue(viewModel.state.value.acting)
        fake.openCommandGate.complete(Unit)
        assertEquals(false, viewModel.state.value.acting)
    }

    @Test
    fun lockViewModelRestoresPreviousStatusWhenOpenCommandFails() {
        val fake = FakeCasaRepository().apply { openCommandFails = true }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(false, viewModel.state.value.snapshot?.open)
        assertFalse(viewModel.state.value.acting)
        assertEquals(UiText.Resource(R.string.operation_failed), viewModel.state.value.error)
    }

    @Test
    fun lockViewModelDoesNotSendOpenCommandWhenRemoteAccessIsDisabled() {
        val fake = FakeCasaRepository().apply { remoteEnabled = false }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(true)

        assertEquals(null, fake.lastOpen)
        assertEquals(false, viewModel.state.value.snapshot?.open)
        assertEquals(UiText.Resource(R.string.remote_access_required), viewModel.state.value.error)
    }

    @Test
    fun lockViewModelDoesNotRepeatCloseWhenAlreadyClosed() {
        val fake = FakeCasaRepository().apply { initialOpen = false }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setOpen(false)

        assertEquals(null, fake.lastOpen)
        assertEquals(UiText.Resource(R.string.lock_already_closed), viewModel.state.value.error)
    }

    @Test
    fun lockViewModelKeepsRemoteToggleValueWhileStatusEndpointIsStale() {
        val fake = FakeCasaRepository().apply { remoteEnabled = false }
        val viewModel = lockViewModel(lockSavedState(), fake)

        viewModel.setRemote(true)

        assertEquals(true, fake.remoteEnabled)
        assertEquals(true, viewModel.state.value.snapshot?.remoteEnabled)
        assertEquals(1, fake.remoteStatusCalls)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun lockViewModelRestoresVolumeAfterReenteringScreen() {
        val fake = FakeCasaRepository().apply { volumeUnavailable = true }
        val savedState = lockSavedState()
        val firstViewModel = lockViewModel(savedState, fake)

        firstViewModel.setVolume(3)
        val secondViewModel = lockViewModel(lockSavedState(), fake)

        assertEquals(3, fake.currentVolume)
        assertEquals(3, fake.savedVolumes["SN-9"])
        assertEquals(3, secondViewModel.state.value.snapshot?.volume)
        assertEquals(false, firstViewModel.state.value.acting)
        assertEquals(null, firstViewModel.state.value.error)
    }

    @Test
    fun lockViewModelFinishesActionWhileBackgroundSnapshotRefreshIsPending() {
        val fake = FakeCasaRepository()
        val viewModel = lockViewModel(lockSavedState(), fake)
        fake.blockSnapshot = true
        viewModel.setOpen(true)

        assertFalse(viewModel.state.value.acting)
        assertEquals(true, viewModel.state.value.snapshot?.open)
    }

    @Test
    fun lockViewModelSkipsLockRequestsForCamera() {
        val fake = FakeCasaRepository()
        val viewModel = lockViewModel(cameraSavedState(), fake)

        assertEquals(0, fake.snapshotCalls)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun cameraViewModelLimitsStreamToAvailableQuota() {
        val fake = FakeCasaRepository().apply { quota = 0.4 }
        val viewModel = CameraViewModel(cameraSavedState(), GetCameraQuota(fake), StartCamera(fake), StopCamera(fake), testApplicationScope())

        viewModel.start()

        assertEquals(0.4, fake.requestedStreamGb, 0.0)
        assertEquals("session-1", viewModel.state.value.session?.sessionId)
    }

    @Test
    fun cameraViewModelStopsSessionOnlyOnce() {
        val fake = FakeCasaRepository().apply { quota = 0.4 }
        val viewModel = CameraViewModel(cameraSavedState(), GetCameraQuota(fake), StartCamera(fake), StopCamera(fake), testApplicationScope())
        viewModel.start()
        viewModel.stop("session-1")
        viewModel.stop("session-1")

        assertTrue(fake.stopped.await(1, TimeUnit.SECONDS))
        assertEquals(1, fake.stopCalls)
    }

    @Test
    fun cameraViewModelStopsActiveSessionWhenItsNavigationEntryIsCleared() {
        val fake = FakeCasaRepository().apply { quota = 0.4 }
        val viewModel = CameraViewModel(cameraSavedState(), GetCameraQuota(fake), StartCamera(fake), StopCamera(fake), testApplicationScope())
        val store = ViewModelStore().apply { put("camera", viewModel) }

        viewModel.start()
        store.clear()

        assertTrue(fake.stopped.await(1, TimeUnit.SECONDS))
        assertEquals(1, fake.stopCalls)
    }

    @Test
    fun cameraViewModelBlocksDuplicateSessionRequests() {
        val fake = FakeCasaRepository().apply { quota = 0.4; blockCameraStart = true }
        val viewModel = CameraViewModel(cameraSavedState(), GetCameraQuota(fake), StartCamera(fake), StopCamera(fake), testApplicationScope())

        viewModel.start()
        viewModel.start()

        assertEquals(1, fake.startCalls)
    }

    @Test
    fun cameraViewModelIgnoresRepeatedQuotaRefreshWhileLoading() {
        val fake = FakeCasaRepository().apply { blockQuota = true }
        val viewModel = CameraViewModel(cameraSavedState(), GetCameraQuota(fake), StartCamera(fake), StopCamera(fake), testApplicationScope())

        viewModel.refreshQuota()

        assertEquals(1, fake.quotaCalls)
        fake.quotaGate.complete(Unit)
        assertFalse(viewModel.state.value.loading)
    }

    private fun testApplicationScope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private class FakeCasaRepository : DeviceRepository, LockRepository, LockSettingsRepository, CameraRepository, TokenStore {
        var token: String? = null
        var lastOrigin = DeviceOrigin.ALL
        var lastOpen: Boolean? = null
        var initialOpen = false
        val savedActions = mutableListOf<LockEvent>()
        var remoteHistory = emptyList<LockEvent>()
        var remoteEnabled = true
        var remoteBeforeUpdate = true
        var staleRemoteSnapshots = 0
        var currentVolume = 2
        var volumeUnavailable = false
        var failVolumeSave = false
        var failActionSave = false
        var quota = 1.0
        var requestedStreamGb = 0.0
        var stopCalls = 0
        var startCalls = 0
        var tokenSaveCalls = 0
        var quotaCalls = 0
        var blockSnapshot = false
        var blockHistory = false
        var blockOpenCommand = false
        var openCommandFails = false
        var snapshotCalls = 0
        var openStatusCalls = 0
        var remoteStatusCalls = 0
        var volumeStatusCalls = 0
        var staleSnapshotsAfterOpen = 0
        var blockCameraStart = false
        var blockTokenSave = false
        var blockQuota = false
        var blockedOrigin: DeviceOrigin? = null
        var staleFailureOrigin: DeviceOrigin? = null
        var devicePageGate: CompletableDeferred<Unit>? = null
        val tokenSaveGate = CompletableDeferred<Unit>()
        val quotaGate = CompletableDeferred<Unit>()
        val startGate = CompletableDeferred<Unit>()
        val snapshotGate = CompletableDeferred<Unit>()
        val historyGate = CompletableDeferred<Unit>()
        val openCommandGate = CompletableDeferred<Unit>()
        val stopped = CountDownLatch(1)
        val pages = mutableMapOf<Int, DevicePage>()
        val originPages = mutableMapOf<DeviceOrigin, DevicePage>()
        val savedVolumes = mutableMapOf<String, Int>()

        override fun read() = token
        override fun write(token: String) { this.token = token }
        override fun clear() { token = null }
        override suspend fun isConnected() = !token.isNullOrBlank()
        override suspend fun saveToken(token: String) = runCatching {
            tokenSaveCalls++
            if (blockTokenSave) tokenSaveGate.await()
            write(token)
        }
        override suspend fun clearToken() = runCatching { clear() }
        override suspend fun listDevices(page: Int, pageSize: Int, origin: DeviceOrigin): Result<DevicePage> {
            lastOrigin = origin
            if (origin == blockedOrigin) {
                if (origin == staleFailureOrigin) withContext(NonCancellable) { devicePageGate?.await() }
                else devicePageGate?.await()
            }
            if (origin == staleFailureOrigin) return Result.failure(IllegalStateException("stale response"))
            return Result.success(originPages[origin] ?: pages[page] ?: DevicePage(emptyList(), page, pageSize, page))
        }

        override suspend fun getLockSnapshot(device: Device): Result<LockSnapshot> {
            snapshotCalls++
            if (blockSnapshot) snapshotGate.await()
            if (lastOpen == true && staleSnapshotsAfterOpen > 0) {
                staleSnapshotsAfterOpen--
                return Result.success(LockSnapshot(false, true, 2))
            }
            val reportedRemote = if (staleRemoteSnapshots > 0) {
                staleRemoteSnapshots--
                remoteBeforeUpdate
            } else remoteEnabled
            return Result.success(LockSnapshot(lastOpen ?: initialOpen, reportedRemote, currentVolume.takeUnless { volumeUnavailable }))
        }
        override suspend fun getLockOpenStatus(device: Device): Result<Boolean?> {
            openStatusCalls++
            if (lastOpen == true && staleSnapshotsAfterOpen > 0) {
                staleSnapshotsAfterOpen--
                return Result.success(false)
            }
            return Result.success(lastOpen ?: initialOpen)
        }
        override suspend fun getRemoteAccessStatus(device: Device): Result<Boolean?> {
            remoteStatusCalls++
            if (staleRemoteSnapshots > 0) {
                staleRemoteSnapshots--
                return Result.success(remoteBeforeUpdate)
            }
            return Result.success(remoteEnabled)
        }
        override suspend fun readVolume(device: Device) = savedVolumes[device.serial]
        override suspend fun saveVolume(device: Device, volume: Int) {
            if (failVolumeSave) error("Preference storage failed")
            savedVolumes[device.serial] = volume
        }
        override suspend fun readActions(device: Device) = savedActions.toList()
        override suspend fun saveAction(device: Device, event: LockEvent) {
            if (failActionSave) error("History storage failed")
            savedActions.add(0, event)
        }
        override suspend fun setLockOpen(device: Device, open: Boolean) = runCatching {
            if (blockOpenCommand) openCommandGate.await()
            if (openCommandFails) error("Comando recusado")
            lastOpen = open
        }
        override suspend fun setLockVolume(device: Device, volume: Int) = runCatching { currentVolume = volume }
        override suspend fun setRemoteEnabled(device: Device, enabled: Boolean) = runCatching {
            remoteBeforeUpdate = remoteEnabled
            remoteEnabled = enabled
            staleRemoteSnapshots = 1
        }
        override suspend fun getLockHistory(device: Device, count: Int): Result<List<LockEvent>> {
            if (blockHistory) historyGate.await()
            return Result.success(remoteHistory)
        }
        override suspend fun startCamera(device: Device, channel: Int, stream: Int, streamGb: Double): Result<CameraSession> {
            startCalls++
            if (blockCameraStart) startGate.await()
            requestedStreamGb = streamGb
            return Result.success(CameraSession("session-1", "https://open-casainteligente.intelbras.com.br/player"))
        }

        override suspend fun stopCamera(sessionId: String): Result<Unit> {
            stopCalls++
            stopped.countDown()
            return Result.success(Unit)
        }
        override suspend fun availableQuota(): Result<Double> {
            quotaCalls++
            if (blockQuota) quotaGate.await()
            return Result.success(quota)
        }
    }

    private companion object {
        val sampleDevice = Device("SN-9", "lock-2", "Porta", "MFR 7000", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
        fun lockViewModel(state: SavedStateHandle, repository: FakeCasaRepository) = LockViewModel(
            state,
            GetLockSnapshot(repository),
            GetLockOpenStatus(repository),
            GetRemoteAccessStatus(repository),
            GetSavedLockVolume(repository),
            GetSavedLockActions(repository),
            SaveLockVolumePreference(repository),
            SaveLockAction(repository),
            SetLockOpen(repository),
            SetLockVolume(repository),
            SetRemoteEnabled(repository),
            GetLockHistory(repository)
        )
        fun lockSavedState() = SavedStateHandle(
            mapOf(
                "serial" to "SN-9",
                "productId" to "lock-2",
                "name" to "Porta",
                "model" to "MFR 7000",
                "category" to DeviceCategory.LOCK,
                "origin" to DeviceOrigin.LINKED,
                "online" to true,
                "apiNamespace" to null
            )
        )
        fun cameraSavedState() = SavedStateHandle(
            mapOf(
                "serial" to "CAM-1",
                "productId" to "cam-1",
                "name" to "Sala",
                "model" to "iM5",
                "category" to DeviceCategory.CAMERA,
                "origin" to DeviceOrigin.LINKED,
                "online" to true,
                "apiNamespace" to null
            )
        )
    }
}



