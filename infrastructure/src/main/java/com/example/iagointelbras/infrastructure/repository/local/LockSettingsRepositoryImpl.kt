package com.example.iagointelbras.infrastructure.repository.local

import androidx.core.content.edit
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.repository.LockSettingsRepository
import com.example.iagointelbras.domain.repository.TokenStore
import com.example.iagointelbras.infrastructure.mapper.toLockAction
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenStore: TokenStore
) : LockSettingsRepository {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val actionsMutex = Mutex()

    override suspend fun readVolume(device: Device): Int? = withContext(Dispatchers.IO) {
        preferences.getInt(device.volumeKey(), NO_VOLUME).takeIf { it in 0..3 }
    }

    override suspend fun saveVolume(device: Device, volume: Int) = withContext(Dispatchers.IO) {
        preferences.edit(commit = true) { putInt(device.volumeKey(), volume) }
    }

    override suspend fun readActions(device: Device): List<LockEvent> =
        withContext(Dispatchers.IO) {
            try {
                JSONArray(preferences.getString(device.actionKey(), "[]")).toLockEvents()
            } catch (_: org.json.JSONException) {
                emptyList()
            }
        }

    override suspend fun saveAction(device: Device, event: LockEvent) =
        withContext(Dispatchers.IO) {
            actionsMutex.withLock {
                val events = JSONArray().apply {
                    put(event.toJson())
                    readActions(device).take(MAX_SAVED_ACTIONS - 1).forEach { put(it.toJson()) }
                }
                preferences.edit(commit = true) { putString(device.actionKey(), events.toString()) }
            }
        }

    private fun LockEvent.toJson() = JSONObject()
        .put("timestamp", timestamp)
        .put("description", description)
        .put("method", method)
        .put("action", action?.name.orEmpty())

    private fun Device.volumeKey() = "${accountKey()}_${deviceKey()}_volume"
    private fun Device.actionKey() = "${accountKey()}_${deviceKey()}_actions"
    private fun Device.deviceKey() = "${apiNamespace ?: serial}_$productId"
    private fun accountKey() = tokenStore.read().orEmpty().toByteArray().let { token ->
        MessageDigest.getInstance("SHA-256").digest(token).joinToString("") { "%02x".format(it) }
    }

    private fun JSONArray.toLockEvents() = (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { event ->
            val description = event.optString("description")
            val action = event.optString("action").toLockAction() ?: description.toLockAction()
            val method = event.optString("method")
            LockEvent(event.optString("timestamp"), description, method, action)
        }
    }

    private companion object {
        const val PREFERENCES = "lock_settings"
        const val NO_VOLUME = -1
        const val MAX_SAVED_ACTIONS = 50
    }
}
