package com.asc.markets.ui.terminal.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.asc.markets.ui.terminal.viewmodels.ChartViewModel
import com.asc.markets.ui.terminal.models.Candle
import com.asc.markets.ui.terminal.models.DrawingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChartJSBridge(
    private val webView: WebView,
    private val viewModel: ChartViewModel,
    private val scope: CoroutineScope
) {
    @JavascriptInterface
    fun onTrendlineCreated(data: String) {
        scope.launch(Dispatchers.Main) {
            viewModel.addDrawing(DrawingItem(
                id = "draw_${System.currentTimeMillis()}",
                type = "trendline"
            ))
        }
    }

    @JavascriptInterface
    fun onDrawingDeleted(id: String) {
        scope.launch(Dispatchers.Main) {
            viewModel.removeDrawing(id)
        }
    }

    @JavascriptInterface
    fun onDrawingSelected(id: String?) {
        scope.launch(Dispatchers.Main) {
            viewModel.selectDrawing(id)
        }
    }

    @JavascriptInterface
    fun onScrolledBackChanged(scrolled: Boolean) {
        scope.launch(Dispatchers.Main) {
            viewModel.setIsScrolledBack(scrolled)
        }
    }

    @JavascriptInterface
    fun onChartReady() {
        scope.launch(Dispatchers.Main) {
            sendInitialData()
        }
    }

    fun setSymbol(symbol: String) {
        webView.evaluateJavascript("chart.setSymbol('$symbol')", null)
    }

    fun setTimeframe(tf: String) {
        webView.evaluateJavascript("chart.setTimeframe('$tf')", null)
    }

    fun setIndicator(indicator: String, enabled: Boolean) {
        webView.evaluateJavascript("chart.setIndicator('$indicator', $enabled)", null)
    }

    fun setTool(tool: String) {
        webView.evaluateJavascript("chart.setTool('$tool')", null)
    }

    fun setChartType(type: String) {
        webView.evaluateJavascript("chart.setChartType('$type')", null)
    }

    fun setTheme(theme: String) {
        webView.evaluateJavascript("chart.setTheme('$theme')", null)
    }

    fun setTimezone(timezone: String) {
        webView.evaluateJavascript("chart.setTimezone('$timezone')", null)
    }

    fun setMagnetMode(enabled: Boolean) {
        webView.evaluateJavascript("chart.setMagnetMode($enabled)", null)
    }

    fun setLocked(enabled: Boolean) {
        webView.evaluateJavascript("chart.setLocked($enabled)", null)
    }

    fun setCrosshairEnabled(enabled: Boolean) {
        webView.evaluateJavascript("chart.setCrosshairEnabled($enabled)", null)
    }

    fun clearDrawings() {
        webView.evaluateJavascript("chart.clearDrawings()", null)
    }

    fun resetZoom() {
        webView.evaluateJavascript("chart.resetZoom()", null)
    }

    fun scrollToRealTime() {
        webView.evaluateJavascript("chart.scrollToRealTime()", null)
    }

    fun executeTrade(type: String) {
        webView.evaluateJavascript("chart.executeTrade('$type')", null)
    }

    fun removeTrade(type: String) {
        webView.evaluateJavascript("chart.removeTrade('$type')", null)
    }

    fun removeDrawing(id: String) {
        webView.evaluateJavascript("chart.removeDrawing('$id')", null)
    }

    private fun sendInitialData() {
        val data = viewModel.candleData.value
        val json = data.joinToString(prefix = "[", postfix = "]") { candle ->
            """{"time":${candle.time / 1000},"open":${candle.open},"high":${candle.high},"low":${candle.low},"close":${candle.close},"volume":${candle.volume ?: 0.0},"rsi":${candle.rsi ?: "null"}}"""
        }
        webView.evaluateJavascript("chart.setData($json)", null)
    }

    fun updateCandle(candle: Candle) {
        val json = """{"time":${candle.time / 1000},"open":${candle.open},"high":${candle.high},"low":${candle.low},"close":${candle.close},"volume":${candle.volume ?: 0.0},"rsi":${candle.rsi ?: "null"}}"""
        webView.evaluateJavascript("chart.updateData($json)", null)
    }
}
