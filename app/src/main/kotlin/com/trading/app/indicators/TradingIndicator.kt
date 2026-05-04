package com.trading.app.indicators

import com.trading.app.models.OHLCData

interface TradingIndicator {
    val id: String
    val name: String
    val color: Int
    
    /**
     * Every indicator takes the OHLC data list and returns its own data points.
     * Returns a list of the same size as the input data, with nulls where values are not yet calculated.
     */
    fun calculate(candles: List<OHLCData>): List<Float?>
}
