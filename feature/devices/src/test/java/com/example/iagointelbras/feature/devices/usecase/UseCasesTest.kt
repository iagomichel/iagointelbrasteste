package com.example.iagointelbras.feature.devices.usecase

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


import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {
    private val repository = FakeRepositories()
    private val device = Device("serial", "product", "name", "model", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
    private val event = LockEvent("time", "", APPLICATION_HISTORY_METHOD, LockAction.OPENED)

    @Test
    fun saveAccessTokenTrimsBeforePersisting() = runTest {
        SaveAccessToken(repository)(" token ")

        assertEquals("token", repository.savedToken)
    }

    @Test
    fun isConnectedReturnsRepositoryState() = runTest {
        repository.connected = true

        assertTrue(IsConnected(repository)())
    }

    @Test
    fun clearAccessTokenDelegates() = runTest {
        ClearAccessToken(repository)()

        assertTrue(repository.tokenCleared)
    }

    @Test
    fun getDevicesPassesPageSizeAndOrigin() = runTest {
        val result = GetDevices(repository)(2, 30, DeviceOrigin.SHARED)

        assertEquals(2, repository.requestedPage)
        assertEquals(30, repository.requestedPageSize)
        assertEquals(DeviceOrigin.SHARED, repository.requestedOrigin)
        assertEquals(repository.devicePage, result.getOrThrow())
    }

    @Test
    fun getLockSnapshotPassesDevice() = runTest {
        val result = GetLockSnapshot(repository)(device)

        assertEquals(device, repository.requestedDevice)
        assertEquals(repository.snapshot, result.getOrThrow())
    }

    @Test
    fun getSavedLockVolumeReturnsStoredValue() = runTest {
        repository.volume = 2

        assertEquals(2, GetSavedLockVolume(repository)(device))
    }

    @Test
    fun saveLockVolumePreferencePersistsValue() = runTest {
        SaveLockVolumePreference(repository)(device, 3)

        assertEquals(3, repository.volume)
    }

    @Test
    fun getSavedLockActionsReturnsStoredHistory() = runTest {
        repository.actions = listOf(event)

        assertEquals(listOf(event), GetSavedLockActions(repository)(device))
    }

    @Test
    fun saveLockActionPersistsEvent() = runTest {
        SaveLockAction(repository)(device, event)

        assertEquals(event, repository.savedAction)
    }

    @Test
    fun setLockOpenPassesDesiredState() = runTest {
        SetLockOpen(repository)(device, true)

        assertEquals(true, repository.requestedOpen)
    }

    @Test
    fun setLockVolumePassesValue() = runTest {
        SetLockVolume(repository)(device, 2)

        assertEquals(2, repository.requestedVolume)
    }

    @Test
    fun setRemoteEnabledPassesDesiredState() = runTest {
        SetRemoteEnabled(repository)(device, false)

        assertEquals(false, repository.requestedRemote)
    }

    @Test
    fun getLockHistoryPassesCount() = runTest {
        val result = GetLockHistory(repository)(device, 25)

        assertEquals(25, repository.requestedHistoryCount)
        assertEquals(listOf(event), result.getOrThrow())
    }

    @Test
    fun startCameraPassesStreamParameters() = runTest {
        val session = StartCamera(repository)(device, 1, 2, 0.5).getOrThrow()

        assertEquals(1, repository.requestedChannel)
        assertEquals(2, repository.requestedStream)
        assertEquals(0.5, repository.requestedQuota, 0.0)
        assertEquals(repository.cameraSession, session)
    }

    @Test
    fun stopCameraPassesSessionId() = runTest {
        StopCamera(repository)("session")

        assertEquals("session", repository.stoppedSession)
    }

    @Test
    fun getCameraQuotaReturnsRepositoryValue() = runTest {
        repository.quota = 0.75

        assertEquals(0.75, GetCameraQuota(repository)().getOrThrow(), 0.0)
    }

    private class FakeRepositories : DeviceRepository, LockRepository, LockSettingsRepository, CameraRepository {
        var connected = false
        var savedToken = ""
        var tokenCleared = false
        var requestedPage = 0
        var requestedPageSize = 0
        var requestedOrigin = DeviceOrigin.ALL
        var requestedDevice: Device? = null
        var requestedOpen: Boolean? = null
        var requestedVolume: Int? = null
        var requestedRemote: Boolean? = null
        var requestedHistoryCount = 0
        var requestedChannel = 0
        var requestedStream = 0
        var requestedQuota = 0.0
        var stoppedSession: String? = null
        var savedAction: LockEvent? = null
        var volume: Int? = null
        var actions = emptyList<LockEvent>()
        var quota = 1.0
        val snapshot = LockSnapshot(false, true, 2)
        val devicePage = DevicePage(emptyList(), 2, 30, 3)
        val cameraSession = CameraSession("session", "https://stream.example")

        override suspend fun isConnected() = connected
        override suspend fun listDevices(page: Int, pageSize: Int, origin: DeviceOrigin): Result<DevicePage> {
            requestedPage = page
            requestedPageSize = pageSize
            requestedOrigin = origin
            return Result.success(devicePage)
        }
        override suspend fun saveToken(token: String): Result<Unit> {
            savedToken = token
            return Result.success(Unit)
        }
        override suspend fun clearToken(): Result<Unit> {
            tokenCleared = true
            return Result.success(Unit)
        }
        override suspend fun getLockSnapshot(device: Device): Result<LockSnapshot> {
            requestedDevice = device
            return Result.success(snapshot)
        }
        override suspend fun getLockOpenStatus(device: Device): Result<Boolean?> {
            requestedDevice = device
            return Result.success(snapshot.open)
        }
        override suspend fun getRemoteAccessStatus(device: Device): Result<Boolean?> {
            requestedDevice = device
            return Result.success(snapshot.remoteEnabled)
        }
        override suspend fun setLockOpen(device: Device, open: Boolean): Result<Unit> {
            requestedOpen = open
            return Result.success(Unit)
        }
        override suspend fun setLockVolume(device: Device, volume: Int): Result<Unit> {
            requestedVolume = volume
            return Result.success(Unit)
        }
        override suspend fun setRemoteEnabled(device: Device, enabled: Boolean): Result<Unit> {
            requestedRemote = enabled
            return Result.success(Unit)
        }
        override suspend fun getLockHistory(device: Device, count: Int): Result<List<LockEvent>> {
            requestedHistoryCount = count
            return Result.success(listOf(LockEvent("time", "", APPLICATION_HISTORY_METHOD, LockAction.OPENED)))
        }
        override suspend fun readVolume(device: Device) = volume
        override suspend fun saveVolume(device: Device, volume: Int) { this.volume = volume }
        override suspend fun readActions(device: Device) = actions
        override suspend fun saveAction(device: Device, event: LockEvent) { savedAction = event }
        override suspend fun startCamera(device: Device, channel: Int, stream: Int, streamGb: Double): Result<CameraSession> {
            requestedChannel = channel
            requestedStream = stream
            requestedQuota = streamGb
            return Result.success(cameraSession)
        }
        override suspend fun stopCamera(sessionId: String): Result<Unit> {
            stoppedSession = sessionId
            return Result.success(Unit)
        }
        override suspend fun availableQuota(): Result<Double> = Result.success(quota)
    }
}

