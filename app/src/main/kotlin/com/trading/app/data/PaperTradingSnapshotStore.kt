package com.trading.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class PaperTradingAccountSnapshot(
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val floatingPnl: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val margin: Double = 0.0,
    val freeMargin: Double = 0.0,
    val ordersMargin: Double = 0.0,
    val marginLevel: Double = 100.0,
    val openRisk: Double = 0.0,
    val openRiskPct: Double = 0.0,
    val activeTrades: Int = 0,
    val activeOrders: Int = 0,
    val balanceHistoryCount: Int = 0,
    val lastUpdatedMillis: Long = 0L,
    val isConnected: Boolean = false,
    val hasLiveAccountData: Boolean = false,
    val hasLiveTradeData: Boolean = false,
    val currentTradeSymbol: String? = null,
    val currentTradeSide: String? = null,
    val currentTradeEntryPrice: Double? = null,
    val currentTradeVolume: Double? = null,
    val currentTradePrice: Double? = null,
    val currentTradePriceChange: Double? = null,
    val currentTradePriceChangePct: Double? = null,
    val currentTradePnl: Double? = null,
    val currentTradePnlPct: Double? = null,
    val currentQuoteUpdatedMillis: Long = 0L
)

object PaperTradingSnapshotStore {
    var snapshot by mutableStateOf(PaperTradingAccountSnapshot())
}
