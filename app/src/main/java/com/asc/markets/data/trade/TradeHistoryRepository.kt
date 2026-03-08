package com.asc.markets.data.trade

class TradeHistoryRepository(private val tradeDao: TradeDao) {

    suspend fun saveTrade(trade: TradeEntity) {
        tradeDao.insertTrade(trade)
    }

    suspend fun getLast100Trades(): List<TradeEntity> {
        return tradeDao.getLast100Trades()
    }

    suspend fun getWorst5Trades(): List<TradeEntity> {
        return tradeDao.getWorst5Trades()
    }

    suspend fun getTradesLast6Months(): List<TradeEntity> {
        val sixMonthsAgo = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30 * 6
        return tradeDao.getTradesSince(sixMonthsAgo)
    }
}
