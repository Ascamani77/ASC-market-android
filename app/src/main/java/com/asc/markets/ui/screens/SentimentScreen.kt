package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.ui.theme.*
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.PairFlags
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Brush
import com.asc.markets.data.FOREX_PAIRS

@Composable
fun SentimentScreen(viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val watchlist by viewModel.watchlistItems.collectAsState()

    LaunchedEffect(scrollState.value) {
        val isAtTop = scrollState.value < 100
        viewModel.setGlobalHeaderVisible(isAtTop)
    }

    // Process data
    val decisions = aiDeployments?.final_decision ?: emptyList()
    val longCount = decisions.count { d ->
        val dir = d.journal_direction?.uppercase(java.util.Locale.US) ?: ""
        dir == "LONG" || dir == "BUY" || dir == "BULLISH"
    }
    val shortCount = decisions.count { d ->
        val dir = d.journal_direction?.uppercase(java.util.Locale.US) ?: ""
        dir == "SHORT" || dir == "SELL" || dir == "BEARISH"
    }
    val totalDirectional = maxOf(1, longCount + shortCount)
    
    val globalConfidence = decisions.mapNotNull { it.journal_score }.average().let { if (it.isNaN()) 0.0 else it } * 100
    val globalSentiment = if (longCount >= shortCount) "BULLISH" else "BEARISH"
    val globalColor = if (globalSentiment == "BULLISH") EmeraldSuccess else RoseError

    val buyPressure = (longCount.toFloat() / totalDirectional) * 100
    val sellPressure = (shortCount.toFloat() / totalDirectional) * 100

    val timestamps = decisions.mapNotNull { it.journal_timestamp }
    val durationHours = if (timestamps.isNotEmpty()) {
        try {
            val oldest = timestamps.minOrNull() ?: ""
            val parsedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(oldest)?.time
            val diff = if (parsedTime != null) System.currentTimeMillis() - parsedTime else 0L
            (diff / (1000 * 60 * 60)).toInt()
        } catch (e: Exception) { 0 }
    } else 0

    val currencies = FOREX_PAIRS
        .filter { it.category == com.asc.markets.data.MarketCategory.FOREX }
        .flatMap { pair ->
            // Extract both currencies from the pair (e.g., "EUR/USD" -> ["EUR", "USD"])
            pair.symbol.split("/").map { it.trim() }
        }
        .distinct()
        .map { ccy ->
            // Calculate sentiment for this currency across all pairs it appears in
            val relatedDecisions = decisions.filter { decision ->
                val asset = decision.asset_1 ?: ""
                // Match currency in asset name (e.g., EUR in EURUSD, EURGBP, etc.)
                asset.contains(ccy, ignoreCase = true)
            }
            
            var score = 0.0
            relatedDecisions.forEach { d ->
                val asset = d.asset_1 ?: ""
                // Determine if currency is base or quote
                val isBase = asset.startsWith(ccy, ignoreCase = true)
                val dir = d.journal_direction?.uppercase(java.util.Locale.US) ?: ""
                val isLong = dir == "LONG" || dir == "BUY" || dir == "BULLISH"
                val isShort = dir == "SHORT" || dir == "SELL" || dir == "BEARISH"
                val s = d.journal_score ?: 0.0
                
                // If base currency: LONG = bullish, SHORT = bearish
                // If quote currency: LONG = bearish, SHORT = bullish
                if (isBase) {
                    score += if (isLong) s else if (isShort) -s else 0.0
                } else {
                    score += if (isLong) -s else if (isShort) s else 0.0
                }
            }
            
            // Average the score
            val avgScore = if (relatedDecisions.isNotEmpty()) score / relatedDecisions.size else 0.0
            val strength = ((avgScore + 1.0) / 2.0 * 5).toFloat().coerceIn(0f, 5f)
            val state = when {
                avgScore > 0.5 -> "Accumulating"
                avgScore > 0.1 -> "Building"
                avgScore < -0.5 -> "Distributing"
                avgScore < -0.1 -> "Weak"
                else -> "Neutral"
            }
            val color = when {
                avgScore > 0.1 -> EmeraldSuccess
                avgScore < -0.1 -> RoseError
                else -> SlateText
            }
            CurrencyRowData(ccy, state, color, avgScore.toFloat(), strength)
        }
        .sortedByDescending { it.score }

    val strongest = currencies.firstOrNull()?.name ?: "EUR"
    val weakest = currencies.lastOrNull()?.name ?: "USD"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("AI SENTIMENT", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Market sentiment decoded by AI", color = SlateText, fontSize = 11.sp)
        }

        // Top Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopCard(
                title = "GLOBAL SENTIMENT",
                value = globalSentiment,
                valueColor = globalColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Confidence", color = SlateText, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${globalConfidence.toInt()}%", color = globalColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(4.dp)
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(globalConfidence.toFloat() / 100f)
                                            .height(4.dp)
                                            .background(globalColor, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Signals", color = SlateText, fontSize = 12.sp)
                            Text("${decisions.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("LONG Signals", color = SlateText, fontSize = 11.sp)
                            Text("$longCount", color = EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("SHORT Signals", color = SlateText, fontSize = 11.sp)
                            Text("$shortCount", color = RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Score", color = SlateText, fontSize = 11.sp)
                            val avgScore = decisions.mapNotNull { it.journal_score }.average().let { if (it.isNaN()) 0.0 else it }
                            Text(String.format("%.2f", avgScore), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TopCard(
                title = "SENTIMENT STATE",
                value = "BUILDING",
                valueColor = Color(0xFFF59E0B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val states = listOf("Neutral", "Building", "Aligned", "Trigger", "Exhausted")
                        val activeIndex = when {
                            durationHours > 6 -> 4
                            globalConfidence > 70 -> 3
                            globalConfidence > 50 -> 2
                            globalConfidence > 30 -> 1
                            else -> 0
                        }
                        states.forEachIndexed { index, state ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (index == activeIndex) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state,
                                    color = if (index == activeIndex) Color(0xFFF59E0B) else SlateText,
                                    fontSize = 10.sp
                                )
                            }
                            if (index < states.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Trend", color = SlateText, fontSize = 11.sp)
                            Text(if (globalSentiment == "BULLISH") "Upward" else "Downward", color = if (globalSentiment == "BULLISH") EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Momentum", color = SlateText, fontSize = 11.sp)
                            val avgDirectionalScore = decisions.mapNotNull { it.directional_score }.average().let { if (it.isNaN()) 0.0 else it }
                            val momentumLabel = when {
                                avgDirectionalScore > 0.7 -> "Strong"
                                avgDirectionalScore > 0.4 -> "Moderate"
                                avgDirectionalScore > 0.0 -> "Weak"
                                else -> "Neutral"
                            }
                            val momentumColor = when {
                                avgDirectionalScore > 0.4 -> EmeraldSuccess
                                avgDirectionalScore > 0.0 -> Color(0xFFF59E0B)
                                else -> SlateText
                            }
                            Text(momentumLabel, color = momentumColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Duration", color = SlateText, fontSize = 11.sp)
                            Text("${durationHours}h", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TopCard(
                title = "MARKET TONE",
                value = "RISK-ON",
                valueColor = EmeraldSuccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Market Environment", color = SlateText, fontSize = 12.sp)
                            val regimeStates = decisions.mapNotNull { it.regime_state }.distinct()
                            val dominantRegime = regimeStates.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Mixed"
                            Text(dominantRegime, color = SlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("VIX Level", color = SlateText, fontSize = 12.sp)
                            val avgVolScore = decisions.mapNotNull { it.feeder_volatility_score }.average().let { if (it.isNaN()) 0.0 else it }
                            val vixEstimate = (avgVolScore * 30).toInt() // Scale to VIX-like range
                            val vixColor = when {
                                vixEstimate > 25 -> RoseError
                                vixEstimate > 15 -> Color(0xFFF59E0B)
                                else -> EmeraldSuccess
                            }
                            Text("~$vixEstimate", color = vixColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("USD Index", color = SlateText, fontSize = 11.sp)
                            val dxyPair = watchlist.firstOrNull { it.assetName.contains("DXY") }
                            val dxyPrice = dxyPair?.price ?: 0.0
                            val dxyText = if (dxyPrice > 0) String.format("%.2f", dxyPrice) else "N/A"
                            val dxyColor = if ((dxyPair?.changePercent ?: 0.0) > 0) EmeraldSuccess else RoseError
                            Text(dxyText, color = dxyColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Bond Yields", color = SlateText, fontSize = 11.sp)
                            val us10y = watchlist.firstOrNull { it.assetName.contains("US10Y") }
                            val yieldText = if ((us10y?.price ?: 0.0) > 0) String.format("%.2f%%", us10y?.price) else "N/A"
                            Text(yieldText, color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Risk Appetite", color = SlateText, fontSize = 11.sp)
                            val riskAppetite = if (longCount > shortCount * 1.5) "High" else if (shortCount > longCount * 1.5) "Low" else "Moderate"
                            val riskColor = when (riskAppetite) {
                                "High" -> EmeraldSuccess
                                "Low" -> RoseError
                                else -> Color(0xFFF59E0B)
                            }
                            Text(riskAppetite, color = riskColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Currency Sentiment Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CURRENCY SENTIMENT", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1D", "1W", "1M", "3M", "1Y").forEach { tf ->
                        Text(
                            text = tf,
                            color = if (tf == "1D") EmeraldSuccess else SlateText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = if (tf == "1D") Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp) else Modifier
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            currencies.forEach { rowData ->
                CurrencyRow(rowData)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("WEAKER", color = RoseError, fontSize = 11.sp)
                Text("NEUTRAL", color = SlateText, fontSize = 11.sp)
                Text("STRONGER", color = EmeraldSuccess, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Middle Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Buy vs Sell Pressure
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BUY vs SELL PRESSURE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    val pressureRatio = if (sellPressure > 0) String.format("%.1f:1", buyPressure / sellPressure) else "∞:1"
                    Text(pressureRatio, color = SlateText, fontSize = 10.sp)
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawArc(
                            color = RoseError,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = EmeraldSuccess,
                            startAngle = -90f,
                            sweepAngle = 360f * (buyPressure / 100f),
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${buyPressure.toInt()}%", color = EmeraldSuccess, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("BUY", color = EmeraldSuccess, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${sellPressure.toInt()}%", color = RoseError, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("SELL", color = RoseError, fontSize = 12.sp)
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Buy pressure is dominant but not yet confirmed", color = EmeraldSuccess, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Net Flow", color = SlateText, fontSize = 10.sp)
                            val netFlow = buyPressure - sellPressure
                            Text("${String.format("%.1f", netFlow)}%", color = if (netFlow > 0) EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Volume", color = SlateText, fontSize = 10.sp)
                            val avgTickCount = decisions.mapNotNull { it.live_tick_count }.average().let { if (it.isNaN()) 0.0 else it }
                            val volumeLabel = when {
                                avgTickCount > 1000 -> "High"
                                avgTickCount > 500 -> "Med"
                                avgTickCount > 0 -> "Low"
                                else -> "N/A"
                            }
                            Text(volumeLabel, color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Strength", color = SlateText, fontSize = 10.sp)
                            val avgConfidence = decisions.mapNotNull { it.direction_confidence }.average().let { if (it.isNaN()) 0.0 else it }
                            val strengthPct = (avgConfidence * 100).toInt()
                            Text("$strengthPct%", color = if (avgConfidence > 0.6) EmeraldSuccess else SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Market Flow
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MARKET FLOW", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("By Category", color = SlateText, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val totalWl = maxOf(1, watchlist.size)
                val flows = listOf(
                    Triple("Forex", "${(watchlist.count { it.category == com.asc.markets.data.MarketCategory.FOREX }.toFloat() / totalWl * 100).toInt()}%", EmeraldSuccess),
                    Triple("Crypto", "${(watchlist.count { it.category == com.asc.markets.data.MarketCategory.CRYPTO }.toFloat() / totalWl * 100).toInt()}%", Color(0xFFF59E0B)),
                    Triple("Stocks", "${(watchlist.count { it.category == com.asc.markets.data.MarketCategory.STOCK }.toFloat() / totalWl * 100).toInt()}%", Color(0xFF8B5CF6)),
                    Triple("Commodities", "${(watchlist.count { it.category == com.asc.markets.data.MarketCategory.COMMODITIES }.toFloat() / totalWl * 100).toInt()}%", SlateText),
                    Triple("Indices", "${(watchlist.count { it.category == com.asc.markets.data.MarketCategory.INDICES }.toFloat() / totalWl * 100).toInt()}%", SlateText)
                )
                flows.forEach { (name, pct, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier
                                .size(12.dp)
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier
                                    .size(6.dp)
                                    .background(color, RoundedCornerShape(3.dp)))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, color = SlateText, fontSize = 13.sp)
                        }
                        Text(pct, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Active Assets", color = SlateText, fontSize = 10.sp)
                        Text("${watchlist.size}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Trending", color = SlateText, fontSize = 10.sp)
                        val trendingCount = decisions.count { (it.trend_state?.contains("TREND") == true) }
                        Text("$trendingCount", color = EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Avg Vol", color = SlateText, fontSize = 10.sp)
                        val avgVolScore = watchlist.map { it.volatilityScore }.average().let { if (it.isNaN()) 0.0 else it }
                        Text("${avgVolScore.toInt()}%", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Dominant Play
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DOMINANT PLAY", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("Currency Pairs", color = SlateText, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, EmeraldSuccess, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(strongest, color = EmeraldSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("↑", color = EmeraldSuccess, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Strongest", color = SlateText, fontSize = 10.sp)
                        }
                        Text("vs", color = SlateText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, RoseError, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(weakest, color = RoseError, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("↓", color = RoseError, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Weakest", color = SlateText, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("$strongest strength vs $weakest weakness", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Watch for $strongest/$weakest long setups", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Spread", color = SlateText, fontSize = 10.sp)
                        val strongestDecision = decisions.firstOrNull { it.asset_1?.contains(strongest) == true }
                        val weakestDecision = decisions.firstOrNull { it.asset_1?.contains(weakest) == true }
                        val spreadScore = ((strongestDecision?.journal_score ?: 0.0) - (weakestDecision?.journal_score ?: 0.0)).coerceIn(-1.0, 1.0)
                        Text(String.format("%.2f", spreadScore), color = if (spreadScore > 0) EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Volatility", color = SlateText, fontSize = 10.sp)
                        val pairVolScore = watchlist.firstOrNull { it.assetName.contains(strongest) && it.assetName.contains(weakest) }?.volatilityScore ?: 0
                        val volLabel = when {
                            pairVolScore > 70 -> "High"
                            pairVolScore > 40 -> "Med"
                            else -> "Low"
                        }
                        Text(volLabel, color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Confidence", color = SlateText, fontSize = 10.sp)
                        val pairConfidence = watchlist.firstOrNull { it.assetName.contains(strongest) && it.assetName.contains(weakest) }?.confidence ?: 0
                        Text("$pairConfidence%", color = if (pairConfidence > 60) EmeraldSuccess else SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AI Interpretation
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI INTERPRETATION", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Text("---", color = SlateText, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val interpretations = if (decisions.isNotEmpty()) {
                    decisions.take(4).mapNotNull { it.portfolio_decision_reason?.split("|")?.firstOrNull()?.trim() }.filter { it.isNotBlank() }.distinct()
                } else {
                    listOf(
                        "Market in accumulation phase",
                        "No breakout yet",
                        "Momentum building but not confirmed",
                        "Waiting for alignment and trigger"
                    )
                }

                interpretations.take(4).forEach { text ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = SlateText, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text, color = SlateText, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Pattern", color = SlateText, fontSize = 10.sp)
                        val chartContexts = decisions.mapNotNull { it.chart_context_label }.distinct()
                        val dominantPattern = chartContexts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key?.take(10) ?: "Mixed"
                        Text(dominantPattern, color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Key Level", color = SlateText, fontSize = 10.sp)
                        val structureLabels = decisions.mapNotNull { it.structure_label }.distinct()
                        val keyLevel = structureLabels.firstOrNull()?.take(10) ?: "N/A"
                        Text(keyLevel, color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Probability", color = SlateText, fontSize = 10.sp)
                        val avgIgnitionProb = decisions.mapNotNull { it.ignition_probability }.average().let { if (it.isNaN()) 0.0 else it }
                        Text("${(avgIgnitionProb * 100).toInt()}%", color = if (avgIgnitionProb > 0.6) EmeraldSuccess else SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sentiment Timeline
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SENTIMENT TIMELINE (1D)", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1H", color = SlateText, fontSize = 10.sp)
                        Text("4H", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("1D", color = SlateText, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NEUTRAL", color = SlateText, fontSize = 10.sp)
                    Text("BUILDING", color = EmeraldSuccess, fontSize = 10.sp)
                    Text("EXPECTED", color = Color(0xFF8B5CF6), fontSize = 10.sp)
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Generate sparkline data from currency sentiment scores
                        val sparklineData = currencies.map { it.score.toDouble() }
                        val normalizedData = if (sparklineData.isNotEmpty()) {
                            val min = sparklineData.minOrNull() ?: -1.0
                            val max = sparklineData.maxOrNull() ?: 1.0
                            val range = (max - min).coerceAtLeast(0.1)
                            sparklineData.map { ((it - min) / range).toFloat() }
                        } else {
                            listOf(0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)
                        }

                        val lineColor = if (globalSentiment == "BULLISH") EmeraldSuccess else RoseError
                        val shadowColor = if (globalSentiment == "BULLISH") EmeraldSuccess else RoseError

                        // Create path for the line
                        val path = Path()
                        val stepX = w / (normalizedData.size - 1).coerceAtLeast(1)
                        normalizedData.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = h * (1f - value * 0.7f - 0.15f) // Scale to fit with padding
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        // Create path for shadow fill (close the loop at bottom)
                        val shadowPath = Path()
                        shadowPath.addPath(path)
                        shadowPath.lineTo(w, h)
                        shadowPath.lineTo(0f, h)
                        shadowPath.close()

                        // Draw thick shadow fill
                        drawPath(
                            path = shadowPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    shadowColor.copy(alpha = 0.6f),
                                    shadowColor.copy(alpha = 0.1f)
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Draw the line on top
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("00:00", color = SlateText, fontSize = 10.sp)
                    Text("06:00", color = SlateText, fontSize = 10.sp)
                    Text("12:00", color = SlateText, fontSize = 10.sp)
                    Text("18:00", color = SlateText, fontSize = 10.sp)
                    Text("24:00", color = SlateText, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Peak", color = SlateText, fontSize = 10.sp)
                        val peakScore = currencies.maxByOrNull { it.score }?.score ?: 0f
                        Text(String.format("%.2f", peakScore), color = if (peakScore > 0) EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Trend", color = SlateText, fontSize = 10.sp)
                        Text(if (globalSentiment == "BULLISH") "Up" else "Down", color = if (globalSentiment == "BULLISH") EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Change", color = SlateText, fontSize = 10.sp)
                        val avgScoreChange = decisions.mapNotNull { it.journal_score }.average().let { if (it.isNaN()) 0.0 else it }
                        val changeText = if (avgScoreChange > 0) "+${String.format("%.2f", avgScoreChange)}" else String.format("%.2f", avgScoreChange)
                        Text(changeText, color = if (avgScoreChange >= 0) EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conflict Detection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFFF59E0B).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", color = Color(0xFFF59E0B), fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("CONFLICT DETECTION", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Mixed signals detected", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("NO TRADE ZONE", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Avoid taking new positions. Wait for alignment.", color = SlateText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val conflictingSignals = decisions.count { d ->
                    val dir = d.journal_direction?.uppercase(java.util.Locale.US) ?: ""
                    val isLong = dir == "LONG" || dir == "BUY" || dir == "BULLISH"
                    val isShort = dir == "SHORT" || dir == "SELL" || dir == "BEARISH"
                    (isLong && d.directional_score ?: 0.0 < 0.3) || (isShort && d.directional_score ?: 0.0 > 0.7)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Conflicts", color = SlateText, fontSize = 10.sp)
                        Text("$conflictingSignals", color = if (conflictingSignals > 0) RoseError else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Severity", color = SlateText, fontSize = 10.sp)
                        val severity = when {
                            conflictingSignals > 5 -> "High"
                            conflictingSignals > 2 -> "Medium"
                            conflictingSignals > 0 -> "Low"
                            else -> "None"
                        }
                        val severityColor = when (severity) {
                            "High" -> RoseError
                            "Medium" -> Color(0xFFF59E0B)
                            "Low" -> EmeraldSuccess
                            else -> SlateText
                        }
                        Text(severity, color = severityColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Resolution", color = SlateText, fontSize = 10.sp)
                        val resolutionHours = if (conflictingSignals > 0) {
                            val timestamps = decisions.mapNotNull { it.journal_timestamp }
                            if (timestamps.isNotEmpty()) {
                                try {
                                    val oldest = timestamps.minOrNull() ?: ""
                                    val parsedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(oldest)?.time
                                    val diff = if (parsedTime != null) System.currentTimeMillis() - parsedTime else 0L
                                    ((diff / (1000 * 60 * 60)) + 4).toInt()
                                } catch (e: Exception) { 4 }
                            } else 4
                        } else 0
                        Text(if (resolutionHours > 0) "${resolutionHours}h" else "N/A", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Volatility Metrics - New Section
        CardContainer(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VOLATILITY METRICS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("---", color = SlateText, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("EURUSD", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val eurusdVol = watchlist.firstOrNull { it.assetName.contains("EURUSD") }?.volatilityScore ?: 0
                    Text("${eurusdVol}%", color = if (eurusdVol > 50) RoseError else if (eurusdVol > 30) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (eurusdVol > 0) "Active" else "N/A", color = SlateText, fontSize = 10.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("GBPUSD", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val gbpusdVol = watchlist.firstOrNull { it.assetName.contains("GBPUSD") }?.volatilityScore ?: 0
                    Text("${gbpusdVol}%", color = if (gbpusdVol > 50) RoseError else if (gbpusdVol > 30) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (gbpusdVol > 0) "Active" else "N/A", color = SlateText, fontSize = 10.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("USDJPY", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val usdjpyVol = watchlist.firstOrNull { it.assetName.contains("USDJPY") }?.volatilityScore ?: 0
                    Text("${usdjpyVol}%", color = if (usdjpyVol > 50) RoseError else if (usdjpyVol > 30) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(if (usdjpyVol > 0) "Active" else "N/A", color = SlateText, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val marketVol = watchlist.map { it.volatilityScore }.average().let { if (it.isNaN()) 0.0 else it }.toInt()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("VIX Index", color = SlateText, fontSize = 10.sp)
                    val avgVolScore = decisions.mapNotNull { it.feeder_volatility_score }.average().let { if (it.isNaN()) 0.0 else it }
                    val vixEstimate = (avgVolScore * 30).toInt()
                    Text("~$vixEstimate", color = if (vixEstimate > 25) RoseError else if (vixEstimate > 15) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Market Vol", color = SlateText, fontSize = 10.sp)
                    Text("$marketVol%", color = if (marketVol > 50) RoseError else if (marketVol > 30) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("ATR (14)", color = SlateText, fontSize = 10.sp)
                    val atrEstimate = (marketVol * 0.01).toInt()
                    Text("$atrEstimate", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Vol Trend", color = SlateText, fontSize = 10.sp)
                    val volTrend = if (marketVol > 40) "Rising" else if (marketVol < 20) "Falling" else "Stable"
                    val trendColor = when (volTrend) {
                        "Rising" -> RoseError
                        "Falling" -> EmeraldSuccess
                        else -> SlateText
                    }
                    Text(volTrend, color = trendColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Key Levels - New Section
        CardContainer(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("KEY LEVELS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("---", color = SlateText, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("EURUSD", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val eurusdPrice = watchlist.firstOrNull { it.assetName.contains("EURUSD") }?.price ?: 0.0
                    val pp = eurusdPrice
                    val r1 = pp + (pp * 0.001)
                    val r2 = pp + (pp * 0.002)
                    val r3 = pp + (pp * 0.003)
                    val s1 = pp - (pp * 0.001)
                    val s2 = pp - (pp * 0.002)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R3:", color = SlateText, fontSize = 10.sp)
                        Text(if (eurusdPrice > 0) String.format("%.5f", r3) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R2:", color = SlateText, fontSize = 10.sp)
                        Text(if (eurusdPrice > 0) String.format("%.5f", r2) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R1:", color = SlateText, fontSize = 10.sp)
                        Text(if (eurusdPrice > 0) String.format("%.5f", r1) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PP:", color = SlateText, fontSize = 10.sp)
                        Text(if (eurusdPrice > 0) String.format("%.5f", pp) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("S1:", color = SlateText, fontSize = 10.sp)
                        Text(if (eurusdPrice > 0) String.format("%.5f", s1) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("S2:", color = SlateText, fontSize = 10.sp)
                        Text(if (eurusdPrice > 0) String.format("%.5f", s2) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("GBPUSD", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val gbpusdPrice = watchlist.firstOrNull { it.assetName.contains("GBPUSD") }?.price ?: 0.0
                    val pp = gbpusdPrice
                    val r1 = pp + (pp * 0.0015)
                    val r2 = pp + (pp * 0.003)
                    val r3 = pp + (pp * 0.0045)
                    val s1 = pp - (pp * 0.0015)
                    val s2 = pp - (pp * 0.003)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R3:", color = SlateText, fontSize = 10.sp)
                        Text(if (gbpusdPrice > 0) String.format("%.5f", r3) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R2:", color = SlateText, fontSize = 10.sp)
                        Text(if (gbpusdPrice > 0) String.format("%.5f", r2) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R1:", color = SlateText, fontSize = 10.sp)
                        Text(if (gbpusdPrice > 0) String.format("%.5f", r1) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PP:", color = SlateText, fontSize = 10.sp)
                        Text(if (gbpusdPrice > 0) String.format("%.5f", pp) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("S1:", color = SlateText, fontSize = 10.sp)
                        Text(if (gbpusdPrice > 0) String.format("%.5f", s1) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("S2:", color = SlateText, fontSize = 10.sp)
                        Text(if (gbpusdPrice > 0) String.format("%.5f", s2) else "---", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TopCard(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CardContainer(modifier = modifier) {
        Text(title, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
        content()
    }
}

@Composable
fun CardContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

data class CurrencyRowData(
    val name: String,
    val state: String,
    val color: Color,
    val score: Float,
    val strength: Float
)

@Composable
fun CurrencyRow(data: CurrencyRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            PairFlags(symbol = data.name, size = 24)
            Spacer(modifier = Modifier.width(12.dp))
            Text(data.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            data.state,
            color = data.color,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            if (data.score > 0) "↑" else if (data.score < 0) "↓" else "−",
            color = data.color,
            fontSize = 18.sp,
            modifier = Modifier.weight(0.3f)
        )

        // Strength bars
        Row(
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..5) {
                val isFilled = i <= data.strength
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            if (isFilled) data.color else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Text(
            (if (data.score > 0) "+" else "") + String.format("%.2f", data.score),
            color = data.color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.End
        )
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Color.White.copy(alpha = 0.05f)))
}
