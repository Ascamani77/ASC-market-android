package com.asc.markets.ui.components

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ChartWebView(
    modifier: Modifier = Modifier,
    height: Int = 400,
    url: String = "http://192.168.1.198:5173"
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        factory = { context ->
            WebView(context).apply {
                val webSettings: WebSettings = settings
                webSettings.javaScriptEnabled = true
                webSettings.domStorageEnabled = true
                webSettings.databaseEnabled = true
                webSettings.cacheMode = WebSettings.LOAD_DEFAULT
                webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        }
    )
}
