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
fun TradingViewChart(symbol: String, modifier: Modifier = Modifier, showRsi: Boolean = true, rsiPeriod: Int = 14) {
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
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <script src="https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js"></script>
                <style>
                    body { margin: 0; padding: 0; background-color: #0d0f1a; overflow: hidden; display: flex; flex-direction: column; height: 100vh; }
                    #price-chart { flex: 3; width: 100%; border-bottom: 1px solid #2b2b43; }
                    #rsi-chart { flex: 1; width: 100%; display: none; }
                </style>
            </head>
            <body>
                <div id="price-chart"></div>
                <div id="rsi-chart"></div>
                <script>
                    const chartOptions = {
                        layout: { background: { type: 'solid', color: '#0d0f1a' }, textColor: '#d1d4dc' },
                        grid: { vertLines: { color: 'rgba(42,46,57,0.5)' }, horzLines: { color: 'rgba(42,46,57,0.5)' } },
                        rightPriceScale: { borderColor: '#2b2b43' },
                        timeScale: { borderColor: '#2b2b43', visible: false }
                    };
                    const priceChart = LightweightCharts.createChart(document.getElementById('price-chart'), { ...chartOptions, height: document.getElementById('price-chart').clientHeight });
                    const rsiChart = LightweightCharts.createChart(document.getElementById('rsi-chart'), { ...chartOptions, height: document.getElementById('rsi-chart').clientHeight, timeScale: { ...chartOptions.timeScale, visible: true } });
                    const candleSeries = priceChart.addCandlestickSeries({ upColor: '#089981', downColor: '#f23645', borderVisible: false, wickUpColor: '#089981', wickDownColor: '#f23645' });
                    const rsiSeries = rsiChart.addLineSeries({ color: '#7e57c2', lineWidth: 2, priceFormat: { type: 'price', precision: 2 } });

                    function calculateRSI(data, period = 14) {
                        let results = [];
                        let avgGain = 0; let avgLoss = 0;
                        for (let i = 1; i < data.length; i++) {
                            const change = data[i].close - data[i-1].close;
                            const gain = change > 0 ? change : 0;
                            const loss = change < 0 ? -change : 0;
                            if (i <= period) {
                                avgGain += gain / period; avgLoss += loss / period;
                                if (i === period) results.push({ time: data[i].time, value: 100 - (100 / (1 + (avgGain / (avgLoss || 1)))) });
                            } else {
                                avgGain = (avgGain * (period - 1) + gain) / period;
                                avgLoss = (avgLoss * (period - 1) + loss) / period;
                                results.push({ time: data[i].time, value: 100 - (100 / (1 + (avgGain / (avgLoss || 1)))) });
                            }
                        }
                        return results;
                    }

                    let currentRsiPeriod = 14;
                    window.updateData = (jsonData, rsiPeriod) => {
                        if (rsiPeriod) currentRsiPeriod = rsiPeriod;
                        const data = JSON.parse(jsonData);
                        if (data && data.length > 0) {
                            candleSeries.setData(data);
                            rsiSeries.setData(calculateRSI(data, currentRsiPeriod));
                        }
                    };
                    window.setRsiVisible = (visible) => {
                        document.getElementById('rsi-chart').style.display = visible ? 'block' : 'none';
                        priceChart.resize(window.innerWidth, document.getElementById('price-chart').offsetHeight);
                        rsiChart.resize(window.innerWidth, document.getElementById('rsi-chart').offsetHeight);
                    };
                    window.addEventListener('resize', () => {
                        priceChart.resize(window.innerWidth, document.getElementById('price-chart').offsetHeight);
                        rsiChart.resize(window.innerWidth, document.getElementById('rsi-chart').offsetHeight);
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
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        evaluateJavascript("if(window.updateData) { window.updateData('$jsonData', $rsiPeriod); }", null)
                        evaluateJavascript("if(window.setRsiVisible) { window.setRsiVisible(${showRsi.toString().lowercase()}); }", null)
                    }
                }
                
                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.evaluateJavascript("if(window.updateData) { window.updateData('$jsonData', $rsiPeriod); }", null)
            webView.evaluateJavascript("if(window.setRsiVisible) { window.setRsiVisible(${showRsi.toString().lowercase()}); }", null)
        },
        modifier = modifier
    )
}
