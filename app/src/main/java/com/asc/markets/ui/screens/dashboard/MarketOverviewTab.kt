package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.ASCSignalData
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.PriceStreamManager
import com.trading.app.components.AssetIcon
import com.trading.app.models.SymbolInfo
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import java.util.Locale

private val CATEGORY_ORDER = listOf(
    MarketCategory.COMMODITIES,
    MarketCategory.CRYPTO,
    MarketCategory.FOREX,
    MarketCategory.INDICES,
    MarketCategory.STOCK,
    MarketCategory.BONDS,
    MarketCategory.FUTURES
)

// Only assets with trained ML models — matches ml_models/regime_*_model.joblib
data class TrainedAsset(val symbol: String, val name: String, val category: MarketCategory)

// The complete set of assets that have trained ML models (43). This is the canonical list shown
// on the Markets page regardless of the saved watchlist or live MT5 feed.
private val TRAINED_ASSETS: List<TrainedAsset> = listOf(
    // COMMODITIES (5)
    TrainedAsset("UKOIL", "UK Brent Oil", MarketCategory.COMMODITIES),
    TrainedAsset("USOIL", "WTI Crude Oil", MarketCategory.COMMODITIES),
    TrainedAsset("XAGUSD", "Silver / US Dollar", MarketCategory.COMMODITIES),
    TrainedAsset("XAUUSD", "Gold / US Dollar", MarketCategory.COMMODITIES),
    TrainedAsset("XCUUSD", "Copper / US Dollar", MarketCategory.COMMODITIES),

    // CRYPTO (8)
    TrainedAsset("BTCCNH", "Bitcoin / CNH", MarketCategory.CRYPTO),
    TrainedAsset("BTCUSD", "Bitcoin / US Dollar", MarketCategory.CRYPTO),
    TrainedAsset("BTCUSDT", "Bitcoin / Tether", MarketCategory.CRYPTO),
    TrainedAsset("BTCXAG", "Bitcoin / Silver", MarketCategory.CRYPTO),
    TrainedAsset("BTCXAU", "Bitcoin / Gold", MarketCategory.CRYPTO),
    TrainedAsset("ETHBTC", "Ethereum / Bitcoin", MarketCategory.CRYPTO),
    TrainedAsset("ETHUSD", "Ethereum / US Dollar", MarketCategory.CRYPTO),
    TrainedAsset("ETHUSDT", "Ethereum / Tether", MarketCategory.CRYPTO),

    // FOREX (14)
    TrainedAsset("AUDJPY", "Aussie / Yen", MarketCategory.FOREX),
    TrainedAsset("AUDUSD", "Aussie / US Dollar", MarketCategory.FOREX),
    TrainedAsset("EURCAD", "Euro / Canadian Dollar", MarketCategory.FOREX),
    TrainedAsset("EURCHF", "Euro / Swiss Franc", MarketCategory.FOREX),
    TrainedAsset("EURGBP", "Euro / British Pound", MarketCategory.FOREX),
    TrainedAsset("EURJPY", "Euro / Japanese Yen", MarketCategory.FOREX),
    TrainedAsset("EURUSD", "Euro / US Dollar", MarketCategory.FOREX),
    TrainedAsset("GBPJPY", "British Pound / Yen", MarketCategory.FOREX),
    TrainedAsset("GBPUSD", "British Pound / US Dollar", MarketCategory.FOREX),
    TrainedAsset("NZDUSD", "Kiwi / US Dollar", MarketCategory.FOREX),
    TrainedAsset("USDCAD", "US Dollar / Canadian Dollar", MarketCategory.FOREX),
    TrainedAsset("USDCHF", "US Dollar / Swiss Franc", MarketCategory.FOREX),
    TrainedAsset("USDCNH", "US Dollar / Chinese Yuan", MarketCategory.FOREX),
    TrainedAsset("USDJPY", "US Dollar / Japanese Yen", MarketCategory.FOREX),

    // INDICES (8)
    TrainedAsset("DE30", "Germany DAX 30", MarketCategory.INDICES),
    TrainedAsset("DXY", "US Dollar Index", MarketCategory.INDICES),
    TrainedAsset("JP225", "Japan Nikkei 225", MarketCategory.INDICES),
    TrainedAsset("STOXX50", "Euro Stoxx 50", MarketCategory.INDICES),
    TrainedAsset("UK100", "UK FTSE 100", MarketCategory.INDICES),
    TrainedAsset("US30", "Dow Jones 30", MarketCategory.INDICES),
    TrainedAsset("US500", "US S&P 500", MarketCategory.INDICES),
    TrainedAsset("USTEC", "US Tech 100", MarketCategory.INDICES),

    // STOCK (8)
    TrainedAsset("AAPL", "Apple Inc.", MarketCategory.STOCK),
    TrainedAsset("AMZN", "Amazon.com Inc.", MarketCategory.STOCK),
    TrainedAsset("META", "Meta Platforms", MarketCategory.STOCK),
    TrainedAsset("MSFT", "Microsoft Corp.", MarketCategory.STOCK),
    TrainedAsset("NFLX", "Netflix Inc.", MarketCategory.STOCK),
    TrainedAsset("NVDA", "NVIDIA Corp.", MarketCategory.STOCK),
    TrainedAsset("PYPL", "PayPal Holdings", MarketCategory.STOCK),
    TrainedAsset("TSLA", "Tesla Inc.", MarketCategory.STOCK)
)

