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

        // Load HTML from assets as a string for "fail-safe" loading. If asset missing,
        // use a Lightweight Charts template that exposes `window.updateData(valuesJson)`
        // so Kotlin can push numeric series into the chart. This provides a TradingView-like
        // lightweight chart experience without external paywalled libraries.
        val htmlContent = remember {
                try {
                        val inputStream = context.assets.open("mini-chart.html")
                        val reader = InputStreamReader(inputStream)
                        reader.readText()
                } catch (e: Exception) {
                        // Lightweight Charts standalone bundle from unpkg
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                html, body, #chart { height:100%; margin:0; padding:0; background: transparent; }
                                #chart { box-sizing: border-box; }
                            </style>
                        </head>
                        <body>
                            <div id="chart"></div>

                            <script src="https://unpkg.com/lightweight-charts@3.7.0/dist/lightweight-charts.standalone.production.js"></script>
                            <script>
                                const chart = LightweightCharts.createChart(document.getElementById('chart'), {
                                    width: document.getElementById('chart').clientWidth,
                                    height: document.getElementById('chart').clientHeight,
                                    layout: { backgroundColor: 'transparent', textColor: '#FFFFFF' },
                                    rightPriceScale: { visible: false },
                                    timeScale: { visible: false }
                                });

                                const series = chart.addLineSeries({ color: '#3EA6FF', lineWidth: 2 });

                                window.updateData = function(json) {
                                    try {
                                        const arr = JSON.parse(json);
                                        if (!Array.isArray(arr) || arr.length === 0) return;

                                        const now = Math.floor(Date.now() / 1000);
                                        const data = arr.map((v, i) => ({ time: now - (arr.length - i) * 60, value: Number(v) }));

                                        series.setData(data);
                                        chart.resize(document.getElementById('chart').clientWidth, document.getElementById('chart').clientHeight);
                                    } catch (err) {
                                        console.error(err);
                                    }
                                };

                                // ensure chart resizes when container changes
                                window.addEventListener('resize', function() {
                                    chart.resize(document.getElementById('chart').clientWidth, document.getElementById('chart').clientHeight);
                                });
                            </script>
                        </body>
                        </html>
                        """.trimIndent()
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