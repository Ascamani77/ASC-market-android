package com.asc.markets.data.trade

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TradeDao {

    @Insert
    suspend fun insertTrade(trade: TradeEntity)

    @Query("SELECT * FROM trade_history ORDER BY timestamp DESC LIMIT 100")
    suspend fun getLast100Trades(): List<TradeEntity>

    @Query("SELECT * FROM trade_history WHERE timestamp >= :fromTime")
    suspend fun getTradesSince(fromTime: Long): List<TradeEntity>

    @Query("SELECT * FROM trade_history ORDER BY pnl ASC LIMIT 5")
    suspend fun getWorst5Trades(): List<TradeEntity>

    @Query("SELECT COUNT(*) FROM trade_history")
    suspend fun getTotalTradeCount(): Int
}
