package com.trading.app.ui.chart

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.trading.app.models.Candle
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import javax.inject.Inject
import javax.inject.Singleton

class TradingChartCoordinator @Inject constructor(
    private val jsBuilder: TradingChartJsBuilder
) {
    private var webView: WebView? = null
    private val gson = Gson()
    private var pendingData: List<Candle>? = null
    private var pendingRsiPeriod: Int = 14
    private var isRsiVisible: Boolean = false
    private var isJsLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(view: WebView) {
        this.webView = view
        this.isJsLoaded = false
        
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isJsLoaded = true
                setRsiVisible(isRsiVisible)
                pendingData?.let {
                    updateData(it, pendingRsiPeriod)
                    pendingData = null
                }
            }
        }

        // Load the generated HTML/JS from the builder
        view.loadDataWithBaseURL("https://tvc", jsBuilder.buildHtml(), "text/html", "UTF-8", null)
    }

    /**
     * Maps LightweightCharts CandlestickData to our internal Candle model and sends to JS
     */
    fun updateData(data: List<Candle>, rsiPeriod: Int = 14) {
        if (!isJsLoaded) {
            pendingData = data
            pendingRsiPeriod = rsiPeriod
            return
        }
        
        val json = gson.toJson(data)
        webView?.post {
            webView?.evaluateJavascript("window.updateData('$json', $rsiPeriod)", null)
        }
    }

    fun setRsiVisible(visible: Boolean) {
        isRsiVisible = visible
        if (isJsLoaded) {
            webView?.post {
                webView?.evaluateJavascript("window.setRsiVisible($visible)", null)
            }
        }
    }

    fun detach() {
        webView = null
        isJsLoaded = false
    }
}
