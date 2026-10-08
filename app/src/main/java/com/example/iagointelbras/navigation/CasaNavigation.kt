package com.example.iagointelbras.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.feature.devices.camera.ui.CameraScreen
import com.example.iagointelbras.feature.devices.connection.ui.ConnectionScreen
import com.example.iagointelbras.feature.devices.connection.viewmodel.ConnectionViewModel
import com.example.iagointelbras.feature.devices.devices.ui.DeviceListScreen
import com.example.iagointelbras.feature.devices.lock.ui.LockScreen
import com.example.iagointelbras.feature.devices.navigation.CameraDestination
import com.example.iagointelbras.feature.devices.navigation.ConnectionDestination
import com.example.iagointelbras.feature.devices.navigation.DeviceListDestination
import com.example.iagointelbras.feature.devices.navigation.LockDestination
import com.example.iagointelbras.feature.devices.navigation.toCameraDestination
import com.example.iagointelbras.feature.devices.navigation.toLockDestination

@Composable
fun CasaNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = StartupDestination) {
        composable<StartupDestination> {
            val viewModel: ConnectionViewModel = hiltViewModel()
            val state = viewModel.state.collectAsStateWithLifecycle().value
            LaunchedEffect(state.loading, state.connected) {
                if (!state.loading) {
                    navController.openInitialDestination(state.connected)
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable<ConnectionDestination> {
            ConnectionScreen(hiltViewModel()) { navController.openDeviceList() }
        }
        composable<DeviceListDestination> {
            DeviceListScreen(
                viewModel = hiltViewModel(),
                onDeviceSelected = navController::openLock,
                onDisconnected = navController::openConnection
            )
        }
        composable<LockDestination> {
            LockScreen(
                viewModel = hiltViewModel(),
                onBack = navController::popBackStack,
                onCamera = navController::openCamera
            )
        }
        composable<CameraDestination> {
            CameraScreen(hiltViewModel(), onBack = navController::popBackStack)
        }
    }
}

private fun NavHostController.openInitialDestination(connected: Boolean) {
    if (connected) {
        navigate(DeviceListDestination) {
            popUpTo<StartupDestination> { inclusive = true }
        }
    } else {
        navigate(ConnectionDestination) {
            popUpTo<StartupDestination> { inclusive = true }
        }
    }
}

private fun NavHostController.openDeviceList() {
    navigate(DeviceListDestination) {
        popUpTo<ConnectionDestination> { inclusive = true }
    }
}

private fun NavHostController.openConnection() {
    navigate(ConnectionDestination) {
        popUpTo<DeviceListDestination> { inclusive = true }
    }
}

private fun NavHostController.openLock(device: Device) = navigate(device.toLockDestination())

private fun NavHostController.openCamera(device: Device) = navigate(device.toCameraDestination())
