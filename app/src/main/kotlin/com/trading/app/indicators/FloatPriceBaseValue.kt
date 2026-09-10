package com.trading.app.indicators

import com.tradingview.lightweightcharts.api.series.models.BaseValueType

/**
 * The SDK's BaseValuePrice only accepts Int prices, which destroys zone geometry
 * on sub-10 symbols (EURUSD 1.08 -> round().toInt() == 1). Gson serializes options
 * reflectively (no adapter is registered for BaseValueType), so this Double-priced
 * implementation emits {"price": <double>, "type": "PRICE"} - the exact shape the
 * JS side already consumes from the working high-price path.
 */
class FloatPriceBaseValue(
    val price: Double,
    val type: String = "PRICE"
) : BaseValueType
