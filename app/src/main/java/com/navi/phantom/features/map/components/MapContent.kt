package com.navi.phantom.features.map.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.navi.phantom.features.map.logic.MapController
import com.navi.phantom.features.map.logic.MapPickerUiState

private const val MAP_BASE_URL = "file:///android_asset/map.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun MapContent(
    isSatelliteMode: Boolean,
    onCameraMove: (latitude: Double, longitude: Double) -> Unit,
    onCameraMoving: (Boolean) -> Unit,
    onMapReady: (MapController) -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val currentOnCameraMove = rememberUpdatedState(onCameraMove)
    val currentOnCameraMoving = rememberUpdatedState(onCameraMoving)
    val currentOnMapReady = rememberUpdatedState(onMapReady)

    val controller = remember { WebViewMapController() }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            @SuppressLint("AllowAllHostsAccess")
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.allowUniversalAccessFromFileURLs = true
            webViewClient = WebViewClient()

            // Use onJsPrompt as the JS→Android bridge
            webChromeClient = object : WebChromeClient() {
                override fun onJsPrompt(
                    view: WebView, url: String?, message: String?,
                    defaultValue: String?, result: JsPromptResult
                ): Boolean {
                    if (message == null) {
                        result.cancel()
                        return true
                    }
                    val parts = message.split(":")
                    when (parts[0]) {
                        "cameraMove" -> {
                            val lat = parts.getOrNull(1)?.toDoubleOrNull()
                            val lng = parts.getOrNull(2)?.toDoubleOrNull()
                            if (lat != null && lng != null) {
                                mainHandler.post { currentOnCameraMove.value(lat, lng) }
                            }
                        }
                        "cameraMoving" -> {
                            val moving = parts.getOrNull(1) == "true"
                            mainHandler.post { currentOnCameraMoving.value(moving) }
                        }
                        "mapReady" -> {
                            mainHandler.post {
                                controller.markReady()
                                currentOnMapReady.value(controller)
                                // Recalculate map dimensions now that the WebView is laid out
                                controller.runJs("map.invalidateSize()")
                            }
                        }
                    }
                    result.confirm()
                    return true
                }
            }
        }
    }

    LaunchedEffect(webView) {
        controller.webView = webView
    }

    LaunchedEffect(isSatelliteMode) {
        controller.runJs("setSatelliteMode($isSatelliteMode)")
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }

    AndroidView(
        factory = { _ ->
            webView.apply {
                val mapUrl = "$MAP_BASE_URL#${MapPickerUiState.DEFAULT_LATITUDE},${MapPickerUiState.DEFAULT_LONGITUDE}"
                post { loadUrl(mapUrl) }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private class WebViewMapController : MapController {
    var webView: WebView? = null
    private var isReady = false
    private val pendingCommands = mutableListOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun markReady() {
        isReady = true
        val wv = webView ?: return
        pendingCommands.forEach { js ->
            mainHandler.post { wv.evaluateJavascript(js, null) }
        }
        pendingCommands.clear()
    }

    @Synchronized
    fun runJs(js: String) {
        if (isReady && webView != null) {
            mainHandler.post { webView?.evaluateJavascript(js, null) }
        } else {
            pendingCommands.add(js)
        }
    }

    override fun animateCamera(latitude: Double, longitude: Double, zoom: Double) {
        runJs("animateCamera($latitude, $longitude, $zoom)")
    }
}
