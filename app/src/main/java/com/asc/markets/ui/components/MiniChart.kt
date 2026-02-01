package com.asc.markets.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniChart(values: List<Double>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val jsonPayload = remember(values) { 
        Json.encodeToString(values).replace("'", "\\'") 
    }

    // Load HTML from assets as a string for "fail-safe" loading
    val htmlContent = remember {
        try {
            val inputStream = context.assets.open("mini-chart.html")
            val reader = InputStreamReader(inputStream)
            reader.readText()
        } catch (e: Exception) {
            // Fallback empty template if asset is missing
            "<!DOCTYPE html><html><body style='background:transparent;'><div id='chart' style='width:100vw;height:100vh;'></div></body></html>"
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true // CRITICAL for chart engine
                    databaseEnabled = true
                    allowFileAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                
                setOnTouchListener { _, _ -> true }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        evaluateJavascript("if(window.updateData) { window.updateData('$jsonPayload'); }", null)
                    }
                }
                
                // Using BaseURL allows the script in HTML to find relative resources if needed
                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Re-injection on data update
            webView.evaluateJavascript("if(window.updateData) { window.updateData('$jsonPayload'); }", null)
        },
        modifier = modifier
    )
}