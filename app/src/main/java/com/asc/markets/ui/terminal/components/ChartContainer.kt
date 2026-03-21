package com.asc.markets.ui.terminal.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.asc.markets.ui.terminal.models.CanvasSettings
import com.asc.markets.ui.terminal.viewmodels.ChartViewModel
import com.google.gson.Gson
import android.graphics.Color as AndroidColor

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChartContainer(
    canvasSettings: CanvasSettings,
    viewModel: ChartViewModel
) {
    val candleData by viewModel.candleData.collectAsState()
    val gson = Gson()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF131722))) { // TradingView Dark
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Initial data load when page is ready
                            if (candleData.isNotEmpty()) {
                                updateChartData(this@apply, candleData, gson)
                            }
                        }
                    }
                    
                    // Load your chart from assets
                    loadUrl("file:///android_asset/www/index.html")
                }
            },
            update = { webView ->
                // Push new data updates from Kotlin to JS
                if (candleData.isNotEmpty()) {
                    updateChartData(webView, candleData, gson)
                }
            }
        )
    }
}

private fun updateChartData(webView: WebView, data: Any, gson: Gson) {
    val json = gson.toJson(data)
    // This calls the global function defined in your React/JS code
    webView.evaluateJavascript("if(window.updateChartData) { window.updateChartData($json); }", null)
}
