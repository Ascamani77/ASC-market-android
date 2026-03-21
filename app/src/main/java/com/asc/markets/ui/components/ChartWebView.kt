package com.asc.markets.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.asc.markets.R

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun ChartWebView(
    modifier: Modifier = Modifier,
    url: String = "" // Ignored
) {
    val context = LocalContext.current
    
    // Load the REAL chart HTML from the assets/www folder
    val realChartHtml = remember {
        try {
            // Try the 'www' subfolder where Vite usually outputs
            context.assets.open("www/index.html")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ChartWebView", "Error loading assets/www/index.html, trying root", e)
            try {
                context.assets.open("index.html")
                    .bufferedReader().use { it.readText() }
            } catch (e2: Exception) {
                Log.e("ChartWebView", "Error loading assets/index.html", e2)
                // Fallback to the demo chart if index.html is missing
                try {
                    context.resources.openRawResource(R.raw.trading_chart)
                        .bufferedReader().use { it.readText() }
                } catch (e3: Exception) {
                    "<!DOCTYPE html><html><body style='background:#000; color:white;'>Error loading any chart resource</body></html>"
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                // Asset loader serves requests for the https://appassets.androidplatform.net origin
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                    .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(ctx))
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                        // Let the asset loader handle requests to the virtual origin
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("ChartWebView", "Page finished loading: $url")
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d("ChartWebView JS", "[${it.messageLevel()}] ${it.message()} -- line ${it.lineNumber()}")
                        }
                        return true
                    }
                }

                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        v.requestFocus()
                    }
                    false
                }

                // Load HTML string with the virtual domain; assetLoader will serve resource requests
                // We use /assets/www/ as base so that ./index.js resolves to assets/www/index.js
                // IMPORTANT: We use loadUrl with the virtual path instead of loadDataWithBaseURL
                // to avoid issues with relative path resolution for CSS and JS
                loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
            }
        }
    )
}
