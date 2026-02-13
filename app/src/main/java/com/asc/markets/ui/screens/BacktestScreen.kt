package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.focusable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.BacktestEngine
import com.asc.markets.logic.BacktestParams
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.HorizontalDivider
import com.asc.markets.ui.components.ShimmerPlaceholder
import com.asc.markets.ui.components.SkeletonColumn
import com.asc.markets.ui.components.TooltipIcon
import com.asc.markets.ui.components.responsivePadding
import kotlinx.coroutines.launch

@Composable
fun BacktestScreen(viewModel: ForexViewModel) {
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var fastMa by remember { mutableStateOf(50) }
    var slowMa by remember { mutableStateOf(200) }
    var rsiPeriod by remember { mutableStateOf(14) }
    var rsiLow by remember { mutableStateOf(30) }
    var rsiHigh by remember { mutableStateOf(70) }
    var timeframe by remember { mutableStateOf("H1") }
    var pair by remember { mutableStateOf("EUR/USD") }

    var running by remember { mutableStateOf(false) }
    val terminalLogs = remember { mutableStateListOf<String>() }
    var result by remember { mutableStateOf<com.asc.markets.logic.BacktestResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = PureBlack,
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0f0f10),
                            modifier = Modifier.size(44.dp),
                            tonalElevation = 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("STRATEGY", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("SIMULATION", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Text(
                        "ANALYTICAL LOOKBACK: 5000\nDYNAMIC BARS",
                        color = SlateText,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }

                // Pair selector (styled) - grouped dropdown like AlertsScreen
                val forex = listOf("EUR/USD","GBP/USD","USD/JPY","AUD/USD","USD/CAD","USD/CHF","NZD/USD","EUR/GBP","EUR/JPY","GBP/JPY")
                val stocks = listOf("AAPL","MSFT","GOOGL","AMZN","TSLA")
                val indices = listOf("SPX/500","NAS100","DOW30")
                val commodities = listOf("XAU/USD","XAG/USD","WTI")
                val crypto = listOf("BTC/USD","ETH/USD","BNB/USD")
                val grouped = listOf(
                    "Forex" to forex,
                    "Stocks" to stocks,
                    "Indices" to indices,
                    "Commodities" to commodities,
                    "Crypto" to crypto
                )

                var assetMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(52.dp).clickable { assetMenuExpanded = true },
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pair, color = Color.White, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                        }
                    }

                    DropdownMenu(expanded = assetMenuExpanded, onDismissRequest = { assetMenuExpanded = false }, modifier = Modifier.fillMaxWidth(0.95f)) {
                        val scroll = rememberScrollState()
                        Column(modifier = Modifier.heightIn(max = 340.dp).verticalScroll(scroll)) {
                            grouped.forEach { (group, items) ->
                                Text(group.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(12.dp, 8.dp))
                                items.forEach { item ->
                                    DropdownMenuItem(text = {
                                        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(36.dp)) {
                                                Box(contentAlignment = Alignment.Center) { Text(item.takeWhile { it != '/' }.take(1), color = Color.White, fontWeight = FontWeight.Black) }
                                            }
                                            Text(item, color = Color.White, modifier = Modifier.padding(start = 12.dp))
                                        }
                                    }, onClick = {
                                        pair = item
                                        assetMenuExpanded = false
                                        viewModel.selectPairBySymbolNoNavigate(item)
                                    })
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                            }
                        }
                    }
                }

                // Calibration desk label
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("INSTITUTIONAL CALIBRATION DESK", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)

                    Text("MOVING AVERAGE ENGINE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)

                    // MA sliders
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("FAST PERIOD", color = SlateText, fontSize = 10.sp)
                            Text("$fastMa", color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Slider(value = fastMa.toFloat(), onValueChange = { fastMa = it.toInt() }, valueRange = 2f..200f, colors = SliderDefaults.colors(activeTrackColor = IndigoAccent))

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("SLOW PERIOD", color = SlateText, fontSize = 10.sp)
                            Text("$slowMa", color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Slider(value = slowMa.toFloat(), onValueChange = { slowMa = it.toInt() }, valueRange = 6f..400f, colors = SliderDefaults.colors(activeTrackColor = IndigoAccent))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("RSI OSCILLATOR LOGIC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("LOOKBACK (PERIOD)", color = SlateText, fontSize = 10.sp)
                            Text("$rsiPeriod", color = Color.White, fontWeight = FontWeight.Black)
                        }
                        Slider(value = rsiPeriod.toFloat(), onValueChange = { rsiPeriod = it.toInt() }, valueRange = 6f..30f, colors = SliderDefaults.colors(activeTrackColor = IndigoAccent))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OB BOUND", color = SlateText, fontSize = 10.sp)
                                Slider(value = rsiHigh.toFloat(), onValueChange = { rsiHigh = it.toInt() }, valueRange = 50f..90f, colors = SliderDefaults.colors(activeTrackColor = EmeraldSuccess))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OS BOUND", color = SlateText, fontSize = 10.sp)
                                Slider(value = rsiLow.toFloat(), onValueChange = { rsiLow = it.toInt() }, valueRange = 10f..50f, colors = SliderDefaults.colors(activeTrackColor = RoseError))
                            }
                        }
                    }
                }

                // Environment controls / timeframe pills
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ENVIRONMENT CONTROLS", color = SlateText, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("M15","H1","H4","D1").forEach { tf ->
                            Surface(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clickable { timeframe = tf },
                                color = if (timeframe==tf) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(tf, color = if (timeframe==tf) Color.Black else Color.White)
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "LOOKBACK CONFIGURED FOR 5,000 ALGORITHMIC CYCLES BASED ON CURRENT TIMEFRAME RESOLUTION",
                                color = SlateText,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Run button
                Button(
                    onClick = {
                        if (!running) {
                            running = true
                            terminalLogs.clear()
                            result = null
                            scope.launch {
                                val params = BacktestParams(fastMa, slowMa, rsiPeriod, rsiLow, rsiHigh, timeframe, pair)
                                try {
                                    val res = BacktestEngine.runBacktest(params) { line -> terminalLogs.add(line) }
                                    result = res
                                } catch (_: Throwable) {
                                    terminalLogs.add("[Sim_Engine_Node_L14] ERROR: interrupted")
                                }
                                running = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).focusable().semantics { contentDescription = "Initiate audit" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (running) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("RUNNING SIMULATION", color = Color.White, fontWeight = FontWeight.Black)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIATE AUDIT", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preview writeup shown before initiating audit
        if (!running && result == null) {
            // compute lightweight preview metrics (deterministic, no randomness)
            val basePreview = (fastMa + slowMa) % 50
            val winRatePreview = (40.0 + (rsiHigh - rsiLow) * 0.4 + (basePreview % 10)).coerceIn(10.0, 95.0)
            val profitFactorPreview = (1.1 + (slowMa - fastMa) * 0.02).coerceIn(0.3, 5.0)
            val sharpePreview = (0.2 + (rsiHigh - rsiLow) * 0.01).coerceIn(-1.0, 3.5)
            val recoveryPreview = (0.5 + profitFactorPreview * 0.4).coerceIn(0.2, 6.0)
            val sessionsPreview = mutableListOf<String>().apply {
                if (timeframe in listOf("M15", "M30", "H1")) add("London")
                if (timeframe in listOf("H1", "H4", "D1")) add("New York")
                if (isEmpty()) add("London")
            }

            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), color = PureBlack, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = SlateText, modifier = Modifier.size(48.dp))
                    Text("READY FOR STRATEGIC SIMULATION", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("SELECT ASSET AND CALIBRATION TO BEGIN HISTORICAL AUDIT.", color = SlateText, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    // short sample audit writeup
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("Institutional Audit & Strategy Analysis", color = SlateText, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Strategy Overview: The simulation uses a Moving Average Crossover (${fastMa}/${slowMa}) on the $timeframe timeframe filtered by RSI($rsiPeriod) to avoid extreme entries.", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Win Rate", color = SlateText, fontSize = 11.sp)
                                Text(String.format("%.1f%%", winRatePreview), color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Profit Factor", color = SlateText, fontSize = 11.sp)
                                Text(String.format("%.2f", profitFactorPreview), color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Institutional Alignment: Majority of attributed sessions — ${sessionsPreview.joinToString(", ")}.", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Conclusion: The strategy tends to perform in trending regimes; preview metrics suggest ${if (profitFactorPreview>1.0) "positive expectancy" else "weak expectancy"}.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Running status card (shows while simulation runs)
        if (running) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                color = PureBlack,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(EmeraldSuccess, RoundedCornerShape(4.dp))) {}
                        Text("SIM_ENGINE_NODE_L14", color = Color(0xFF2EF28C), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                    Text(
                        "APPLYING CALIBRATION: MA($fastMa/$slowMa) RSI($rsiPeriod [$rsiLow/$rsiHigh]) @ $timeframe...",
                        color = Color(0xFF2EF28C),
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        // Terminal log window
        if (terminalLogs.isNotEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), color = Color.Black, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    terminalLogs.forEach { line ->
                        Text(line, color = Color(0xFF2EF28C), fontFamily = InterFontFamily, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Staged content reveal: shimmer placeholders while async panels load
            if (running && result == null) {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), color = PureBlack, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                    Column(modifier = Modifier.padding(responsivePadding()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // verdict skeleton
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)))

                        // bias skeleton
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(8.dp)))

                        // metrics row skeletons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShimmerPlaceholder(modifier = Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(8.dp)))
                            ShimmerPlaceholder(modifier = Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(8.dp)))
                        }

                        // Efficiency Matrix & Session Liquidity placeholders
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)))
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)))

                        // rationale skeleton
                        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Final structured result rendering (with dominant bias panel)
        result?.let { r ->
            // derive a dominant bias label from structured metrics
            val dominantBias = when {
                r.verdict == "BUY" -> "BULLISH"
                r.verdict == "SELL" -> "BEARISH"
                r.winRate >= 60.0 || r.profitFactor >= 1.5 -> "BULLISH"
                r.winRate <= 35.0 || r.profitFactor <= 0.8 -> "BEARISH"
                else -> "MIXED"
            }

            val biasColor = when (dominantBias) {
                "BULLISH" -> EmeraldSuccess
                "BEARISH" -> RoseError
                else -> Color(0xFFFFA000)
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = PureBlack, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // header row with small labels + icon
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFB300), modifier = Modifier.size(48.dp), tonalElevation = 0.dp) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Black) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("BACKTEST", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("STRATEGY VERDICT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // large verdict text centered
                    Text(
                        r.verdict,
                        color = Color(0xFFFFC107),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // dominant bias panel
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.widthIn(min = 220.dp).heightIn(min = 72.dp), color = Color(0xFF0F1113), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DOMINANT BIAS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(dominantBias, color = biasColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // metrics grid (compact)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(modifier = Modifier.weight(1f), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Win Rate", color = SlateText)
                                }
                                Text("${r.winRate}%", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                        Surface(modifier = Modifier.weight(1f), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Profit Factor", color = SlateText)
                                }
                                Text("${r.profitFactor}", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(modifier = Modifier.weight(1f), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BarChart, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sharpe", color = SlateText)
                                }
                                Text("${r.sharpe}", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                        Surface(modifier = Modifier.weight(1f), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Recovery", color = SlateText)
                                }
                                Text("${r.recoveryRatio}", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // --- Efficiency Matrix & Session Liquidity (two boxed panels) ---
                    Surface(modifier = Modifier.fillMaxWidth(), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("EFFICIENCY MATRIX", color = SlateText, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                TooltipIcon("Sharpe and Recovery metrics are risk-adjusted and show distributional performance versus drawdown. Click for more.")
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // left column (two rows of metrics)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text("SHARPE RATIO", color = SlateText, fontSize = 10.sp); Text("${r.sharpe}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
                                        Column { Text("RECOVERY FACTOR", color = SlateText, fontSize = 10.sp); Text("${r.recoveryRatio}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text("AVG DURATION", color = SlateText, fontSize = 10.sp); Text("6h 45m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                                        Column { Text("BEST DISPATCH", color = SlateText, fontSize = 10.sp); Text("+142.5 pips", color = EmeraldSuccess, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text("WORST DISPATCH", color = SlateText, fontSize = 10.sp); Text("-45.2 pips", color = RoseError, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                                        Column { Text("TOTAL SIGNALS", color = SlateText, fontSize = 10.sp); Text("342", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(modifier = Modifier.fillMaxWidth(), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("SESSION LIQUIDITY", color = SlateText, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(12.dp))

                            // London Hub
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("LONDON HUB", color = SlateText, modifier = Modifier.weight(1f))
                                Text("45%", color = Color.White)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF0B0B0C), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(0.45f).height(8.dp).background(Color.White, RoundedCornerShape(4.dp))) {}
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // New York Hub
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("NEWYORK HUB", color = SlateText, modifier = Modifier.weight(1f))
                                Text("35%", color = Color.White)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF0B0B0C), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(0.35f).height(8.dp).background(Color.White, RoundedCornerShape(4.dp))) {}
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Tokyo Hub
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("TOKYO HUB", color = SlateText, modifier = Modifier.weight(1f))
                                Text("20%", color = Color.White)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF0B0B0C), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(0.20f).height(8.dp).background(Color.White, RoundedCornerShape(4.dp))) {}
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Australia Hub
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("AUSTRALIA HUB", color = SlateText, modifier = Modifier.weight(1f))
                                Text("10%", color = Color.White)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF0B0B0C), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxWidth(0.10f).height(8.dp).background(Color.White, RoundedCornerShape(4.dp))) {}
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(modifier = Modifier.fillMaxWidth(), color = PureBlack, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("INSTITUTIONAL AUDIT - CLINICAL RATIONALE", color = SlateText, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(r.rationale, color = Color.Gray, lineHeight = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}