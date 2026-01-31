package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import java.util.Locale

// Small helper to ensure we never pass zero (or negative) to weight APIs which require > 0
private fun safeWeight(w: Float): Float = if (w <= 0f) 0.0001f else w

@Composable
fun MarketDepthLadder(symbol: String, price: Double) {
    // Generate simulated DOM with 5 asks and 5 bids using provided price
    val isCrypto = symbol.contains("BTC") || symbol.contains("ETH")
    val step = if (isCrypto) 50.0 else 0.0001

    val asks = remember { List(5) { kotlin.random.Random.nextInt(50, 400).toFloat() } }
    val bids = remember { List(5) { kotlin.random.Random.nextInt(50, 400).toFloat() } }
    val maxVol = maxOf(asks.maxOrNull() ?: 1f, bids.maxOrNull() ?: 1f)

    // cumulative sizes
    val asksCumulative = asks.runningFold(0f) { acc, v -> acc + v }.drop(1)
    val bidsCumulative = bids.runningFold(0f) { acc, v -> acc + v }.drop(1)

    InfoBox(modifier = Modifier.fillMaxWidth(), minHeight = 320.dp) {
        // reduced internal padding to keep cards compact but readable (8.dp)
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text("MARKET DEPTH (L2)", color = SlateText, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(6.dp))

            // header row — alignments now explicitly match the data row columns
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(safeWeight(0.25f)), contentAlignment = Alignment.CenterStart) { Text("VOL", color = SlateText, fontSize = 10.sp) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.06f))) {}
                Box(modifier = Modifier.weight(safeWeight(0.5f)), contentAlignment = Alignment.Center) { Text("PRICE", color = SlateText, fontSize = 10.sp) }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.06f))) {}
                Box(modifier = Modifier.weight(safeWeight(0.25f)), contentAlignment = Alignment.CenterEnd) { Text("SIZE", color = SlateText, fontSize = 10.sp) }
            }

            // Asks (sell side) - show from highest price down
            asks.forEachIndexed { i, vol ->
                val weight = (vol / maxVol).coerceIn(0.02f, 0.98f)
                val safeBar = safeWeight(weight)
                val priceStr = String.format(Locale.US, "%.5f", price + ((5 - i) * step))
                val volStr = vol.toInt().toString()
                val sizeStr = asksCumulative.getOrNull(i)?.toInt()?.toString() ?: volStr
                DepthRowTable(price = priceStr, vol = volStr, size = sizeStr, weight = safeBar, isAsk = true)
            }

            // Current price separator (centered)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                    Text(String.format(Locale.US, "%.5f", price), color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
            Spacer(modifier = Modifier.height(6.dp))

            // Bids (buy side) - show from highest bid downward
            bids.forEachIndexed { i, vol ->
                val weight = (vol / maxVol).coerceIn(0.02f, 0.98f)
                val safeBar = safeWeight(weight)
                val priceStr = String.format(Locale.US, "%.5f", price - ((i + 1) * step))
                val volStr = vol.toInt().toString()
                val sizeStr = bidsCumulative.getOrNull(i)?.toInt()?.toString() ?: volStr
                DepthRowTable(price = priceStr, vol = volStr, size = sizeStr, weight = safeBar, isAsk = false)
            }
        }
    }
}

// Simple DepthRowTable implementation used by MarketDepthLadder.
@Composable
private fun DepthRowTable(price: String, vol: String, size: String, weight: Float, isAsk: Boolean) {
    val bgColor = if (isAsk) RoseError.copy(alpha = 0.35f) else EmeraldSuccess.copy(alpha = 0.35f)
    val priceColor = if (isAsk) RoseError else EmeraldSuccess

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume (left) — left aligned and uses same weight as header (0.25)
        Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterStart) {
            Text(vol, color = SlateText, fontSize = 11.sp)
        }

        // separator
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.04f))) {}

        // Price + depth bar center area — matches header weight 0.5
        Box(modifier = Modifier.weight(0.5f)) {
            // Track full width, place a directional bar inside
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(if (isAsk) Alignment.CenterEnd else Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(weight)
                        .background(bgColor)
                ) {}
            }
            // price text overlay centered
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(price, color = priceColor, fontSize = 11.sp)
            }
        }

        // separator
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.04f))) {}

        // Size (right) — right aligned and uses same weight as header (0.25)
        Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterEnd) {
            Text(size, color = SlateText, fontSize = 11.sp)
        }
    }
}