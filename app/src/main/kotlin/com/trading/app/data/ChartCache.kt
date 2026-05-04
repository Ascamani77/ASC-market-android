package com.trading.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.trading.app.models.OHLCData
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import java.io.File

class ChartCache(private val context: Context) {
    private val gson = GsonBuilder()
        .serializeSpecialFloatingPointValues()
        .create()
    private val cacheDir = File(context.cacheDir, "chart_data")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    private fun getCacheFile(symbol: String, timeframe: String): File {
        // Replace symbols that might be invalid in filenames
        val safeSymbol = symbol.replace("/", "_").replace(" ", "_").replace(":", "_").replace("!", "_").replace("*", "_")
        return File(cacheDir, "${safeSymbol}_${timeframe}.json")
    }

    fun saveHistory(symbol: String, timeframe: String, data: List<CandlestickData>) {
        if (data.isEmpty()) return
        try {
            val ohlcList = data.map { candle ->
                val timeVal = when (val t = candle.time) {
                    is Time.Utc -> t.timestamp
                    else -> 0L
                }
                OHLCData(
                    time = timeVal,
                    open = candle.open,
                    high = candle.high,
                    low = candle.low,
                    close = candle.close
                )
            }
            val json = gson.toJson(ohlcList)
            getCacheFile(symbol, timeframe).writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadHistory(symbol: String, timeframe: String): List<CandlestickData> {
        return try {
            val file = getCacheFile(symbol, timeframe)
            if (!file.exists()) return emptyList()
            
            val json = file.readText()
            val type = object : TypeToken<List<OHLCData>>() {}.type
            val ohlcList: List<OHLCData> = gson.fromJson(json, type)
            
            ohlcList.map { ohlc ->
                CandlestickData(
                    time = Time.Utc(ohlc.time),
                    open = ohlc.open,
                    high = ohlc.high,
                    low = ohlc.low,
                    close = ohlc.close
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
