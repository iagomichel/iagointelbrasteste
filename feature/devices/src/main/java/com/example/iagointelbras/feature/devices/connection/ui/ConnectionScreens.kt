package com.example.iagointelbras.feature.devices.connection.ui

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.common.ui.ErrorMessage
import com.example.iagointelbras.feature.devices.connection.viewmodel.ConnectionState
import com.example.iagointelbras.feature.devices.connection.viewmodel.ConnectionViewModel
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel, onConnected: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.connected) {
        if (state.connected) onConnected()
    }
    ConnectionContent(state, viewModel::updateToken, viewModel::connect)
}

@Composable
fun ConnectionContent(state: ConnectionState, onTokenChange: (String) -> Unit, onConnect: () -> Unit) {
    var revealToken by remember { mutableStateOf(false) }
    var clipboardEmpty by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    Scaffold(containerColor = Color(0xFFF7F8F3)) { insets ->
        Box(
            Modifier.fillMaxSize().padding(insets).imePadding().background(Color(0xFFF7F8F3)),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(R.drawable.connection_bottom_waves),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(90.dp),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomStart
            )
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                ConnectionHero()
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                ConnectionHeading()
                Spacer(Modifier.height(16.dp))
                ConnectionTokenField(
                    state = state,
                    onTokenChange = {
                        clipboardEmpty = false
                        onTokenChange(it)
                    },
                    onConnect = onConnect,
                    revealToken = revealToken,
                    onRevealToken = { revealToken = !revealToken },
                    onPaste = {
                        val pastedText = clipboard.readText(context)
                        clipboardEmpty = pastedText == null
                        pastedText?.let(onTokenChange)
                    }
                )
                if (clipboardEmpty) {
                    Text(
                        stringResource(R.string.clipboard_empty),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                state.error?.let { ErrorMessage(it) }
                Spacer(Modifier.height(18.dp))
                ConnectionSubmitButton(state.loading, onConnect)
                ConnectionStorageNote()
                }
            }
        }
    }
}

@Composable
private fun ConnectionHero() {
    Box(Modifier.fillMaxWidth().height(228.dp)) {
        Image(
            painter = painterResource(R.drawable.connection_hero_house),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeWifiMark()
            Text(
                stringResource(R.string.connection_brand_full),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeWifiMark() {
    val tint = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(48.dp)) {
        val stroke = 3.5.dp.toPx()
        val roof = Path().apply {
            moveTo(size.width * .12f, size.height * .48f)
            lineTo(size.width * .5f, size.height * .12f)
            lineTo(size.width * .88f, size.height * .48f)
            lineTo(size.width * .88f, size.height * .9f)
            lineTo(size.width * .68f, size.height * .9f)
            moveTo(size.width * .32f, size.height * .9f)
            lineTo(size.width * .12f, size.height * .9f)
            close()
        }
        drawPath(roof, tint, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(
            color = tint,
            startAngle = 220f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * .29f, size.height * .48f),
            size = androidx.compose.ui.geometry.Size(size.width * .42f, size.height * .3f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = 220f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * .39f, size.height * .57f),
            size = androidx.compose.ui.geometry.Size(size.width * .22f, size.height * .16f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawCircle(tint, radius = stroke * .7f, center = androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .79f))
    }
}

@Composable
private fun ConnectionHeading() {
    Text(
        stringResource(R.string.connection_headline),
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 39.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.connection_intro),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 25.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ConnectionTokenField(
    state: ConnectionState,
    onTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
    revealToken: Boolean,
    onRevealToken: () -> Unit,
    onPaste: () -> Unit
) {
    Text(
        stringResource(R.string.connection_token_hint),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inlinePaste = maxWidth >= 300.dp
        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth().height(62.dp).testTag("token_input"),
            placeholder = { Text(stringResource(R.string.access_token_label), maxLines = 1) },
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(onDone = { if (!state.loading) onConnect() }),
            visualTransformation = if (revealToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRevealToken) {
                        Icon(
                            imageVector = if (revealToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (revealToken) R.string.hide_token_accessibility else R.string.show_token_accessibility
                            )
                        )
                    }
                    if (inlinePaste) {
                        Spacer(Modifier.width(1.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        TextButton(
                            onClick = onPaste,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.padding(horizontal = 8.dp).background(Color(0xFFE9F1EA), RoundedCornerShape(20.dp))
                        ) {
                            Text(stringResource(R.string.paste_token))
                        }
                    }
                }
            }
        )
        if (!inlinePaste) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPaste) {
                    Text(stringResource(R.string.paste_token))
                }
            }
        }
    }
}

private fun ClipboardManager.readText(context: Context): String? = runCatching {
    primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.let { item -> item.text?.toString() ?: item.coerceToText(context)?.toString() }
        ?.trim()
        ?.takeIf(String::isNotBlank)
}.getOrNull()

@Composable
private fun ConnectionSubmitButton(loading: Boolean, onConnect: () -> Unit) {
    Button(
        onClick = onConnect,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("connect_submit"),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(25.dp))
            Text(stringResource(R.string.connect_account), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        }
    }
}

@Composable
private fun ConnectionStorageNote() {
    Row(
        Modifier.fillMaxWidth().padding(start = 28.dp, top = 18.dp, end = 28.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp).size(22.dp)
        )
        Text(
            stringResource(R.string.token_storage_note),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@Preview(showBackground = true, name = "Conexão")
private fun ConnectionContentPreview() {
    MaterialTheme {
        ConnectionContent(ConnectionState(), {}, {})
    }
}



