package com.asc.markets.ui.components

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

@Composable
fun TradingViewChart(symbol: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mockData = remember(symbol) { generateMockChartData(symbol) }
    val jsonData = remember(mockData) { Json.encodeToString<List<ForexDataPoint>>(mockData) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Avoid reloading the page on minor recompositions
                loadUrl("file:///android_asset/chart.html")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Inject the mock data into the JS function
                        // Use double-escaping for the JSON string
                        evaluateJavascript("updateData('${jsonData.replace("'", "\\'")}')", null)
                    }
                }
            }
        },
        modifier = modifier
    )
}