private fun normalizeTrainedKey(symbol: String): String = symbol.uppercase(Locale.US)
    .replace("/", "").replace("-", "").replace("_", "").replace(" ", "").replace(".", "")
    .removeSuffix("M")

/** Canonical symbol list used by the Markets page, so other screens can show the same assets. */
fun marketOverviewAssetSymbols(): List<String> = TRAINED_ASSETS.map { it.symbol }

/** Canonical asset metadata (symbol, name, category) used by the Markets page. */
fun marketOverviewAssets(): List<TrainedAsset> = TRAINED_ASSETS

/**
 * Markets page — direct representative of the ASC EA on MT5.
 * Shows the EA's watchlist grouped into category columns, with a filter menu
 * (ALL, COMMODITIES, CRYPTO, …). Falls back to the saved watchlist when the EA
 * feed is offline so the page is never blank.
 * Tapping an asset opens its EA write-up page (regime, confidence, MTF, prices, timeframes).
 */
@Composable
fun MarketOverviewTab(
    selectedPair: ForexPair,
    onAssetClick: (ForexPair) -> Unit = {},
    viewModel: ForexViewModel = viewModel()
) {
    val livePairs by viewModel.allLivePairs.collectAsState()
    val eaConnected by EALiveDataStore.isConnected.collectAsState()

    // Build the list from the canonical trained assets so every trained symbol always appears.
    // Overlay live EA price/change data when the feed is connected.
    val sourcePairs = remember(livePairs) {
        val liveByKey = livePairs.associateBy { normalizeTrainedKey(it.symbol) }
        TRAINED_ASSETS.map { trained ->
            val live = liveByKey[trained.symbol]
            ForexPair(
                symbol = trained.symbol,
                name = trained.name,
                price = live?.price ?: 0.0,
                change = live?.change ?: 0.0,
                changePercent = live?.changePercent ?: 0.0,
                category = trained.category,
                eaConfidence = live?.eaConfidence ?: 0.0,
                regimeConfidence = live?.regimeConfidence ?: "NONE",
                alignmentPercentage = live?.alignmentPercentage ?: 0.0,
                eaDirection = live?.eaDirection ?: "WAIT"
            )
        }
    }
    var selectedCategory by remember { mutableStateOf<MarketCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val filtered = remember(sourcePairs, selectedCategory, searchQuery) {
        sourcePairs.filter { pair ->
            val catOk = selectedCategory == null || pair.category == selectedCategory
            val q = searchQuery.trim()
            val searchOk = q.isEmpty() || pair.symbol.contains(q, true) || pair.name.contains(q, true)
            catOk && searchOk
        }
    }

    val grouped = remember(filtered) {
        CATEGORY_ORDER.mapNotNull { category ->
            val pairs = filtered.filter { it.category == category }
            if (pairs.isEmpty()) null else category to pairs
        }
    }

    // Collect EA signals so we can display vote score + validation confidence + flags.
    val signalsByAsset by EASignalLiveStore.signalsByAsset.collectAsState()

    // Request signals for all visible assets so the bridge returns cached per-asset signals.
    LaunchedEffect(filtered, eaConnected) {
        if (eaConnected) {
            filtered.forEach { pair ->
                EASignalLiveStore.requestSignal(pair.symbol)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CategoryFilterBar(
                selected = selectedCategory,
                onSelect = { selectedCategory = it },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { searchActive = !searchActive; if (!searchActive) searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        if (searchActive) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    Text("Search symbol or name", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp)
                }
                BasicTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    singleLine = true, cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) { inner() } }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (grouped.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedCategory != null) "No assets in this category"
                            else "No assets in the watchlist yet",
                            color = SlateText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            grouped.forEach { (category, pairs) ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = categoryLabel(category),
                                color = IndigoAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = pairs.size.toString(),
                                color = SlateText,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pairs.forEach { pair ->
                                val signal = signalForPair(pair, signalsByAsset)
                                AssetRow(
                                    pair = pair,
                                    signal = signal,
                                    onClick = { onAssetClick(pair) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    selected: MarketCategory?,
    onSelect: (MarketCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Homepage tab style: plain text labels with a white underline on the
    // active item — no rounded chip backgrounds.
    val entries: List<Pair<String, MarketCategory?>> = remember {
        listOf("ALL" to null) + CATEGORY_ORDER.map { categoryLabel(it) to it }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(entries.size) { index ->
                val (label, category) = entries[index]
                val isSelected = selected == category
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onSelect(category) }
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF8E8E8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(if (isSelected) Color.White else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
    }
}

@Composable
private fun AssetRow(pair: ForexPair, signal: ASCSignalData?, onClick: () -> Unit) {
    // Prefer the real-time WS tick price (bridge pushes ~every 0.2s) and fall back
    // to the EA file poll (up to ~10-20s old) until the first tick arrives.
    val livePrice = PriceStreamManager.prices[pair.symbol]
        ?: PriceStreamManager.prices[pair.symbol.replace("/", "")]
        ?: MarketDataStore.pairSnapshot(pair.symbol)?.let { PriceStreamManager.prices[it.symbol] }
    val displayPrice = livePrice ?: pair.price
    val positive = pair.changePercent >= 0

    // Vote score (AI + EA combined) — wins as % from votes.win_pct
    val votes = signal?.chart_panel?.votes
    val voteWinPct = votes?.win_pct
    val voteDirection = votes?.direction

    // Validation confidence score
    val validConf = signal?.validation?.confidence

    // SMC / liquidity flags
    val liq = signal?.liquidity
    val qualityTier = signal?.chart_panel?.quality_tier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: Flag + Symbol + Name + Flags ── (same method as Stream page via AssetIcon)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
            val assetType = when (pair.category) {
                MarketCategory.FOREX -> "forex"
                MarketCategory.CRYPTO -> "crypto"
                MarketCategory.COMMODITIES -> "commodity"
                MarketCategory.INDICES -> "index"
                MarketCategory.STOCK -> "stock"
                MarketCategory.BONDS -> "bond"
                MarketCategory.FUTURES -> "futures"
            }
            val cleanTicker = pair.symbol.replace("/", "").removeSuffix("m").removeSuffix("M")
            AssetIcon(
                symbol = SymbolInfo(ticker = pair.symbol.replace("/", "").removeSuffix("m").removeSuffix("M"), name = pair.name, type = assetType),
                size = 34
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pair.symbol,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pair.name,
                color = SlateText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // SMC flags + quality tier + validator
            val flagChips = buildList {
                // Validator chip (always show: LONG/SHORT or INACTIVE)
                val panel = signal?.chart_panel
                val vDir = panel?.validator_direction
                val vActive = panel?.validator_active == true
                if (vActive && !vDir.isNullOrBlank()) {
                    add(vDir.uppercase(Locale.US) to when (vDir.uppercase(Locale.US)) {
                        "LONG", "BUY" -> EmeraldSuccess
                        "SHORT", "SELL" -> RoseError
                        else -> SlateText
                    })
                } else {
                    add("INACTIVE" to SlateText)
                }
                // Quality tier chip
                if (!qualityTier.isNullOrBlank() && qualityTier != "NONE") {
                    add(qualityTier to when (qualityTier) {
                        "ELITE" -> EmeraldSuccess
                        "STRONG" -> Color(0xFF60A5FA)
                        "VALID" -> Color(0xFFF59E0B)
                        "FILTERED" -> RoseError
                        else -> SlateText
                    })
                }
                if (liq != null) {
                    if (liq.fvg_bull || liq.fvg_bear) add("FVG" to Color(0xFF60A5FA))
                    if (liq.bos_bull || liq.bos_bear) add("BOS" to Color(0xFFA78BFA))
                    if (liq.sweep_high || liq.sweep_low) add("SWEEP" to Color(0xFFF59E0B))
                }
            }
            if (flagChips.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    flagChips.take(4).forEach { (label, color) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(color.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = label,
                                color = color,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            }
        }

        // ── Middle: Live Price + Vote Score ──
        Column(
            modifier = Modifier.weight(0.85f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatPrice(displayPrice, pair.symbol),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
            // Vote score (AI + EA combined)
            if (voteWinPct != null) {
                val dirColor = when (voteDirection) {
                    "BUY" -> EmeraldSuccess
                    "SELL" -> RoseError
                    else -> SlateText
                }
                val arrow = when (voteDirection) {
                    "BUY" -> "▲"
                    "SELL" -> "▼"
                    else -> "—"
                }
                Text(
                    text = "VOTE ${String.format(Locale.US, "%.0f", voteWinPct)}% $arrow",
                    color = dirColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }

        // ── Right: Change% + Confidence ──
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = (if (positive) "+" else "") + String.format(Locale.US, "%.2f%%", pair.changePercent),
                color = if (positive) EmeraldSuccess else RoseError,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
            // Validation confidence
            if (validConf != null && validConf > 0.0) {
                Text(
                    text = "CONF ${String.format(Locale.US, "%.0f", validConf * 100)}%",
                    color = confColor(validConf * 100),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun signalForPair(pair: ForexPair, signalsByAsset: Map<String, ASCSignalData>): ASCSignalData? {
    if (signalsByAsset.isEmpty()) return null
    val key = pair.symbol
        .uppercase(Locale.US)
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
        .replace(".", "")
        .removeSuffix("M")
    return signalsByAsset[key]
}

private fun categoryLabel(category: MarketCategory): String = when (category) {
    MarketCategory.COMMODITIES -> "COMMODITIES"
    MarketCategory.CRYPTO -> "CRYPTO"
    MarketCategory.FOREX -> "FOREX"
    MarketCategory.INDICES -> "INDICES"
    MarketCategory.STOCK -> "STOCKS"
    MarketCategory.BONDS -> "BONDS"
    MarketCategory.FUTURES -> "FUTURES"
}

private fun formatPrice(price: Double, symbol: String = ""): String {
    val decimals = if (price >= 1000) 2 else if (price >= 10) 3 else 4
    return String.format(Locale.US, "%,.${decimals}f", price)
}

private fun confColor(value: Double): Color =
    if (value >= 70) EmeraldSuccess else if (value >= 50) Color(0xFFF59E0B) else SlateText
