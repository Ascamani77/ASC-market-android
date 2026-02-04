package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

data class NetDeltaData(val currency: String, val bias: String, val delta: Int, val confidence: Int)

@Composable
fun LiquidityHubScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // BOX A: Cross-Asset Correlation Matrix (table layout like image)
        item {
            InfoBox {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Title with icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CROSS-ASSET CORRELATION MATRIX", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("STATISTICAL ALIGNMENT COEFFICIENT ACROSS PRIMARY LIQUIDITY HUBS", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, letterSpacing = 0.5.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Correlation Matrix Table
                    CorrelationMatrixTable()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Alert: Over-Correlation Detected
                    Surface(
                        color = RoseError.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text("⚠", color = RoseError, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("OVER-CORRELATION DETECTED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("EUR/USD AND GBP/USD ARE CURRENTLY MOVING WITH A 0.88 COEFFICIENT. AVOID HOLDING LONG POSITIONS IN BOTH SIMULTANEOUSLY TO PREVENT USD-CONCENTRATION RISK.", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, lineHeight = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Alert: Inverse Signal Opportunity
                    Surface(
                        color = IndigoAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text("⚡", color = IndigoAccent, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("INVERSE SIGNAL OPPORTUNITY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("USD/JPY AND EUR/USD DIVERGENCE DETECTED. INSTITUTIONAL HEDGING SUGGESTS A STRUCTURAL SHIFT IN JAPANESE SESSION LIQUIDITY FLOWS.", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, lineHeight = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // BOX B: Net Currency Delta
        item {
            InfoBox {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Title with icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NET CURRENCY DELTA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AGGREGATED EXPOSURE ACROSS THE SYSTEM BASKET", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, letterSpacing = 0.5.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Currency Delta Rows
                    val currencyData = listOf(
                        NetDeltaData("USD", "SHORT", -452000, 72),
                        NetDeltaData("EUR", "LONG", 284000, 31),
                        NetDeltaData("GBP", "LONG", 142000, 45),
                        NetDeltaData("JPY", "SHORT", -89000, 12),
                        NetDeltaData("AUD", "FLAT", 0, 50)
                    )

                    currencyData.forEach { data ->
                        ExpandedNetDeltaRow(data)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Safety Gate: Arm Dispatch
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SAFETY GATE: ARM DISPATCH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("CALCULATE NET USD RISK", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // BOX C: Volatility Pulse (Risk Gauge)
        item {
            InfoBox {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("VOLATILITY PULSE (VIX PROXY)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Global VIX Proxy display
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Global Fear Index:", color = SlateText, fontSize = 10.sp)
                        Text("23.45", color = EmeraldSuccess, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Equity Beta bar
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Equity Beta", color = SlateText, fontSize = 9.sp)
                            Text("0.62", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.62f)
                                    .background(EmeraldSuccess, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Currency Volatility bar
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Currency Vol", color = SlateText, fontSize = 9.sp)
                            Text("0.38", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.38f)
                                    .background(EmeraldSuccess, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // BOX D: Institutional Disclosure
        item {
            InfoBox {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = SlateText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INSTITUTIONAL DISCLOSURE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Correlations calculated using 1,000 bar rolling window. Net Delta reflects current portfolio aggregation. Volatility bars show 20-day standard deviation. All figures are theoretical approximations.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandedNetDeltaRow(data: NetDeltaData) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Currency, Bias Badge, Delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(data.currency, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val (biasColor, biasTextColor) = when (data.bias) {
                        "LONG" -> EmeraldSuccess to Color.Black
                        "SHORT" -> RoseError to Color.Black
                        else -> Color.White.copy(alpha = 0.4f) to Color.White
                    }
                    
                    Surface(
                        color = biasColor,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .wrapContentSize()
                            .height(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(data.bias, color = biasTextColor, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }

                // Delta amount
                Text(
                    text = "${if (data.delta >= 0) "+" else ""}${String.format("%,d", data.delta)}",
                    color = when (data.bias) {
                        "LONG" -> EmeraldSuccess
                        "SHORT" -> RoseError
                        else -> Color.White
                    },
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Institutional Weighting + Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INSTITUTIONAL WEIGHTING", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("${data.confidence}% CONFIDENCE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val barColor = when (data.bias) {
                "LONG" -> EmeraldSuccess
                "SHORT" -> RoseError
                else -> Color.White.copy(alpha = 0.3f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(data.confidence / 100f)
                        .background(barColor, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun NetDeltaRow(currency: String, delta: Int) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).background(GhostWhite, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text(currency.take(1), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(currency, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
                    Text("BIAS: ${if (delta >= 0) "LONG" else "SHORT"}", color = if (delta >= 0) EmeraldSuccess else RoseError, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "${if (delta >= 0) "+" else ""}${String.format("%,d", delta)}",
                color = if (delta >= 0) EmeraldSuccess else RoseError,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CorrelationMatrixTable() {
    // Correlation matrix data: assets and their correlations
    val assets = listOf("SYM", "EUR", "GBP", "USD", "AUD", "XAU", "BTC")
    val correlations = mapOf(
        "EUR" to listOf(1.00, 0.85, -0.82, 0.49, 0.26, 0.88, -0.73),
        "GBP" to listOf(0.24, 1.00, -0.19, -0.64, 0.91, -0.29, 0.22),
        "USD" to listOf(-0.14, -0.2, 1.00, 0.36, -0.21, 0.55, 0.62),
        "AUD" to listOf(-0.91, 0.42, -0.1, 1.00, -0.35, 0.76, -0.41),
        "XAU" to listOf(0.88, -0.29, -0.76, -0.18, 0.91, 1.00, 0.08),
        "BTC" to listOf(-0.73, 0.22, 0.62, 0.97, -0.8, 0.08, 1.00)
    )

    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            assets.forEachIndexed { index, asset ->
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(asset, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                if (index < assets.size - 1) Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Data rows
        correlations.forEach { (rowAsset, values) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Row header
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rowAsset, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Data cells
                values.forEachIndexed { colIndex, value ->
                    val bgColor = when {
                        value >= 0.99 -> Color.White.copy(alpha = 0.3f) // Diagonal
                        value >= 0.75 -> IndigoAccent.copy(alpha = 0.4f) // High positive
                        value >= 0.4 -> IndigoAccent.copy(alpha = 0.2f) // Medium positive
                        value >= -0.4 -> Color.White.copy(alpha = 0.05f) // Neutral
                        value >= -0.75 -> RoseError.copy(alpha = 0.2f) // Medium negative
                        else -> RoseError.copy(alpha = 0.4f) // High negative
                    }

                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(40.dp)
                            .background(bgColor, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.2f", value),
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    if (colIndex < values.size - 1) Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CorrelationHeatmapCard(pair: String, coeff: Double) {
    val backgroundColor = when {
        coeff > 0.8 -> IndigoAccent.copy(alpha = 0.6f)
        coeff > 0.4 -> IndigoAccent.copy(alpha = 0.25f)
        coeff < -0.8 -> RoseError.copy(alpha = 0.6f)
        coeff < -0.4 -> RoseError.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.size(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(pair, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(
                text = String.format(java.util.Locale.US, "%.2f", coeff),
                color = Color.White,
                fontSize = 16.sp, 
                fontWeight = FontWeight.Black,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}