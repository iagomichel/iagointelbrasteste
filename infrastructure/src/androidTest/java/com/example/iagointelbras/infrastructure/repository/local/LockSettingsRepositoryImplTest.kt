package com.example.iagointelbras.infrastructure.repository.local

import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.repository.TokenStore

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockSettingsRepositoryImplTest {
    @Test
    fun persistsVolumeAndIsolatesSettingsByAccountToken() = runBlocking {
        val accountAToken = "account-a-${UUID.randomUUID()}"
        val tokenStore = FakeTokenStore(accountAToken)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = LockSettingsRepositoryImpl(context, tokenStore)
        val device = Device("serial-${UUID.randomUUID()}", "product", "Door", "Lock", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
        val event = LockEvent("2026-10-06T12:00:00Z", "", APPLICATION_HISTORY_METHOD, LockAction.OPENED)

        repository.saveVolume(device, 3)
        repository.saveAction(device, event)

        assertEquals(3, repository.readVolume(device))
        assertEquals(listOf(event), repository.readActions(device))

        tokenStore.write("account-b-${UUID.randomUUID()}")

        assertEquals(null, repository.readVolume(device))
        assertTrue(repository.readActions(device).isEmpty())

        repository.saveVolume(device, 1)
        tokenStore.write(accountAToken)

        assertEquals(3, repository.readVolume(device))
        assertEquals(listOf(event), repository.readActions(device))
    }

    private class FakeTokenStore(private var token: String) : TokenStore {
        override fun read() = token
        override fun write(token: String) { this.token = token }
        override fun clear() { token = "" }
    }
}



