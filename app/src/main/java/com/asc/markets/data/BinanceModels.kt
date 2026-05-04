package com.asc.markets.data

import kotlinx.serialization.Serializable

@Serializable
data class BinanceWsRequest(
    val id: String,
    val method: String,
    val params: Map<String, String>? = null
)

@Serializable
data class BinanceWsResponse<T>(
    val id: String?,
    val status: Int,
    val result: T? = null,
    val error: BinanceError? = null
)

@Serializable
data class BinanceError(
    val code: Int,
    val msg: String
)

@Serializable
data class BinanceTicker(
    val symbol: String,
    val price: String,
    val time: Long
)

@Serializable
data class BinanceMiniTicker(
    val e: String,      // Event type
    val E: Long,        // Event time
    val s: String,      // Symbol
    val c: String,      // Close price
    val o: String,      // Open price
    val h: String,      // High price
    val l: String,      // Low price
    val v: String,      // Total traded base asset volume
    val q: String       // Total traded quote asset volume
)

@Serializable
data class MarketTick(
    val ts: Long,
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val last: Double,
    val volume: Double,
    val source: String = "binance"
)

@Serializable
data class BinanceMiniTickerEnvelope(
    val stream: String,
    val data: BinanceMiniTicker
)
