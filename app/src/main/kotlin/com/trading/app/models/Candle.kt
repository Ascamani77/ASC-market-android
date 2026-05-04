package com.trading.app.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model for Candle data to be passed from Kotlin to JavaScript.
 * We use @SerializedName to ensure compatibility with Lightweight Charts JS API.
 */
data class Candle(
    @SerializedName("time") val time: Long, // Unix timestamp in seconds
    @SerializedName("open") val open: Float,
    @SerializedName("high") val high: Float,
    @SerializedName("low") val low: Float,
    @SerializedName("close") val close: Float
)
