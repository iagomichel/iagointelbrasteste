package com.example.iagointelbras.feature.devices.devices.ui

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.ui.EmptyState
import com.example.iagointelbras.feature.devices.common.ui.ErrorPanel
import com.example.iagointelbras.feature.devices.common.ui.LoadingState
import com.example.iagointelbras.feature.devices.common.ui.label
import com.example.iagointelbras.feature.devices.common.ui.labelResource
import com.example.iagointelbras.feature.devices.devices.viewmodel.DevicesState
import com.example.iagointelbras.feature.devices.devices.viewmodel.DevicesViewModel
import com.example.iagointelbras.domain.model.Device
import com.example.iagointelbras.domain.model.DeviceCategory
import com.example.iagointelbras.domain.model.DeviceOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DeviceListScreen(viewModel: DevicesViewModel, onDeviceSelected: (Device) -> Unit, onDisconnected: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeviceListContent(
        state = state,
        onOriginSelected = viewModel::selectOrigin,
        onDeviceSelected = onDeviceSelected,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        onDisconnect = { viewModel.disconnect(onDisconnected) }
    )
}

@Composable
fun DeviceListContent(
    state: DevicesState,
    onOriginSelected: (DeviceOrigin) -> Unit,
    onDeviceSelected: (Device) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit
) {
    var filtersBottom by remember { mutableIntStateOf(0) }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = { DeviceListTopBar(onDisconnect) }
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            HomeBackdrop(filtersBottom)
            Column(
                Modifier.widthIn(max = 760.dp).fillMaxSize()
                    .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
                    .padding(bottom = navigationBarPadding, start = 20.dp, end = 20.dp)
            ) {
                HomeOverview(
                    deviceCount = state.devices.size,
                    onlineCount = state.devices.count { it.online == true },
                    loading = state.loading
                )
                OriginFilters(state.origin, onOriginSelected, Modifier.onGloballyPositioned { coordinates ->
                    val measuredBottom = (coordinates.positionInRoot().y + coordinates.size.height).toInt()
                    if (filtersBottom != measuredBottom) filtersBottom = measuredBottom
                })
                when {
                    state.loading && state.devices.isEmpty() -> LoadingState(stringResource(R.string.loading_devices))
                    state.error != null -> ErrorPanel(state.error, onRetry)
                    state.devices.isEmpty() -> EmptyState(stringResource(R.string.no_devices_title), stringResource(R.string.no_devices_message))
                    else -> Box(Modifier.weight(1f).fillMaxWidth()) {
                        DeviceItems(
                            devices = state.devices,
                            loadingMore = state.loadingMore,
                            canLoadMore = state.canLoadMore,
                            onSelect = onDeviceSelected,
                            onLoadMore = onLoadMore,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (state.loading) DeviceListLoadingOverlay()
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DeviceListTopBar(onDisconnect: () -> Unit) {
    TopAppBar(
        modifier = Modifier.heightIn(max = 52.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.home_brand))
            }
        },
        actions = {
            FilterChip(
                selected = false,
                onClick = onDisconnect,
                label = { Text(stringResource(R.string.disconnect)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.88f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun HomeBackdrop(heightPixels: Int) {
    val density = LocalDensity.current
    val height = with(density) { heightPixels.toDp() }
    if (height <= 0.dp) return
    Box(Modifier.fillMaxWidth().height(height)) {
        Image(
            painter = painterResource(R.drawable.home_hero_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.72f),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                    startY = with(density) { 220.dp.toPx() },
                    endY = with(density) { height.toPx() }
                )
            )
        )
    }
}

@Composable
private fun DeviceListLoadingOverlay() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.refreshing_devices),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeOverview(deviceCount: Int, onlineCount: Int, loading: Boolean) {
    Column(Modifier.padding(top = 0.dp)) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeStat(
                value = deviceCount.toString(),
                label = stringResource(R.string.stat_devices),
                icon = Icons.Filled.DevicesOther,
                modifier = Modifier.weight(1f),
                loading = loading
            )
            HomeStat(
                value = onlineCount.toString(),
                label = stringResource(R.string.stat_online),
                icon = Icons.Filled.Wifi,
                modifier = Modifier.weight(1f),
                highlight = true,
                loading = loading
            )
        }
        Text(stringResource(R.string.home_devices), modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeStat(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    loading: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = if (highlight) colorResource(R.color.home_stat_online).copy(alpha = 0.92f)
        else colorResource(R.color.home_stat_devices).copy(alpha = 0.92f)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(42.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp))
            }
            Column(Modifier.padding(start = 10.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun OriginFilters(selected: DeviceOrigin, onSelected: (DeviceOrigin) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.padding(top = 4.dp, bottom = 6.dp).horizontalScroll(rememberScrollState())) {
        DeviceOrigin.entries.filterNot { it == DeviceOrigin.UNKNOWN }.forEach { origin ->
            FilterChip(
                selected = origin == selected,
                onClick = { onSelected(origin) },
                label = { Text(stringResource(origin.labelResource())) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.86f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun DeviceItems(
    devices: List<Device>,
    loadingMore: Boolean,
    canLoadMore: Boolean,
    onSelect: (Device) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxSize()) {
        items(devices, key = { it.serial + it.productId }) { device -> DeviceCard(device, onClick = { onSelect(device) }) }
        item {
            if (loadingMore) LoadingState(stringResource(R.string.loading_more_devices))
            else if (canLoadMore) OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.load_more)) }
        }
    }
}

@Composable
fun DeviceCard(device: Device, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            DeviceIcon(device.category)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(device.name.ifBlank { stringResource(R.string.generic_device) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(device.model.ifBlank { stringResource(R.string.generic_device) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.serial, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(device.category.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Box(Modifier.size(3.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50)))
                    DeviceStatus(device.online)
                    Text(stringResource(device.origin.labelResource()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeviceIcon(category: DeviceCategory) {
    val tile = when (category) {
        DeviceCategory.CAMERA -> colorResource(R.color.device_camera_tile)
        DeviceCategory.LOCK -> colorResource(R.color.device_lock_tile)
        else -> colorResource(R.color.device_other_tile)
    }
    val image = when (category) {
        DeviceCategory.CAMERA -> R.drawable.device_camera
        DeviceCategory.LOCK -> R.drawable.device_lock
        else -> R.drawable.device_hub
    }
    Surface(shape = RoundedCornerShape(20.dp), color = tile, modifier = Modifier.size(78.dp)) {
        Image(
            painter = painterResource(image),
            contentDescription = category.label(),
            modifier = Modifier.fillMaxSize().padding(2.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DeviceStatus(online: Boolean?) {
    val label = when (online) {
        true -> stringResource(R.string.online)
        false -> stringResource(R.string.offline)
        null -> stringResource(R.string.status_unavailable)
    }
    val color = if (online == true) colorResource(R.color.device_online_text) else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (online == true) colorResource(R.color.device_online_background) else MaterialTheme.colorScheme.surfaceVariant
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}



@Preview(showBackground = true, name = "Dispositivos")
@Composable
private fun DeviceListContentPreview() {
    val devices = listOf(
        Device("CAM-001", "camera", "Câmera da sala", "iM4 Dual", DeviceCategory.CAMERA, DeviceOrigin.LINKED, true),
        Device("LOCK-001", "lock", "Porta de entrada", "MFR 2030", DeviceCategory.LOCK, DeviceOrigin.LINKED, true),
        Device("HUB-001", "hub", "Central", "MCA 1002", DeviceCategory.GENERIC, DeviceOrigin.SHARED, false)
    )
    MaterialTheme {
        DeviceListContent(DevicesState(devices = devices), {}, {}, {}, {}, {})
    }
}



