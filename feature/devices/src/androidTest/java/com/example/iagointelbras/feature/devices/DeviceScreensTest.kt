package com.example.iagointelbras.feature.devices

import com.example.iagointelbras.feature.devices.camera.ui.CameraContent
import com.example.iagointelbras.feature.devices.camera.ui.CameraPlayerStatus
import com.example.iagointelbras.feature.devices.camera.ui.isTrustedVideoTarget
import com.example.iagointelbras.feature.devices.camera.viewmodel.CameraState
import com.example.iagointelbras.feature.devices.common.model.UiText
import com.example.iagointelbras.feature.devices.connection.ui.ConnectionContent
import com.example.iagointelbras.feature.devices.connection.viewmodel.ConnectionState
import com.example.iagointelbras.feature.devices.devices.ui.DeviceListContent
import com.example.iagointelbras.feature.devices.devices.viewmodel.DevicesState
import com.example.iagointelbras.feature.devices.lock.ui.LockContent
import com.example.iagointelbras.feature.devices.lock.viewmodel.LockState
import com.example.iagointelbras.feature.devices.navigation.LockDestination
import com.example.iagointelbras.feature.devices.navigation.toDevice

import com.example.iagointelbras.domain.model.APPLICATION_HISTORY_METHOD
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import com.example.iagointelbras.domain.model.LockAction
import com.example.iagointelbras.domain.model.LockEvent
import com.example.iagointelbras.domain.model.LockSnapshot

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class DeviceScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typedLockDestinationRestoresDeviceFromSavedArguments() {
        val destination = SavedStateHandle(
            mapOf(
                "serial" to "serial-1",
                "productId" to "product-1",
                "name" to "Entrada",
                "model" to "MFR 2030",
                "category" to DeviceCategory.LOCK,
                "origin" to DeviceOrigin.SHARED,
                "online" to true,
                "apiNamespace" to "namespace-1"
            )
        ).toRoute<LockDestination>()

        assertEquals(
            Device("serial-1", "product-1", "Entrada", "MFR 2030", DeviceCategory.LOCK, DeviceOrigin.SHARED, true, "namespace-1"),
            destination.toDevice()
        )
    }

    @Test
    fun connectionScreenAcceptsTokenAndSubmits() {
        val connectionState = mutableStateOf(ConnectionState())
        val submitted = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                ConnectionContent(connectionState.value, { connectionState.value = connectionState.value.copy(token = it) }, { submitted.set(true) })
            }
        }

        composeRule.onNodeWithTag("token_input").performClick().performTextInput("temporary")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("token_input").performImeAction()
        composeRule.waitForIdle()

        assertEquals("temporary", connectionState.value.token)
        assertEquals(true, submitted.get())
    }

    @Test
    fun connectionScreenShowsLoadingState() {
        composeRule.setContent {
            MaterialTheme {
                ConnectionContent(ConnectionState(loading = true), {}, {})
            }
        }

        composeRule.onAllNodesWithText("Conectar conta").assertCountEquals(0)
    }

    @Test
    fun connectionScreenShowsTokenErrorState() {
        composeRule.setContent {
            MaterialTheme {
                ConnectionContent(ConnectionState(error = UiText.Resource(R.string.invalid_token)), {}, {})
            }
        }

        composeRule.onNodeWithText("Token inválido. Confira o token e tente novamente.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun connectionScreenScrollsOnShortDisplays() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxWidth().height(420.dp)) {
                    ConnectionContent(ConnectionState(), {}, {})
                }
            }
        }

        composeRule.onNodeWithTag("connect_submit").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deviceListRendersMatchingDeviceAndOpensIt() {
        val device = sampleDevice.copy(name = "Porta da entrada")
        var selected: Device? = null
        composeRule.setContent {
            MaterialTheme {
                DeviceListContent(
                    state = DevicesState(devices = listOf(device)),
                    onOriginSelected = {},
                    onDeviceSelected = { selected = it },
                    onLoadMore = {},
                    onRetry = {},
                    onDisconnect = {}
                )
            }
        }

        composeRule.onNodeWithText("Porta da entrada").assertIsDisplayed().performClick()

        assertEquals(device, selected)
    }

    @Test
    fun deviceListShowsLoadingState() {
        composeRule.setContent {
            MaterialTheme {
                DeviceListContent(DevicesState(loading = true), {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText("Buscando seus dispositivos…").assertIsDisplayed()
    }

    @Test
    fun deviceListShowsErrorStateAndRetries() {
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                DeviceListContent(
                    DevicesState(error = UiText.Resource(R.string.network_error)),
                    {}, {}, {}, { retried = true }, {}
                )
            }
        }
        composeRule.onNodeWithText("Sem conexão. Verifique a internet e tente novamente.").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun deviceListShowsEmptyStateAfterSuccessfulEmptyResponse() {
        composeRule.setContent {
            MaterialTheme {
                DeviceListContent(DevicesState(), {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText("Nenhum dispositivo encontrado").assertIsDisplayed()
    }

    @Test
    fun changingFilterKeepsCountersVisibleWhileRefreshingAndShowsUpdatedDevices() {
        val state = mutableStateOf(
            DevicesState(
                devices = listOf(
                    Device("linked-1", "lock-1", "Fechadura vinculada", "MFR 2030", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
                )
            )
        )
        composeRule.setContent {
            MaterialTheme {
                DeviceListContent(
                    state = state.value,
                    onOriginSelected = { origin -> state.value = state.value.copy(origin = origin, loading = true) },
                    onDeviceSelected = {},
                    onLoadMore = {},
                    onRetry = {},
                    onDisconnect = {}
                )
            }
        }

        composeRule.onNodeWithText("Compartilhados").performClick()
        composeRule.onNodeWithText("dispositivos").assertIsDisplayed()
        composeRule.onNodeWithText("online").assertIsDisplayed()
        composeRule.onNodeWithText("Atualizando dispositivos…").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                devices = listOf(
                    Device("shared-1", "hub-1", "Central compartilhada", "MCA 1002", DeviceCategory.GENERIC, DeviceOrigin.SHARED, true)
                ),
                loading = false
            )
        }

        composeRule.onNodeWithText("Central compartilhada").assertIsDisplayed()
        composeRule.onAllNodesWithText("Atualizando dispositivos…").assertCountEquals(0)
    }

    @Test
    fun lockScreenConfirmsBeforeOpening() {
        var opened: Boolean? = null
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = sampleDevice, snapshot = LockSnapshot(false, true, 2), loading = false),
                    onRefresh = {},
                    onOpen = { opened = it },
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Fechada").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir").performClick()
        composeRule.onNodeWithText("Confirmar").performClick()

        assertEquals(true, opened)
    }

    @Test
    fun lockScreenShowsStatusError() {
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = sampleDevice, loading = false, error = UiText.Resource(R.string.operation_failed)),
                    onRefresh = {}, onOpen = {}, onVolume = {}, onRemote = {}, onBack = {}, onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Não foi possível concluir a operação.").assertIsDisplayed()
    }

    @Test
    fun lockHistoryShowsTheTypedActionLabel() {
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(
                        device = sampleDevice,
                        snapshot = LockSnapshot(false, true, 2),
                        history = listOf(
                            LockEvent("2026-10-06T12:00:00Z", "", APPLICATION_HISTORY_METHOD, LockAction.OPENED),
                            LockEvent("2026-10-06T11:00:00Z", "", APPLICATION_HISTORY_METHOD, LockAction.CLOSED)
                        )
                    ),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Abriu a fechadura").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fechou a fechadura").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Aplicativo", substring = true).assertCountEquals(2)
    }

    @Test
    fun lockHistoryShowsLoadingInsteadOfEmptyStateUntilRequestCompletes() {
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = sampleDevice, historyLoading = true),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Carregando histórico…").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Nenhum evento disponível.").assertCountEquals(0)
    }

    @Test
    fun lockHistoryIsHiddenAfterSuccessfulEmptyResponse() {
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = sampleDevice, historyLoading = false),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Nenhum evento disponível.").assertDoesNotExist()
        composeRule.onNodeWithText("Histórico de abertura").assertDoesNotExist()
    }

    @Test
    fun lockHistoryErrorCanBeRetried() {
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(
                        device = sampleDevice,
                        historyError = UiText.Resource(R.string.network_error)
                    ),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {},
                    onHistoryRetry = { retried = true }
                )
            }
        }

        composeRule.onNodeWithText("Tentar novamente").performScrollTo().performClick()

        assertTrue(retried)
    }

    @Test
    fun lockRemoteSwitchRequestsEnabledState() {
        var requestedState: Boolean? = null
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(
                        device = sampleDevice,
                        snapshot = LockSnapshot(false, false, 2),
                        loading = false
                    ),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = { requestedState = it },
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithTag("remote_access_toggle").performClick()

        assertEquals(true, requestedState)
    }

    @Test
    fun lockScreenDisablesOpenAndCloseWhenRemoteAccessIsOff() {
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = sampleDevice, snapshot = LockSnapshot(false, false, 2), loading = false),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Abrir").assertIsNotEnabled()
        composeRule.onNodeWithText("Fechar").assertIsNotEnabled()
        composeRule.onNodeWithText("Habilite a abertura remota para usar os controles da fechadura.").assertIsDisplayed()
    }

    @Test
    fun lockScreenDisablesCommandsWhileRefreshingStatus() {
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = sampleDevice, snapshot = LockSnapshot(false, true, 2), loading = true),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = {}
                )
            }
        }

        composeRule.onNodeWithText("Abrir").assertIsNotEnabled()
        composeRule.onNodeWithText("Fechar").assertIsNotEnabled()
        composeRule.onNodeWithText("Atualizar status").assertIsNotEnabled()
    }

    @Test
    fun cameraScreenActionCanBeOpenedFromDeviceDetails() {
        val device = sampleDevice.copy(category = DeviceCategory.CAMERA, model = "iM4 Dual")
        var cameraOpened = false
        composeRule.setContent {
            MaterialTheme {
                LockContent(
                    state = LockState(device = device, loading = false),
                    onRefresh = {},
                    onOpen = {},
                    onVolume = {},
                    onRemote = {},
                    onBack = {},
                    onCamera = { cameraOpened = true }
                )
            }
        }

        composeRule.onNodeWithText("Assistir ao vivo").performClick()

        assertEquals(true, cameraOpened)
    }

    @Test
    fun cameraScreenShowsStartActionAfterQuotaLoads() {
        var started = false
        composeRule.setContent {
            MaterialTheme {
                CameraContent(CameraState(quota = 1.0, loading = false), {}, {}, { started = true })
            }
        }

        composeRule.onNodeWithText("Iniciar transmissão").assertIsDisplayed().performClick()

        assertEquals(true, started)
    }

    @Test
    fun cameraScreenShowsLoadingWhileQuotaIsRequested() {
        composeRule.setContent {
            MaterialTheme {
                CameraContent(CameraState(loading = true), {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Preparando transmissão…").assertIsDisplayed()
    }

    @Test
    fun cameraPlayerErrorOffersRetry() {
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                CameraPlayerStatus(loading = false, failed = true, onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText("Erro ao carregar a transmissão. Tente novamente.").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").performClick()

        assertEquals(true, retried)
    }

    @Test
    fun cameraScreenDisablesStartActionWhileRequestIsPending() {
        composeRule.setContent {
            MaterialTheme {
                CameraContent(CameraState(quota = 1.0, loading = false, startRequested = true), {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Iniciar transmissão").assertIsNotEnabled()
    }

    @Test
    fun cameraScreenShowsOnlyRetryActionAfterServiceError() {
        var retried = false
        var started = false
        composeRule.setContent {
            MaterialTheme {
                CameraContent(CameraState(loading = false, error = UiText.Resource(R.string.camera_service_retry)), {}, { retried = true }, { started = true })
            }
        }

        composeRule.onNodeWithText("Erro ao consultar o serviço, tente novamente.").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").assertIsDisplayed().performClick()

        assertEquals(true, retried)
        assertFalse(started)
    }

    @Test
    fun cameraScreenExplainsUnavailableTransmissionWhenQuotaIsEmpty() {
        var started = false
        composeRule.setContent {
            MaterialTheme {
                CameraContent(CameraState(quota = 0.0, loading = false), {}, {}, { started = true })
            }
        }

        composeRule.onNodeWithText("Transmissão indisponível").assertIsDisplayed()
        assertFalse(started)
    }

    @Test
    fun cameraWebViewAllowsOnlySameTrustedHttpsOrigin() {
        val origin = Uri.parse("https://open-casainteligente.intelbras.com.br/player")

        assertTrue(Uri.parse("https://open-casainteligente.intelbras.com.br/stream").isTrustedVideoTarget(origin))
        assertTrue(Uri.parse("https://open-casainteligente.intelbras.com.br:443/stream").isTrustedVideoTarget(origin))
        assertFalse(Uri.parse("http://open-casainteligente.intelbras.com.br/stream").isTrustedVideoTarget(origin))
        assertFalse(Uri.parse("https://attacker.example/stream").isTrustedVideoTarget(origin))
        assertFalse(Uri.parse("https://open-casainteligente.intelbras.com.br:8443/stream").isTrustedVideoTarget(origin))
        assertFalse(Uri.parse("https://attacker@open-casainteligente.intelbras.com.br/stream").isTrustedVideoTarget(origin))
    }

    private companion object {
        val sampleDevice = Device("SN-9", "lock-2", "Porta", "MFR 7000", DeviceCategory.LOCK, DeviceOrigin.LINKED, true)
    }
}



