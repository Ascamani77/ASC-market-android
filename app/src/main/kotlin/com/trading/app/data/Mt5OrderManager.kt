package com.trading.app.data

import com.trading.app.models.Order
import com.trading.app.models.Position
import android.util.Log

class Mt5OrderManager(private val mt5Service: Mt5Service) {

    fun placeOrder(order: Order) {
        val params = mapOf(
            "symbol" to order.symbol,
            "type" to order.type.lowercase(), // "buy" or "sell"
            "orderType" to order.orderType.lowercase(), // "market", "limit", "stop"
            "price" to order.price,
            "volume" to order.volume,
            "tp" to (order.tp ?: 0.0),
            "sl" to (order.sl ?: 0.0),
            "comment" to "Mobile App Order"
        )
        mt5Service.sendAction("place_order", params)
    }

    fun placePosition(position: Position) {
        val params = mapOf(
            "symbol" to position.symbol,
            "type" to position.type.lowercase(), // "buy" or "sell"
            "orderType" to "market",
            "price" to position.entryPrice,
            "volume" to position.volume,
            "tp" to (position.tp ?: 0.0),
            "sl" to (position.sl ?: 0.0),
            "comment" to "Mobile App One-Click"
        )
        mt5Service.sendAction("place_order", params)
    }

    fun closePosition(position: Position) {
        val params = mapOf(
            "ticket" to position.id,
            "symbol" to position.symbol,
            "volume" to position.volume
        )
        mt5Service.sendAction("close_position", params)
    }

    fun cancelOrder(order: Order) {
        val params = mapOf(
            "ticket" to order.id
        )
        mt5Service.sendAction("cancel_order", params)
    }

    fun modifyPosition(position: Position, tp: Float?, sl: Float?) {
        val params = mutableMapOf<String, Any>(
            "ticket" to position.id
        )
        tp?.let { params["tp"] = it }
        sl?.let { params["sl"] = it }
        
        mt5Service.sendAction("modify_position", params)
    }
}
