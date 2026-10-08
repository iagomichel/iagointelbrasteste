package com.example.iagointelbras.feature.devices.camera.ui

import com.example.iagointelbras.feature.devices.R
import com.example.iagointelbras.feature.devices.camera.viewmodel.CameraState
import com.example.iagointelbras.feature.devices.camera.viewmodel.CameraViewModel
import com.example.iagointelbras.feature.devices.common.ui.EmptyState
import com.example.iagointelbras.feature.devices.common.ui.ErrorMessage
import com.example.iagointelbras.feature.devices.common.ui.LoadingState
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CameraScreen(viewModel: CameraViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CameraContent(
        state = state,
        onBack = onBack,
        onRetry = { if (state.quota == null) viewModel.refreshQuota() else viewModel.start() },
        onStart = viewModel::start
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CameraContent(state: CameraState, onBack: () -> Unit, onRetry: () -> Unit, onStart: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.camera_live_title)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.error != null) {
                ErrorMessage(state.error)
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.retry)) }
            } else {
                when {
                    state.loading -> LoadingState(stringResource(R.string.preparing_stream))
                    state.session != null -> CameraPlayer(state.session.monitorUrl)
                    state.quota?.let { it.isFinite() && it > 0.0 } == true -> Button(
                        onClick = onStart,
                        enabled = !state.startRequested,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.start_stream)) }
                    else -> EmptyState(
                        stringResource(R.string.camera_unavailable_title),
                        stringResource(R.string.camera_unavailable_message)
                    )
                }
            }
        }
    }
}
@Composable
private fun CameraPlayer(url: String) {
    val context = LocalContext.current
    val playerState = remember(url) { CameraPlayerState() }
    Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
        CameraWebView(url, context, playerState)
        CameraPlayerStatus(playerState.loading, playerState.failed, playerState::retry)
    }
}

@Composable
private fun CameraWebView(url: String, context: Context, playerState: CameraPlayerState) {
    val trustedUri = remember(url) { url.toUri() }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            createCameraWebView(
                context,
                url,
                trustedUri,
                playerState::onLoading,
                playerState::onLoaded,
                playerState::onError
            ).also(playerState::attach)
        },
        update = { playerState.update(it, url) },
        onRelease = playerState::release
    )
}

private class CameraPlayerState {
    var loading by mutableStateOf(true)
        private set
    var failed by mutableStateOf(false)
        private set
    private var webView: WebView? = null

    fun attach(view: WebView) {
        webView = view
    }

    fun update(view: WebView, url: String) {
        webView = view
        if (view.url != url) view.loadUrl(url)
    }

    fun onLoading() {
        loading = true
        failed = false
    }

    fun onLoaded() {
        loading = false
    }

    fun onError() {
        loading = false
        failed = true
    }

    fun retry() {
        webView?.reload()
        onLoading()
    }

    fun release(view: WebView) {
        if (webView === view) webView = null
        view.stopLoading()
        view.webViewClient = WebViewClient()
        view.webChromeClient = null
        view.loadUrl(EMPTY_WEB_PAGE)
        view.destroy()
    }
}

private fun createCameraWebView(
    context: Context,
    url: String,
    trustedUri: Uri,
    onLoading: () -> Unit,
    onLoaded: () -> Unit,
    onError: () -> Unit
) = WebView(context).apply {
    configureCameraSettings()
    webViewClient = TrustedCameraWebViewClient(trustedUri, onLoading, onLoaded, onError)
    loadUrl(url)
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureCameraSettings() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.setSupportMultipleWindows(false)
    settings.javaScriptCanOpenWindowsAutomatically = false
    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
}

private class TrustedCameraWebViewClient(
    private val trustedUri: Uri,
    private val onLoading: () -> Unit,
    private val onLoaded: () -> Unit,
    private val onError: () -> Unit
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.isTrustedVideoTarget(trustedUri)) return false
        if (request.isForMainFrame) onError()
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) = onLoading()

    override fun onPageFinished(view: WebView, url: String?) = onLoaded()

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError
    ) {
        if (request.isForMainFrame) onError()
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: android.webkit.WebResourceResponse
    ) {
        if (request.isForMainFrame && errorResponse.statusCode >= HTTP_ERROR_CODE) onError()
    }

    override fun onReceivedSslError(
        view: WebView,
        handler: android.webkit.SslErrorHandler,
        error: android.net.http.SslError
    ) {
        handler.cancel()
        onError()
    }
}

@Composable
internal fun CameraPlayerStatus(loading: Boolean, failed: Boolean, onRetry: () -> Unit) {
    when {
        loading && !failed -> LoadingState(stringResource(R.string.loading_live_stream))
        failed -> CameraPlayerError(onRetry)
    }
}

@Composable
internal fun CameraPlayerError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.camera_stream_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Preview(showBackground = true, name = "Erro na transmissão")
@Composable
private fun CameraPlayerErrorPreview() {
    MaterialTheme { CameraPlayerError {} }
}
internal fun Uri.isTrustedVideoTarget(origin: Uri) =
    scheme == "https" &&
        userInfo == null &&
        host == origin.host &&
        effectivePort() == origin.effectivePort()

private fun Uri.effectivePort() = port.takeIf { it != -1 } ?: HTTPS_PORT

private const val HTTPS_PORT = 443
private const val HTTP_ERROR_CODE = 400
private const val EMPTY_WEB_PAGE = "about:blank"



@Preview(showBackground = true, name = "Câmera ao vivo")
@Composable
private fun CameraContentPreview() {
    MaterialTheme {
        CameraContent(CameraState(quota = 1.0, loading = false), {}, {}, {})
    }
}



