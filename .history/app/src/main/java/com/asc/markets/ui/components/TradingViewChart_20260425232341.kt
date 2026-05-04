package com.asc.markets.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.asc.markets.logic.generateMockChartData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.asc.markets.data.ForexDataPoint
import java.io.InputStreamReader

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewChart(symbol: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mockData = remember(symbol) { generateMockChartData(symbol) }
    val jsonData = remember(mockData) { 
        Json.encodeToString<List<ForexDataPoint>>(mockData).replace("'", "\\'") 
    }

    val htmlContent = remember {
        try {
            val inputStream = context.assets.open("chart.html")
            val reader = InputStreamReader(inputStream)
            reader.readText()
        } catch (e: Exception) {
            "<!DOCTYPE html><html><body style='background:#000;'></body></html>"
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        evaluateJavascript("if(window.updateData) { window.updateData('$jsonData'); }", null)
                    }
                }
                
                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.evaluateJavascript("if(window.updateData) { window.updateData('$jsonData'); }", null)
        },
        modifier = modifier
    )
}
