package com.example.iagointelbras.feature.devices

import com.example.iagointelbras.feature.devices.navigation.toCameraDestination
import com.example.iagointelbras.feature.devices.navigation.toDevice
import com.example.iagointelbras.feature.devices.navigation.toLockDestination

import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin

import org.junit.Assert.assertEquals
import org.junit.Test

class CasaNavigationTest {
    @Test
    fun lockDestinationPreservesDeviceArguments() {
        val device = sampleDevice(DeviceCategory.LOCK)

        assertEquals(device, device.toLockDestination().toDevice())
    }

    @Test
    fun cameraDestinationPreservesDeviceArguments() {
        val device = sampleDevice(DeviceCategory.CAMERA)

        assertEquals(device, device.toCameraDestination().toDevice())
    }

    private fun sampleDevice(category: DeviceCategory) = Device(
        serial = "SERIAL-42",
        productId = "PRODUCT-9",
        name = "Porta da sala",
        model = "MFR 2030",
        category = category,
        origin = DeviceOrigin.SHARED,
        online = false,
        apiNamespace = "PARENT_SERIAL_CHILD"
    )
}



