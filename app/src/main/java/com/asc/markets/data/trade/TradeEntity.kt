package com.asc.markets.data.trade

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_history")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val asset: String,
    val regimeStack: String,
    val direction: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnl: Double,
    val win: Boolean,

    val entryVolatility: Double,
    val entryCorrelation: Double,

    val timestamp: Long
)
