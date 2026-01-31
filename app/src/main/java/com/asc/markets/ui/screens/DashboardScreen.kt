package com.asc.markets.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import com.asc.markets.R
import androidx.compose.ui.unit.em
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.screens.dashboard.*
import com.asc.markets.ui.components.*
import com.asc.markets.ui.components.dashboard.*
import com.asc.markets.ui.theme.*
import com.asc.markets.data.ForexPair
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import kotlin.random.Random
import java.util.Locale
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView

enum class DashboardTab { MARKET_OVERVIEW, TECHNICAL_VITALS, STRATEGY_SIGNALS, ANALYTICAL_QUALITY, EXECUTION_LEDGER, MARKET_PSYCHOLOGY, METHODOLOGY }

@Composable
fun DashboardScreen(viewModel: ForexViewModel) {
    var activeTab by remember { mutableStateOf(DashboardTab.MARKET_OVERVIEW) }
    val selectedPair by viewModel.selectedPair.collectAsState()
    Log.d("ASC", "DashboardScreen composed: activeTab=$activeTab")

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.animation.Crossfade(targetState = activeTab) { tab ->
                    when (tab) {
                        DashboardTab.MARKET_OVERVIEW -> MarketOverviewTab(selectedPair)
                        DashboardTab.TECHNICAL_VITALS -> TechnicalVitalsTab()
                        DashboardTab.STRATEGY_SIGNALS -> StrategySignalsTab()
                        DashboardTab.ANALYTICAL_QUALITY -> AnalyticalQualityTab()
                        DashboardTab.EXECUTION_LEDGER -> ExecutionLedgerTab()
                        DashboardTab.MARKET_PSYCHOLOGY -> MarketPsychologyTab()
                        DashboardTab.METHODOLOGY -> EducationTab()
                    }
                }
            }
        }

        // Floating top menu — transparent parent so it doesn't carry a PureBlack backdrop
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            val context = LocalContext.current
            fun hapticClick() {
                try {
                    val vib = context.getSystemService(Vibrator::class.java)
                    vib?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                } catch (_: Exception) {}
            }

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.wrapContentWidth().height(40.dp)
            ) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    items(DashboardTab.entries.toList()) { tab ->
                        val active = activeTab == tab

                        Surface(
                            color = if (active) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .clickable {
                                    hapticClick()
                                    activeTab = tab
                                }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                val resId = when (tab) {
                                    DashboardTab.MARKET_OVERVIEW -> R.drawable.lucide_line_chart
                                    DashboardTab.TECHNICAL_VITALS -> R.drawable.lucide_activity
                                    DashboardTab.STRATEGY_SIGNALS -> R.drawable.lucide_list_filter
                                    DashboardTab.ANALYTICAL_QUALITY -> R.drawable.lucide_pie_chart
                                    DashboardTab.EXECUTION_LEDGER -> R.drawable.lucide_arrow_left_right
                                    DashboardTab.MARKET_PSYCHOLOGY -> R.drawable.lucide_binary
                                    DashboardTab.METHODOLOGY -> R.drawable.lucide_book_open
                                }

                                Icon(painter = painterResource(id = resId), contentDescription = tab.name, tint = if (active) Color.Black else Color(0xFF64748B), modifier = Modifier.size(21.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when (tab) {
                                        DashboardTab.MARKET_OVERVIEW -> "Market Overview"
                                        DashboardTab.TECHNICAL_VITALS -> "Technical Vitals"
                                        DashboardTab.STRATEGY_SIGNALS -> "Strategy Signals"
                                        DashboardTab.ANALYTICAL_QUALITY -> "Analytical Quality"
                                        DashboardTab.EXECUTION_LEDGER -> "Execution Ledger"
                                        DashboardTab.MARKET_PSYCHOLOGY -> "Market Psychology"
                                        DashboardTab.METHODOLOGY -> "Methodology Knowledge"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (active) Color.Black else Color(0xFF64748B),
                                    letterSpacing = 0.1.em,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LightweightChart(symbol: String, price: Double) {
    // Use a WebView to host Lightweight Charts (standalone CDN build).
    // This keeps the same API (symbol, price) as the previous implementation so callers remain unchanged.
    val points = remember {
        val list = mutableListOf<Double>()
        var p = price
        repeat(200) {
            p += Random.nextDouble(-0.0015, 0.0015)
            list.add(p)
        }
        list
    }

    val jsonData = remember(points) {
        val nowSec = System.currentTimeMillis() / 1000L
        val step = 60 // 1-minute steps
        val sb = StringBuilder()
        sb.append("[")
        points.forEachIndexed { idx, v ->
            val ts = nowSec - (points.size - idx) * step
            sb.append("{\"time\":").append(ts).append(",\"value\":").append(String.format(Locale.US, "%.5f", v)).append("}")
            if (idx != points.lastIndex) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    val html = remember(symbol, jsonData) {
        // Standalone production CDN build ensures LightweightCharts is exposed on window.LightweightCharts
        """
        <!doctype html>
        <html>
        <head>
          <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
          <script src=\"https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js\"></script>
          <style>
            html,body,#chart { height:100%; margin:0; background:#000000; }
          </style>
        </head>
        <body>
          <div id=\"chart\" style=\"width:100%;height:100%\"></div>
          <script>
            const data = %s;
            const chart = LightweightCharts.createChart(document.getElementById('chart'), {
              layout: { background: { color: '#000000' }, textColor: '#C0C0C0' },
              grid: {
                vertLines: { color: 'rgba(255,255,255,0.03)' },
                horzLines: { color: 'rgba(255,255,255,0.03)' }
              },
              rightPriceScale: { visible: true, borderColor: 'rgba(255,255,255,0.06)' },
              timeScale: { borderColor: 'rgba(255,255,255,0.06)' }
            });
            const series = chart.addLineSeries({ color: 'white', lineWidth: 2 });
            series.setData(data);
            window.addEventListener('resize', () => { chart.resize(window.innerWidth, window.innerHeight); });
          </script>
        </body>
        </html>
        """.trimIndent().format(jsonData)
    }

    val context = LocalContext.current
    AndroidView(factory = {
        android.webkit.WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            settings.domStorageEnabled = true
            setBackgroundColor(android.graphics.Color.BLACK)
            webViewClient = android.webkit.WebViewClient()
            // Use loadDataWithBaseURL so relative CDN loads succeed.
            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }, modifier = Modifier.fillMaxSize())
}

@Composable
fun MarketChartsContent(pair: ForexPair) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { ExposureHub() }

        // EURUSD box
        item {
            InfoBox(
                modifier = Modifier.fillMaxWidth().padding(2.dp),
                contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 0.dp)
            ) {
                FullAssetHeader(pair)
            }
        }

        item {
            InfoBox(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), height = 450.dp) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    LightweightChart(pair.symbol, pair.price)
                }
            }
        }

        item { MacroBasketGrid() }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SOR TOP-OF-BOOK LIQUIDITY VENUES", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                VenueMap()
            }
        }

        item { MarketDepthLadder(pair.symbol, pair.price) }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val breakpoint = 720.dp
                if (this.maxWidth >= breakpoint) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(0.58f).padding(4.dp)) { StructuralContext(pair.symbol) }
                        Box(modifier = Modifier.weight(0.42f).padding(4.dp)) { InstitutionalLevelsGrid(pair.price) }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(4.dp)) { StructuralContext(pair.symbol) }
                        Box(modifier = Modifier.fillMaxWidth().padding(4.dp)) { InstitutionalLevelsGrid(pair.price) }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBasketGrid() {
    val macros = listOf(
        Triple("DXY", "104.22", "+0.12%"),
        Triple("US10Y", "4.256%", "-0.02"),
        Triple("ES1!", "5210.45", "+0.23%")
    )

    LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(macros) { mac ->
            Box(modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                MacroCard(mac.first, mac.second, mac.third, Modifier.widthIn(min = 280.dp))
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun MacroCard(symbol: String, price: String, change: String, modifier: Modifier) {
    Log.d("ASC", "MacroCard composed: symbol=$symbol price=$price change=$change")

    InfoBox(modifier = modifier, height = 120.dp) {
        // Tighter inner padding so more content fits horizontally
        Column(modifier = Modifier.fillMaxSize().padding(2.dp), verticalArrangement = Arrangement.SpaceBetween) {
            // Top: icon + writeup
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                val iconBoxColor = Color(0xFF0B1220)
                val iconTint = when {
                    symbol.contains("DXY") -> IndigoAccent
                    symbol.contains("US10Y") -> RoseError
                    symbol.contains("ES1") -> EmeraldSuccess
                    else -> IndigoAccent
                }
                val icon = when {
                    symbol.contains("DXY") -> Icons.Filled.AccountBalance
                    symbol.contains("US10Y") -> Icons.Filled.ShowChart
                    symbol.contains("ES1") -> Icons.Filled.TrendingUp
                    else -> Icons.Filled.AccountBalance
                }

                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBoxColor), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = symbol, tint = iconTint, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(
                        when (symbol) {
                            "DXY" -> "US DOLLAR INDEX"
                            "US10Y" -> "10Y TREASURY YIELD"
                            "ES1!" -> "S&P 500 FUTURES"
                            else -> "MARKET"
                        },
                        color = SlateText,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom row: price and delta inline, pill pinned to the right (price + delta are a single inline group)
            val priceText = if (price.isBlank()) "--" else price
            Row(modifier = Modifier.fillMaxWidth().padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Inline group: price then delta (wrapContent so they never wrap to new lines independently)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
                    Text(
                        text = priceText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.wrapContentWidth(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val isPositive = change.trim().startsWith("+")
                    val baseDeltaColor = when {
                        isPositive -> EmeraldSuccess
                        change.trim().startsWith("-") -> RoseError
                        else -> SlateText
                    }
                    val rawDelta = change.trim().removePrefix("+").removePrefix("-")
                    val deltaArrow = if (isPositive) "▲" else "▼"

                    Text(text = "$deltaArrow $rawDelta", color = baseDeltaColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Spacer pushes the pill to the extreme right, keeping the left group intact
                Spacer(modifier = Modifier.weight(1f))

                // Right: small arrow pill
                PercentagePill(change = change, modifier = Modifier.align(Alignment.CenterVertically), filled = false, showText = false)
            }
        }
    }
}

@Composable
fun PercentagePill(
    change: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    showText: Boolean = true
) {
    val isPositive = change.trim().startsWith("+")
    val baseColor = when {
        change.trim().startsWith("+") -> EmeraldSuccess
        change.trim().startsWith("-") -> RoseError
        else -> SlateText
    }

    // When not filled we want no colored background; the arrow/text itself will be colored
    val targetBg = if (filled) baseColor else Color.Transparent
    val targetTextColor = if (filled) Color.White else baseColor

    val bgColor by animateColorAsState(targetValue = targetBg, animationSpec = tween(durationMillis = 380))
    val textColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(durationMillis = 380))

    Box(
        modifier = modifier
            .height(28.dp)
            .defaultMinSize(minWidth = 28.dp)
            // if bgColor is Transparent this is a no-op visually
            .background(bgColor, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(if (isPositive) "▲" else "▼", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
            if (showText) {
                Text(change, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
