package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText

// Data class representing a single price level in the market depth
data class DepthItem(
    val price: Double,
    val volume: Int,
    val type: String // "ASK", "MID", "BID"
)

/**
 * MarketDepthLadder Component
 * 
 * Displays the market depth ladder showing:
 * - Asks (Sellers) in Red - resistance/supply
 * - Bids (Buyers) in Green - support/demand
 * - Volume at each price level shown as background bars
 * 
 * The component automatically adjusts tick size based on asset type:
 * - Forex (e.g., EUR/USD): 0.0001 tick size
 * - Crypto (e.g., BTC): $50.0 tick size
 */
@Composable
fun MarketDepthLadder(
    selectedPair: ForexPair,
    modifier: Modifier = Modifier
) {
    val currentPrice = selectedPair.price
    
    // Determine tick size based on asset type
    val showMicro = com.asc.markets.ui.components.LocalShowMicrostructure.current
    if (!showMicro) return

    val step = remember(selectedPair.symbol) {
        when {
            selectedPair.symbol.contains("/") -> 0.0001  // Forex: 1 pip
            selectedPair.symbol.contains("BTC") -> 50.0   // Crypto: $50
            selectedPair.symbol.contains("ETH") -> 5.0    // Ethereum: $5
            else -> 1.0                                    // Default: $1
        }
    }
    
    // Generate market depth levels (5 Asks + 1 Mid + 5 Bids = 11 levels)
    val levels = remember(currentPrice, selectedPair.symbol) {
        val list = mutableListOf<DepthItem>()
        
        // Generate Asks (Sellers) - top down
        for (i in 5 downTo 1) {
            val askPrice = currentPrice + (i * step)
            val askVolume = (100..500).random()
            list.add(DepthItem(askPrice, askVolume, "ASK"))
        }
        
        // Mid-market center line
        list.add(DepthItem(currentPrice, 0, "MID"))
        
        // Generate Bids (Buyers) - top down
        for (i in 1..5) {
            val bidPrice = currentPrice - (i * step)
            val bidVolume = (100..500).random()
            list.add(DepthItem(bidPrice, bidVolume, "BID"))
        }
        
        list
    }
    
    val maxVolume = remember(levels) {
        levels.filter { it.type != "MID" }.maxOfOrNull { it.volume }?.toDouble() ?: 500.0
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Text(
            text = "Market Depth - ${selectedPair.symbol}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Depth Ladder
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(levels) { item ->
                DepthLadderRow(
                    depthItem = item,
                    maxVolume = maxVolume,
                    step = step,
                    currentPrice = currentPrice
                )
            }
        }
    }
}

@Composable
private fun DepthLadderRow(
    depthItem: DepthItem,
    maxVolume: Double,
    step: Double,
    currentPrice: Double
) {
    val isAsk = depthItem.type == "ASK"
    val isBid = depthItem.type == "BID"
    val isMid = depthItem.type == "MID"
    
    // Colors
    val textColor = when {
        isAsk -> RoseError
        isBid -> EmeraldSuccess
        else -> Color.White
    }
    
    val backgroundColor = when {
        isAsk -> RoseError.copy(alpha = 0.1f)
        isBid -> EmeraldSuccess.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    
    // Volume percentage for bar width
    val volumePercentage = if (depthItem.volume > 0 && maxVolume > 0) {
        (depthItem.volume / maxVolume).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    
    // Market derived value (simulated as volume / 10)
    val marketValue = depthItem.volume / 10
    
    // Spread indicator for mid-price
    val spreadInfo = if (isMid) {
        val bestAsk = currentPrice + (1 * step)
        val bestBid = currentPrice - (1 * step)
        val spread = bestAsk - bestBid
        val spreadPips = (spread / step).toInt()
        "Spread: $spreadPips pips"
    } else {
        ""
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(vertical = 2.dp)
    ) {
        // Background volume bar
        if (!isMid && depthItem.volume > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(volumePercentage.toFloat())
                    .align(
                        if (isAsk) Alignment.CenterEnd else Alignment.CenterStart
                    )
                    .background(backgroundColor)
            )
        }
        
        // Mid-price divider line
        if (isMid) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.Center)
            )
        }
        
        // Content row with three columns: Volume | Price | Market
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Volume (Left column)
            Text(
                text = depthItem.volume.toString(),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp)
            )
            
            // Price (Center column) - Monospace for alignment
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatPrice(depthItem.price),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                if (isMid && spreadInfo.isNotEmpty()) {
                    Text(
                        text = spreadInfo,
                        color = SlateText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Market participation (Right column)
            Text(
                text = marketValue.toString(),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

/**
 * Format price based on precision
 * - Forex: 4 decimal places
 * - Crypto: 2 decimal places
 * - Stocks/Indices: 2 decimal places
 */
private fun formatPrice(price: Double): String {
    return when {
        price > 10000 -> String.format(java.util.Locale.US, "%.2f", price)  // Large prices
        price > 100 -> String.format(java.util.Locale.US, "%.4f", price)    // Medium prices (Forex-like)
        price > 1 -> String.format(java.util.Locale.US, "%.4f", price)      // Regular prices
        else -> String.format(java.util.Locale.US, "%.8f", price)            // Very small prices (Crypto)
    }
}
