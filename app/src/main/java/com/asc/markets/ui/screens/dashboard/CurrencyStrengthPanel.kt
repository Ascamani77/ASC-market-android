package com.asc.markets.ui.screens.dashboard

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.remote.LatestDeploymentsResponse
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AccumulationRadarTimeframe
import com.asc.markets.data.DataSource
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.TimedPrice
import com.asc.markets.data.UnifiedMarketDataStore
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import com.asc.markets.state.AssetContext
import com.asc.markets.state.AssetContextStore
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

data class PreMoveAiData(
    val score: Float,
    val phase: String,
    val phasePriority: Int,
    val ignitionProbability: Float,
    val expansionProbability: Float
)

@Composable
private fun InfoIconWithTooltip(
    title: String,
    whatItIs: String,
    whatToLookFor: String,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp
) {
    val showInfo = remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .size(iconSize)
                .clickable { showInfo.value = true }
        )
        DropdownMenu(
            expanded = showInfo.value,
            onDismissRequest = { showInfo.value = false },
            modifier = Modifier
                .background(Color(0xFF0F0F14))
                .border(BorderStroke(1.dp, Color(0xFF1F1F2A)), RoundedCornerShape(8.dp))
                .padding(12.dp)
                .width(280.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Text(
                    text = "WHAT IT IS FOR:",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = whatItIs,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WHAT TO LOOK FOR:",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = whatToLookFor,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

enum class MarketCompareDensity { COMPACT, FULL }
private val BullColor = Color(0xFF43D17A)
private val BearColor = Color(0xFFE34C4C)
private val NeutralColor = Color(0xFFB6BBC6)
private val CardBg = Color.White.copy(alpha = 0.035f)
private val CardBorder = com.asc.markets.ui.theme.HairlineBorder
private val HeaderSize = 10.sp
private val ValueSize = 24.sp

@Composable
fun CurrencyStrengthPanel(density: MarketCompareDensity = MarketCompareDensity.FULL) {
    MarketCompareSection(density = density)
}

@Composable
fun MarketCompareSection(density: MarketCompareDensity = MarketCompareDensity.FULL) {
    val context = LocalContext.current
    val viewModel: ForexViewModel = viewModel()
    val prefs = remember {
        context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val assetContext by AssetContextStore.context.collectAsState()
    
    // Use Unified Market Data Store (EA ONLY - NO FALLBACK)
    val allPairs by UnifiedMarketDataStore.allPairs.collectAsState()
    val priceHistory by UnifiedMarketDataStore.priceHistory.collectAsState()
    val timedPriceHistory by UnifiedMarketDataStore.timedPriceHistory.collectAsState()
    val dataSource by UnifiedMarketDataStore.dataSource.collectAsState()
    val scannerSignals by com.asc.markets.data.ScannerSignalsStore.signals.collectAsState()
    val scannerConnected by com.asc.markets.data.ScannerSignalsStore.isConnected.collectAsState()
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val aiDecisions = aiDeployments?.final_decision ?: emptyList()
    LaunchedEffect(Unit) { com.asc.markets.data.ScannerSignalsStore.start(context) }
    
    val radarTimeframeState = remember {
        mutableStateOf(AccumulationRadarTimeframe.current(context))
    }
    androidx.compose.runtime.DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == AccumulationRadarTimeframe.PREF_KEY) {
                radarTimeframeState.value = AccumulationRadarTimeframe.fromPref(
                    sharedPreferences.getString(AccumulationRadarTimeframe.PREF_KEY, AccumulationRadarTimeframe.HOUR_1.prefValue)
                )
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val radarTimeframe = radarTimeframeState.value

    val scopedPairs = allPairs
        .filter { pair -> pairInContext(pair.category, assetContext) }
    
    // DEBUG: Log filtering steps
    android.util.Log.d("CurrencyStrength", "allPairs count: ${allPairs.size}")
    android.util.Log.d("CurrencyStrength", "After context filter: ${scopedPairs.size}")
    android.util.Log.d("CurrencyStrength", "Sample symbols: ${scopedPairs.take(10).map { it.symbol }}")
    
    val leaders = scopedPairs
        .sortedByDescending { abs(it.changePercent) }
        .take(8)
    
    val accumulationRadarItems = remember(scopedPairs, priceHistory, timedPriceHistory, radarTimeframe, aiDecisions) {
        buildAccumulationRadarItems(scopedPairs, priceHistory, timedPriceHistory, radarTimeframe, aiDecisions)
    }

    val breadth = rememberBreadth(scopedPairs)
    val buySell = rememberBuySellPressure(scopedPairs)
    val perf = rememberPerformanceRows(leaders)
    val positions = rememberPricePositions(leaders, priceHistory)
    val heatSymbols = leaders.take(6)
    val keyDrivers = rememberKeyDrivers(leaders)
    val selectedPair by viewModel.selectedPair.collectAsState()
    val pulseScore = rememberPulseScore(leaders)
    val timingScore = rememberTimingConvergence(leaders)
    val volatilityScore = rememberAssetAtrPercent(selectedPair.symbol, priceHistory, timedPriceHistory)
    val usdStrength = rememberUsdStrength(leaders)

    val panelTitle = when (assetContext) {
        AssetContext.ALL -> "MARKET COMPARE"
        else -> "MARKET COMPARE • ${assetContext.name}"
    }

    InfoBox(
        minHeight = if (density == MarketCompareDensity.FULL) 440.dp else 280.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(panelTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                // Data Source Indicator
                Surface(
                    color = when (dataSource) {
                        DataSource.MT5_EA -> Color(0xFF10B981) // Green
                        DataSource.LOADING -> Color(0xFF6B7280) // Gray
                    }.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, when (dataSource) {
                        DataSource.MT5_EA -> Color(0xFF10B981)
                        DataSource.LOADING -> Color(0xFF6B7280)
                    }.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    when (dataSource) {
                                        DataSource.MT5_EA -> Color(0xFF10B981)
                                        DataSource.LOADING -> Color(0xFF6B7280)
                                    },
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Text(
                            text = when (dataSource) {
                                DataSource.MT5_EA -> "EA LIVE"
                                DataSource.LOADING -> "LOADING"
                            },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Accumulation Radar (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader("ACCUMULATION RADAR (PRE-MOVE)")
                            Spacer(modifier = Modifier.width(6.dp))
                            InfoIconWithTooltip(
                                title = "ACCUMULATION RADAR (PRE-MOVE)",
                                whatItIs = "Shows the top 5 assets with the highest pre-move AI determinism scores. Displays pre-move AI score, current phase, ignition probability, and expansion probability for each asset.",
                                whatToLookFor = "PRE-MOVE phase (orange) = highest priority for entry. COMPRESSION (indigo) = building pressure. EXPANSION (green) = move in progress. Look for high ignition (>60%) and expansion probabilities for the best trade candidates."
                            )
                        }
                        FixedTimeRangeChip(radarTimeframe.displayName)
                    }
                    Spacer8()
                    if (scannerSignals.isNotEmpty()) {
                        // Header matching chart scanner: ASSET DIR CONF P(T) AGE - spread to fill width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ASSET", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Start)
                            Text("DIR", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("CONF", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("P(T)", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("AGE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                        Spacer8()
                        scannerSignals.take(8).forEach { sig ->
                            ScannerRow(sig)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ASSET", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Start)
                            Text("DIR", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("CONF", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("AGE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                        Spacer8()
                        if (accumulationRadarItems.isEmpty()) {
                            Text("Waiting for scanner feed…", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            accumulationRadarItems.forEach { item ->
                                TopMoverRow(item)
                            }
                        }
                    }
                }
            }

            // USD Dispatch Bias (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                val weakest = leaders.minByOrNull { it.changePercent }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. USD Strength Driver
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("USD DISPATCH BIAS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            InfoIconWithTooltip(
                                title = "USD DISPATCH BIAS",
                                whatItIs = "Measures USD strength relative to all other currencies to determine optimal entry bias. Shows whether institutions are accumulating USD (bullish) or distributing it (bearish).",
                                whatToLookFor = "Values > +0.2 = USD accumulation complete, bias dispatch long. Values < -0.2 = USD liquidity sweep, bias dispatch short. Values between -0.2 and +0.2 = neutral accumulation phase, wait for clearer signal."
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StrengthDetailSectionContent(
                        value = usdStrength,
                        description = if (usdStrength >= 0.2f) "USD Accumulation complete. Bias: Dispatch Long" 
                                      else if (usdStrength <= -0.2f) "USD Liquidity sweep detected. Bias: Dispatch Short"
                                      else "USD in neutral accumulation phase"
                    )

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))

                    // 2. Timing Certainty
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TIMING CONVERGENCE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            InfoIconWithTooltip(
                                title = "TIMING CONVERGENCE",
                                whatItIs = "Measures alignment between macro fundamentals and technical price action. Higher convergence means multiple timeframes and indicators are confirming the same directional bias.",
                                whatToLookFor = "Values > +0.5 = strong bullish convergence across macro and technical. Values < -0.5 = strong bearish convergence. Values near 0 = divergence or uncertainty, avoid trades."
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StrengthDetailSectionContent(
                        value = timingScore,
                        description = "Model conviction: EA alignment × journal confidence"
                    )
                }
            }

            // Market Pulse (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MARKET PULSE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            InfoIconWithTooltip(
                                title = "MARKET PULSE",
                                whatItIs = "Real-time sentiment balance between bullish and bearish market participants based on price momentum across all assets. Shows the current directional bias of institutional money flow.",
                                whatToLookFor = "Bulls > 60% = strong bullish sentiment, favor long entries. Bears > 60% = strong bearish sentiment, favor short entries. Balanced 50/50 = choppy market, avoid trades or wait for clearer signal."
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    MarketPulseBar(bulls = pulseScore, bears = 1f - pulseScore)
                }
            }

            // Volatility Dispatch Meter (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader("VOLATILITY — ${selectedPair.symbol} (ATR%)")
                            Spacer(modifier = Modifier.width(6.dp))
                            InfoIconWithTooltip(
                                title = "VOLATILITY DISPATCH METER",
                                whatItIs = "Tracks accumulation-to-expansion cycle. Measures how close the market is to breaking out of accumulation range into high-volatility expansion phase.",
                                whatToLookFor = "Readiness > 72% + EXPANSION IMMINENT = major move about to occur, prepare entries. Readiness 40-72% + ACCUMULATION = coiling phase, monitor closely. Readiness < 40% = low energy, avoid trades."
                            )
                        }
                        Surface(
                            color = if (volatilityScore > 0.72f) RoseError.copy(alpha = 0.12f) else IndigoAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (volatilityScore > 0.72f) RoseError.copy(alpha = 0.3f) else IndigoAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                if (volatilityScore > 0.72f) "EXPANSION IMMINENT" else "ACCUMULATION",
                                color = if (volatilityScore > 0.72f) RoseError else IndigoAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    DispatchSegmentedBar(volatilityScore)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Readiness: ${(volatilityScore * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("System State: ${if (volatilityScore > 0.72f) "Dispatching" else "Coiling"}", color = SlateText, fontSize = 10.sp)
                    }
                }
            }

            if (density == MarketCompareDensity.FULL) {
                // Institutional Liquidity Gaps (Inner InfoBox, Transparent)
                InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SectionHeader("INSTITUTIONAL LIQUIDITY GAPS")
                                Spacer(modifier = Modifier.width(6.dp))
                                InfoIconWithTooltip(
                                    title = "INSTITUTIONAL LIQUIDITY GAPS",
                                    whatItIs = "Tracks price position within the day's trading range. Shows whether price is at highs (open gap), lows (filled), or middle (partial). Used to identify institutional buy/sell zones.",
                                    whatToLookFor = "OPEN GAP (>80%) = price at range top, potential reversal or breakout. PARTIAL (40-80%) = price in mid-range, wait for direction. FILLED (<40%) = price at range bottom, potential bounce or breakdown."
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FILLED", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("PARTIAL", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("OPEN GAP", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        positions.take(5).forEach { row -> 
                            LiquidityGapRow(row) 
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = com.asc.markets.ui.theme.PureBlack,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .background(CardBg)
                .padding(10.dp),
            content = content
        )
    }
}

@Composable
private fun StrengthDetailSectionContent(
    value: Float,
    description: String,
    pair: ForexPair? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Asset info (if weakest currency)
        if (pair != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                PairFlags(symbol = pair.symbol, size = 32)
                Column {
                    Text(pair.symbol.take(3), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(pair.name, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                
                // Labels above slider (right-aligned in image for weakest)
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Weak", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("Neutral", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("Strong", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Labels above slider for USD Strength
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("← Weak", color = BearColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Neutral", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Strong →", color = BullColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Slider
        StrengthSlider(value = value, modifier = Modifier.fillMaxWidth().height(10.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Value and Description
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val color = if (value >= 0) BullColor else BearColor
            val arrow = if (value >= 0) "↑" else "↓↓"
            Text(
                text = String.format(Locale.US, "%+.2f %s", value, arrow),
                color = color,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StrengthSlider(value: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val trackH = h * 0.6f
        val centerY = h / 2f
        
        // Track with gradient
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(BearColor, NeutralColor.copy(alpha = 0.5f), BullColor),
                startX = 0f,
                endX = w
            ),
            size = androidx.compose.ui.geometry.Size(w, trackH),
            topLeft = Offset(0f, centerY - trackH / 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f)
        )

        // Middle marker
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(w / 2f, centerY - trackH),
            end = Offset(w / 2f, centerY + trackH),
            strokeWidth = 1.dp.toPx()
        )

        // Thumb position (-1 to 1 maps to 0 to w)
        val thumbX = ((value + 1f) / 2f * w).coerceIn(0f, w)
        
        // Shadow/Glow for thumb
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = (trackH * 1.5f),
            center = Offset(thumbX, centerY)
        )
        
        // White Thumb
        drawCircle(
            color = Color.White,
            radius = trackH * 0.9f,
            center = Offset(thumbX, centerY)
        )
    }
}

@Composable
private fun TopMoverRow(item: AccumulationRadarItem) {
    val pair = item.pair
    val dirColor = when (item.direction) {
        "BUY", "LONG", "BULLISH" -> EmeraldSuccess
        "SELL", "SHORT", "BEARISH" -> RoseError
        else -> SlateText
    }
    val combinedScore = item.combinedScore
    val confColor = when {
        combinedScore >= 0.7f -> EmeraldSuccess
        combinedScore >= 0.5f -> Color(0xFFF59E0B)
        combinedScore >= 0.25f -> Color(0xFFFFA940)
        else -> Color.Gray
    }
    
    // Age formatting
    val ageText = when {
        item.ageMinutes < 60 -> "${item.ageMinutes}m"
        item.ageMinutes < 1440 -> "${item.ageMinutes / 60}h ${item.ageMinutes % 60}m"
        else -> "${item.ageMinutes / 1440}d"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Asset (symbol + flag)
        Column(modifier = Modifier.width(65.dp)) {
            PairFlags(symbol = pair.symbol, size = 24)
            Text(
                text = pair.symbol,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // DIR
        Column(modifier = Modifier.width(55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = item.direction,
                color = dirColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        // CONF (combined score)
        Column(modifier = Modifier.width(55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (combinedScore.isFinite() && combinedScore > 0) {
                    String.format(Locale.US, "%.0f%%", combinedScore * 100)
                } else {
                    "N/A"
                },
                color = confColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // AGE
        Column(modifier = Modifier.width(55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = ageText,
                color = when {
                    item.ageMinutes < 15 -> EmeraldSuccess
                    item.ageMinutes < 60 -> Color(0xFFF59E0B)
                    else -> SlateText
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ScannerRow(sig: com.asc.markets.data.ScannerSignal) {
    val isLong = sig.direction == "LONG"
    val isShort = sig.direction == "SHORT"
    val hot = (isLong || isShort) && sig.pTrade >= 0.60 && sig.confidence >= 0.55
    val stale = sig.age > 900
    val dirColor = when {
        stale -> Color.Gray
        isLong -> if (hot) Color(0xFF00FF00) else EmeraldSuccess
        isShort -> if (hot) RoseError else Color(0xFFE57373)
        else -> SlateText
    }
    val arrow = when (sig.direction) { "LONG" -> "↑" ; "SHORT" -> "↓" ; else -> "→" }
    val ageText = when {
        sig.age < 60 -> "${sig.age}s"
        sig.age < 3600 -> "${sig.age / 60}m"
        else -> "${sig.age / 3600}h"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sig.asset, color = if (stale) Color.Gray else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
        Text(arrow + " " + sig.direction, color = dirColor, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, textAlign = TextAlign.Center)
        Text(String.format(Locale.US, "%.0f%%", sig.confidence * 100), color = if (stale) Color.Gray else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(if (sig.pTrade >= 0) String.format(Locale.US, "%.0f%%", sig.pTrade * 100) else "—", color = if (stale) Color.Gray else SlateText, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(ageText + if (hot) " *" else "", color = if (stale) Color.Gray else if (sig.age < 120) EmeraldSuccess else SlateText, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}

private fun getDecimalPlaces(price: Double): Int {
    return when {
        price >= 1000 -> 2
        price >= 100 -> 2
        price >= 10 -> 3
        price >= 1 -> 4
        else -> 5
    }
}

@Composable private fun StrengthBarRow(symbol: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(symbol, color = SlateText, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
            Text(String.format("%+.2f", value), color = if (value >= 0f) BullColor else BearColor, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        }
        SegmentedTrack(value = value, positiveColor = BullColor, negativeColor = BearColor, showCenterMarker = true)
        Spacer8()
    }
}

@Composable private fun PerformanceBar(symbol: String, pct: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))) {
            val bar = (abs(normalizeChange(pct)) + 0.05f).coerceIn(0.04f, 1f)
            Box(modifier = Modifier.fillMaxWidth(bar).height(8.dp).background(if (pct >= 0) BullColor else BearColor, RoundedCornerShape(8.dp)))
        }
        Text(
            String.format("  %+.2f%%", pct),
            color = if (pct >= 0) BullColor else BearColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable private fun SplitPressureBar(buy: Float, sell: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(10.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))) {
            Box(modifier = Modifier.fillMaxWidth(max(sell, 0.04f)).height(10.dp).background(BearColor, RoundedCornerShape(10.dp)))
            Box(modifier = Modifier.fillMaxWidth(max(buy, 0.04f)).height(10.dp).background(BullColor, RoundedCornerShape(10.dp)))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(2.dp)
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
            )
        }
        Spacer8()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SELL ${(sell * 100).toInt()}%", color = BearColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("BUY ${(buy * 100).toInt()}%", color = BullColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PositionRow(row: PricePositionRow) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.symbol, color = SlateText, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
            Text("${(row.position * 100).toInt()}%", color = Color.White, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        }
        SegmentedTrack(value = row.position.coerceIn(0.03f, 1f), positiveColor = Color(0xFF8B7CFF), negativeColor = Color(0xFF8B7CFF), showCenterMarker = true)
        Spacer8()
    }
}

@Composable
private fun DispatchSegmentedBar(value: Float) {
    val clamped = value.coerceIn(0f, 1f)
    val segments = 12
    val activeSegments = (clamped * segments).toInt().coerceAtLeast(1)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(segments) { index ->
            val isActive = index < activeSegments
            val color = when {
                !isActive -> Color.White.copy(alpha = 0.08f)
                clamped > 0.8f -> RoseError
                clamped > 0.6f -> Color(0xFFFFA940)
                else -> IndigoAccent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun LiquidityGapRow(row: PricePositionRow) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(row.symbol, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(if (row.position > 0.8f) "OPEN GAP" else if (row.position > 0.4f) "PARTIAL" else "FILLED", 
                 color = if (row.position > 0.8f) RoseError else if (row.position > 0.4f) Color(0xFFFFA940) else EmeraldSuccess, 
                 fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
            val color = if (row.position > 0.8f) RoseError else if (row.position > 0.4f) Color(0xFFFFA940) else EmeraldSuccess
            Box(
                modifier = Modifier
                    .fillMaxWidth(row.position.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
            // Visual markers for institutional levels
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(2) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.3f)))
                }
            }
        }
    }
}

@Composable private fun Spacer8() = Box(modifier = Modifier.height(8.dp))

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = Color.White, fontSize = HeaderSize, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
}

@Composable
private fun SegmentedTrack(
    value: Float,
    positiveColor: Color,
    negativeColor: Color,
    segments: Int = 20,
    showCenterMarker: Boolean = false
) {
    val clamped = value.coerceIn(-1f, 1f)
    val active = (abs(clamped) * segments).toInt().coerceAtLeast(1)
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(segments) { idx ->
                val enabled = idx < active
                val c = when {
                    !enabled -> Color.White.copy(alpha = 0.10f)
                    clamped >= 0f -> positiveColor
                    else -> negativeColor
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(c, RoundedCornerShape(2.dp))
                )
            }
        }
        if (showCenterMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun MeterScale() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("LOW", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("MODERATE", color = Color(0xFFF0C14A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("HIGH", color = Color(0xFFFFA940), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("EXTREME", color = BearColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FixedTimeRangeChip(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MiniSparklineStrip(points: List<Float>, color: Color, modifier: Modifier = Modifier.fillMaxWidth()) {
    val transition = rememberInfiniteTransition(label = "sparkline-tip")
    val tipPulse by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkline-tip-scale"
    )
    val tipAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkline-tip-alpha"
    )
    Canvas(modifier = modifier.height(26.dp)) {
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1)
        val normalizedPoints = points.map { it.coerceIn(0.06f, 0.94f) }
        val linePath = Path().apply {
            moveTo(0f, size.height * (1f - normalizedPoints.first()))
            for (i in 1 until normalizedPoints.size) {
                lineTo(i * step, size.height * (1f - normalizedPoints[i]))
            }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                startY = 0f,
                endY = size.height
            )
        )
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val lastPoint = Offset(size.width, size.height * (1f - normalizedPoints.last()))
        drawCircle(
            color = color.copy(alpha = 0.16f * tipAlpha),
            radius = 7.5f * tipPulse,
            center = lastPoint
        )
        drawCircle(
            color = color.copy(alpha = 0.28f + (0.28f * tipAlpha)),
            radius = 4.4f * tipPulse,
            center = lastPoint
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 1.9f,
            center = lastPoint
        )
    }
}

@Composable
fun MarketPulseBar(bulls: Float, bears: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("BULLS ${(bulls * 100).toInt()}%", color = BullColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("BEARS ${(bears * 100).toInt()}%", color = BearColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp) // Increased height from ~6dp to 12dp
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bulls)
                    .background(BullColor, RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(BearColor, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
            )
        }
    }
}

@Composable
fun MarketPulseWidget(bulls: Float, bears: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MARKET PULSE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BULLS ${(bulls * 100).toInt()}%", color = BullColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("BEARS ${(bears * 100).toInt()}%", color = BearColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp) // Increased height from ~6dp to 12dp
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bulls)
                    .background(BullColor, RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(BearColor, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
            )
        }
    }
}

private fun pairInContext(category: MarketCategory, context: AssetContext): Boolean = when (context) {
    AssetContext.ALL -> true
    AssetContext.FOREX -> category == MarketCategory.FOREX
    AssetContext.CRYPTO -> category == MarketCategory.CRYPTO
    AssetContext.COMMODITIES -> category == MarketCategory.COMMODITIES
    AssetContext.INDICES -> category == MarketCategory.INDICES
    AssetContext.STOCKS -> category == MarketCategory.STOCK
    AssetContext.FUTURES -> category == MarketCategory.FUTURES
    AssetContext.BONDS -> category == MarketCategory.BONDS
}

private data class Breadth(val advancing: Int, val neutral: Int, val declining: Int)
private data class BuySell(val buyPressure: Float, val sellPressure: Float)
private data class PricePositionRow(val symbol: String, val position: Float)
private data class AccumulationRadarItem(
    val pair: ForexPair,
    val changePercent: Double,
    val sparkPoints: List<Float>,
    val accumulationScore: Float,
    val aiScore: Float,
    val combinedScore: Float,
    val direction: String,
    val ageMinutes: Int,
    val preMoveData: PreMoveAiData?
)

private fun rememberBreadth(pairs: List<ForexPair>): Breadth {
    val adv = pairs.count { it.changePercent > 0.03 }
    val dec = pairs.count { it.changePercent < -0.03 }
    return Breadth(adv, (pairs.size - adv - dec).coerceAtLeast(0), dec)
}

private fun rememberBuySellPressure(pairs: List<ForexPair>): BuySell {
    val buy = pairs.filter { it.changePercent > 0.0 }.sumOf { abs(it.changePercent) }
    val sell = pairs.filter { it.changePercent < 0.0 }.sumOf { abs(it.changePercent) }
    val total = (buy + sell).takeIf { it > 0.0 } ?: 1.0
    return BuySell((buy / total).toFloat(), (sell / total).toFloat())
}

private fun rememberPerformanceRows(pairs: List<ForexPair>): List<Pair<String, Double>> {
    return pairs.take(6).map { it.symbol to it.changePercent }
}

private fun rememberPricePositions(
    pairs: List<ForexPair>,
    priceHistory: Map<String, List<Double>>
): List<PricePositionRow> {
    return pairs.map { pair ->
        val hist = priceHistory[pair.symbol].orEmpty()
        if (hist.isEmpty()) return@map PricePositionRow(pair.symbol, 0.5f)
        val low = hist.minOrNull() ?: pair.price
        val high = hist.maxOrNull() ?: pair.price
        val range = (high - low).takeIf { it > 0.0 } ?: 1.0
        PricePositionRow(pair.symbol, ((pair.price - low) / range).toFloat())
    }
}

private fun rememberKeyDrivers(pairs: List<ForexPair>): List<Pair<String, Float>> {
    val avgAbs = pairs.take(6).map { abs(it.changePercent) }.average().toFloat()
    val momentum = (avgAbs / 2.0f).coerceIn(0.05f, 1f)
    val trend = pairs.take(5).count { it.changePercent > 0 }.toFloat() / max(pairs.take(5).size, 1)
    val volatility = pairs.take(6).map { abs(it.change) }.average().toFloat().coerceIn(0f, 1000f) / 1000f
    val volume = (pairs.size.coerceAtMost(12) / 12f).coerceIn(0.1f, 1f)
    return listOf(
        "Trend" to trend,
        "Momentum" to momentum,
        "Volatility" to volatility,
        "Volume" to volume
    )
}

private fun rememberPulseScore(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0f
    val avg = pairs.take(8).map { it.changePercent }.average().toFloat()
    return (avg / 1.5f).coerceIn(-1f, 1f)
}

private fun rememberVolatilityScore(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0.2f
    val avgAbsChange = pairs.take(8).map { abs(it.changePercent) }.average().toFloat()
    return (avgAbsChange / 2.5f).coerceIn(0.08f, 1f)
}

private fun rememberAssetAtrPercent(symbol: String, priceHistory: Map<String, List<Double>>, timedHistory: Map<String, List<TimedPrice>>): Float {
    val hist = timedHistory[symbol]?.map { it.price } ?: priceHistory[symbol] ?: return 0.08f
    if (hist.size < 4) return 0.08f
    val window = hist.takeLast(14)
    // ATR% approx: avg true-range proxy via |close - prevClose| / close, scaled
    var sum = 0.0
    for (i in 1 until window.size) {
        val prev = window[i - 1].takeIf { it.isFinite() && it != 0.0 } ?: continue
        val cur = window[i].takeIf { it.isFinite() } ?: continue
        sum += kotlin.math.abs(cur - prev) / prev
    }
    val atrPct = (sum / (window.size - 1).coerceAtLeast(1)).toFloat() // e.g. 0.001 = 0.1%
    // Map ATR% 0..1.5% to 0..1 meter (forex typical ATR% 0.2-0.8%)
    return (atrPct / 0.015f).coerceIn(0.08f, 1f)
}

private fun rememberUsdStrength(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0f
    val usdPairs = pairs.filter { it.symbol.contains("USD", true) }
    if (usdPairs.isEmpty()) return 0f
    val avg = usdPairs.map { it.changePercent }.average().toFloat()
    return (avg / 1.5f).coerceIn(-1f, 1f)
}

private fun rememberTimingConvergence(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0f
    // Model conviction: EA alignment × journal confidence, signed by avg direction
    val avgAlign = pairs.take(8).mapNotNull { it.alignmentPercentage.takeIf { v -> v.isFinite() } }.average().toFloat().let { if (it.isNaN()) 50f else it }
    val avgScore = pairs.take(8).map { it.eaConfidence }.average().toFloat().let { if (it.isNaN()) 0.5f else it }
    val signal = (avgAlign / 100f * 0.6f + avgScore * 0.4f).coerceIn(0f, 1f) // 0..1 conviction
    val dir = pairs.take(8).map { it.changePercent }.average().toFloat().let { if (it.isNaN()) 0f else it }
    val sign = if (dir >= 0) 1f else -1f
    return (signal * sign).coerceIn(-1f, 1f)
}

private fun buildAccumulationRadarItems(
    pairs: List<ForexPair>,
    priceHistory: Map<String, List<Double>>,
    timedPriceHistory: Map<String, List<TimedPrice>>,
    timeframe: AccumulationRadarTimeframe,
    aiDecisions: List<com.asc.markets.data.remote.FinalDecisionItem> = emptyList()
): List<AccumulationRadarItem> {
    // Remove duplicates by symbol before processing
    val uniquePairs = pairs.distinctBy { it.symbol }
    
    // Build AI decision lookup by normalized symbol
    val aiBySymbol = aiDecisions.associateBy {
        (it.asset_1 ?: "").uppercase(Locale.US).replace("/", "").replace("-", "").replace("_", "").replace(" ", "")
    }

    android.util.Log.d("CurrencyStrength", "Building accumulation radar with ${uniquePairs.size} unique pairs (EA mode)")
    android.util.Log.d("CurrencyStrength", "First 3 pairs: ${uniquePairs.take(3).map { "${it.symbol}(confidence=${it.eaConfidence}, regime=${it.regimeConfidence})" }}")

    val now = System.currentTimeMillis()
    val rankedItems = uniquePairs.map { pair ->
        val sampledPrices = resolveAccumulationPrices(pair, priceHistory, timedPriceHistory, timeframe)
        val sparkPoints = normalizeSparklinePoints(sampledPrices)
        val dayChangePercent = pair.changePercent
        
        // Use EA confidence directly instead of calculated price score
        val eaConfidenceScore = pair.eaConfidence.toFloat()
        
        // Look up AI decision for this symbol
        val normalizedSymbol = pair.symbol.uppercase(Locale.US).replace("/", "").replace("-", "").replace("_", "").replace(" ", "")
        val aiDecision = aiBySymbol[normalizedSymbol]
        val aiScore = normalize01(aiDecision?.journal_score)
        
        // Combined score: average of EA and AI (both 0..1)
        val combinedScore = (eaConfidenceScore + aiScore) / 2f
        
        // Direction: prefer AI direction, fallback to EA direction
        val aiDirection = aiDecision?.journal_direction?.uppercase(Locale.US)
        val eaDirection = pair.eaDirection
        val direction = when {
            aiDirection != null && aiDirection != "WAIT" -> aiDirection
            eaDirection != "WAIT" -> eaDirection
            else -> "WAIT"
        }
        
        // Age: time since signal generation
        val ageMillis = when {
            aiDecision?.journal_timestamp_utc != null && aiDecision.journal_timestamp_utc > 0 -> now - aiDecision.journal_timestamp_utc
            aiDecision?.generated_at != null -> {
                // Try parsing generated_at string
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    sdf.parse(aiDecision.generated_at)?.time ?: 0L
                } catch (_: Exception) { 0L }
            }
            else -> 0L // no timestamp available
        }
        val ageMinutes = (ageMillis / 60000).coerceAtLeast(0).toInt()
        
        android.util.Log.d("CurrencyStrength", "  ${pair.symbol}: EA=${eaConfidenceScore}, AI=${aiScore}, combined=${combinedScore}, dir=${direction}, age=${ageMinutes}m")
        
        AccumulationRadarItem(
            pair = pair,
            changePercent = dayChangePercent,
            sparkPoints = sparkPoints,
            accumulationScore = eaConfidenceScore,
            aiScore = aiScore,
            combinedScore = combinedScore,
            direction = direction,
            ageMinutes = ageMinutes,
            preMoveData = null
        )
    }.sortedByDescending { it.combinedScore }
    
    // Return top 8 by combined score
    val topItems = rankedItems.take(8)
    android.util.Log.d("CurrencyStrength", "Top 8 by combined score: ${topItems.map { "${it.pair.symbol}(combined=${String.format("%.2f", it.combinedScore)}, dir=${it.direction}, age=${it.ageMinutes}m)" }}")
    return topItems
}

private fun resolveAccumulationPrices(
    pair: ForexPair,
    priceHistory: Map<String, List<Double>>,
    timedPriceHistory: Map<String, List<TimedPrice>>,
    timeframe: AccumulationRadarTimeframe
): List<Double> {
    val timedHistory = timedHistoryForSymbol(pair.symbol, timedPriceHistory)
        .filter { it.timestampMillis > 0L && it.price.isFinite() && it.price > 0.0 }
        .sortedBy { it.timestampMillis }

    if (timedHistory.isNotEmpty()) {
        val sampledPrices = sampleTimeframePrices(timedHistory, timeframe)
        if (sampledPrices.size >= 8) {
            return sampledPrices
        }
    }

    val rawHistory = priceHistoryForSymbol(pair.symbol, priceHistory)
        .filter { it.isFinite() && it > 0.0 }
    if (rawHistory.isNotEmpty()) {
        return downsamplePrices(rawHistory, timeframe.bucketCount)
    }

    // Fallback: generate synthetic variation based on current price and change percent
    // This ensures sparkline shows some movement even without historical data
    val basePrice = pair.price
    val changePercent = pair.changePercent / 100.0
    return List(timeframe.bucketCount) { index ->
        val progress = index.toDouble() / (timeframe.bucketCount - 1).coerceAtLeast(1)
        // More dynamic variation with multiple sine waves
        val variation1 = sin(progress * 3.14159 * 2) * (basePrice * 0.005)
        val variation2 = sin(progress * 3.14159 * 4) * (basePrice * 0.003)
        val variation3 = sin(progress * 3.14159 * 6) * (basePrice * 0.002)
        val trend = basePrice * (1.0 + (changePercent * progress))
        (trend + variation1 + variation2 + variation3).coerceAtLeast(basePrice * 0.98).coerceAtMost(basePrice * 1.02)
    }
}

private fun timedHistoryForSymbol(
    symbol: String,
    timedPriceHistory: Map<String, List<TimedPrice>>
): List<TimedPrice> {
    return timedPriceHistory[symbol]
        ?: timedPriceHistory.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, symbol) }?.value
        ?: emptyList()
}

private fun priceHistoryForSymbol(
    symbol: String,
    priceHistory: Map<String, List<Double>>
): List<Double> {
    return priceHistory[symbol]
        ?: priceHistory.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, symbol) }?.value
        ?: emptyList()
}

private fun sampleTimeframePrices(
    history: List<TimedPrice>,
    timeframe: AccumulationRadarTimeframe
): List<Double> {
    if (history.isEmpty()) return emptyList()

    val sortedHistory = history.sortedBy { it.timestampMillis }
    val endTime = sortedHistory.last().timestampMillis
    val startTime = endTime - timeframe.windowMillis
    val window = sortedHistory.filter { it.timestampMillis >= startTime }
    if (window.isEmpty()) return emptyList()

    // Take actual price points instead of interpolating to preserve real variations
    val bucketCount = timeframe.bucketCount
    if (window.size <= bucketCount) {
        return window.map { it.price }
    }

    // Downsample by taking evenly spaced points from the window
    val step = (window.size.toDouble() / bucketCount).toInt().coerceAtLeast(1)
    return List(bucketCount) { index ->
        window[(index * step).coerceAtMost(window.lastIndex)].price
    }
}

private fun downsamplePrices(prices: List<Double>, targetCount: Int): List<Double> {
    if (prices.isEmpty()) return emptyList()
    if (prices.size <= targetCount) return prices

    val lastIndex = prices.lastIndex
    val sampled = List(targetCount) { index ->
        val position = (index.toDouble() / (targetCount - 1).coerceAtLeast(1)) * lastIndex
        prices[position.toInt().coerceIn(0, lastIndex)]
    }
    return sampled
}

private fun smoothPrices(prices: List<Double>): List<Double> {
    if (prices.size < 3) return prices
    return prices.indices.map { index ->
        val fromIndex = (index - 1).coerceAtLeast(0)
        val toIndex = (index + 1).coerceAtMost(prices.lastIndex)
        prices.subList(fromIndex, toIndex + 1).average()
    }
}

private fun interpolatedPriceAt(history: List<TimedPrice>, timestampMillis: Long): Double {
    if (history.isEmpty()) return 0.0
    if (timestampMillis <= history.first().timestampMillis) return history.first().price
    if (timestampMillis >= history.last().timestampMillis) return history.last().price

    for (index in 1 until history.size) {
        val previous = history[index - 1]
        val next = history[index]
        if (timestampMillis <= next.timestampMillis) {
            val duration = (next.timestampMillis - previous.timestampMillis).takeIf { it > 0L } ?: return next.price
            val progress = ((timestampMillis - previous.timestampMillis).toDouble() / duration.toDouble())
                .coerceIn(0.0, 1.0)
            return previous.price + ((next.price - previous.price) * progress)
        }
    }

    return history.last().price
}

private fun normalizeSparklinePoints(prices: List<Double>): List<Float> {
    if (prices.size < 2) return emptyList()
    // Use raw prices instead of smoothed to make sparklines more dynamic
    val minPrice = prices.minOrNull() ?: return emptyList()
    val maxPrice = prices.maxOrNull() ?: return emptyList()
    val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: return List(prices.size) { 0.5f }
    return prices.map { price ->
        (((price - minPrice) / range).toFloat()).coerceIn(0.08f, 0.92f)
    }
}

private fun accumulationRadarScore(
    prices: List<Double>,
    changePercent: Double,
    timeframe: AccumulationRadarTimeframe
): Float {
    if (prices.size < 8) return 0.15f

    val safeLastPrice = prices.lastOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: return 0.15f
    val highPrice = prices.maxOrNull() ?: safeLastPrice
    val lowPrice = prices.minOrNull() ?: safeLastPrice
    val driftLimit = accumulationDriftLimit(timeframe)
    val rangeLimit = accumulationRangeLimit(timeframe)
    val stepLimit = accumulationStepLimit(timeframe)
    val rangePercent = ((highPrice - lowPrice) / safeLastPrice).coerceIn(0.0, rangeLimit)
    val compressionScore = (1.0 - (rangePercent / rangeLimit)).toFloat().coerceIn(0f, 1f)
    val driftScore = (1.0 - (abs(changePercent) / driftLimit)).toFloat().coerceIn(0f, 1f)
    val averageStepPercent = prices.zipWithNext { previous, next ->
        if (previous > 0.0) abs((next - previous) / previous) else 0.0
    }.average()
    val smoothnessScore = (1.0 - (averageStepPercent / stepLimit)).toFloat().coerceIn(0f, 1f)
    val coverageScore = (prices.size / timeframe.bucketCount.toFloat()).coerceIn(0.35f, 1f)

    return (
        (compressionScore * 0.45f) +
            (driftScore * 0.35f) +
            (smoothnessScore * 0.15f) +
            (coverageScore * 0.05f)
        ).coerceIn(0f, 1f)
}

private fun accumulationDriftLimit(timeframe: AccumulationRadarTimeframe): Double = when (timeframe) {
    AccumulationRadarTimeframe.MIN_5 -> 0.35
    AccumulationRadarTimeframe.MIN_15 -> 0.65
    AccumulationRadarTimeframe.MIN_30 -> 0.9
    AccumulationRadarTimeframe.HOUR_1 -> 1.2
    AccumulationRadarTimeframe.HOUR_4 -> 1.8
    AccumulationRadarTimeframe.HOUR_12 -> 2.6
    AccumulationRadarTimeframe.DAY_1 -> 3.5
}

private fun accumulationRangeLimit(timeframe: AccumulationRadarTimeframe): Double = when (timeframe) {
    AccumulationRadarTimeframe.MIN_5 -> 0.01
    AccumulationRadarTimeframe.MIN_15 -> 0.018
    AccumulationRadarTimeframe.MIN_30 -> 0.026
    AccumulationRadarTimeframe.HOUR_1 -> 0.04
    AccumulationRadarTimeframe.HOUR_4 -> 0.07
    AccumulationRadarTimeframe.HOUR_12 -> 0.095
    AccumulationRadarTimeframe.DAY_1 -> 0.12
}

private fun accumulationStepLimit(timeframe: AccumulationRadarTimeframe): Double = when (timeframe) {
    AccumulationRadarTimeframe.MIN_5 -> 0.0025
    AccumulationRadarTimeframe.MIN_15 -> 0.004
    AccumulationRadarTimeframe.MIN_30 -> 0.006
    AccumulationRadarTimeframe.HOUR_1 -> 0.008
    AccumulationRadarTimeframe.HOUR_4 -> 0.011
    AccumulationRadarTimeframe.HOUR_12 -> 0.013
    AccumulationRadarTimeframe.DAY_1 -> 0.015
}

private fun normalizeChange(pct: Double): Float = (pct / 1.5).toFloat().coerceIn(-1f, 1f)

private fun normalize01(value: Double?): Float {
    val raw = value?.toFloat() ?: return 0f
    val normalized = if (raw > 1f) raw / 100f else raw
    return normalized.coerceIn(0f, 1f)
}

private fun sparklineFromScore(score: Float, count: Int = 20): List<Float> {
    val base = 0.5f + (score * 0.25f)
    return List(count) { idx ->
        (base + (sin(idx * 0.55f) * 0.12f) + (score * idx * 0.005f)).coerceIn(0.04f, 0.96f)
    }
}
