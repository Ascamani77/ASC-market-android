package com.trading.app.ui.chart

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChartCoordinator @Inject constructor(
    private val jsBuilder: IndicatorJsBuilder
) {
    private var webView: WebView? = null
    private val gson = Gson()
    private var pendingData: List<CandlestickData>? = null
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
                pendingData?.let {
                    updateData(it)
                    pendingData = null
                }
            }
        }

        view.loadDataWithBaseURL("https://tvc", jsBuilder.buildFullChartHtml(), "text/html", "UTF-8", null)
    }

    fun updateData(data: List<CandlestickData>) {
        if (!isJsLoaded) {
            pendingData = data
            return
        }
        
        val json = gson.toJson(data)
        webView?.post {
            webView?.evaluateJavascript("window.updateData('$json')", null)
        }
    }

    fun detach() {
        webView = null
        isJsLoaded = false
    }
}
