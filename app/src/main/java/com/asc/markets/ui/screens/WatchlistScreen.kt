@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ASCLiquidityData
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.PriceStreamManager
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlin.collections.buildList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: ForexViewModel,
    onViewChart: (String) -> Unit = {},
    onSetAlert: (String) -> Unit = {},
    onDeepDive: (String) -> Unit = {}
) {
    val livePairs by viewModel.allLivePairs.collectAsState()
    val eaConnected by EALiveDataStore.isConnected.collectAsState()
    val signalsByAsset by EASignalLiveStore.signalsByAsset.collectAsState()

    LaunchedEffect(eaConnected) {
        if (eaConnected) {
            livePairs.forEach { pair ->
                EASignalLiveStore.requestSignal(pair.symbol)
            }
        }
    }

    val qualifyingPairs = remember(livePairs, signalsByAsset) {
        livePairs.filter { pair ->
            val key = pair.symbol
                .uppercase()
                .replace("/", "")
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .removeSuffix("M")
            val signal = signalsByAsset[key]
            if (signal == null) return@filter false
            val votes = signal.chart_panel?.votes
            val votePct = votes?.win_pct ?: 0.0
            val panel = signal.chart_panel
            val validatorActive = panel?.validator_active == true
            val validatorDir = panel?.validator_direction?.uppercase() ?: ""
            val hasValidator = validatorActive && (validatorDir == "LONG" || validatorDir == "SHORT")
            votePct >= 40.0 || hasValidator
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUALIFIED SETUPS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${qualifyingPairs.size} assets",
                            color = SlateText,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { qualifyingPairs.forEach { EASignalLiveStore.requestSignal(it.symbol) } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SlateText, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Text(
                    text = "Vote ≥ 40%  •  Validator LONG/SHORT",
                    color = SlateText,
                    fontSize = 11.sp
                )
            }

            if (qualifyingPairs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No assets meet criteria",
                        color = SlateText,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Vote ≥ 40%  or  Validator LONG/SHORT",
                        color = SlateText.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(qualifyingPairs, key = { it.symbol }) { pair ->
                        val signal = signalsByAsset[pair.symbol
                            .uppercase()
                            .replace("/", "")
                            .replace("-", "")
                            .replace("_", "")
                            .replace(" ", "")
                            .removeSuffix("M")]
                        val votes = signal?.chart_panel?.votes
                        val votePct = votes?.win_pct ?: 0.0
                        val voteDir = votes?.direction
                        val panel = signal?.chart_panel
                        val validatorDir = panel?.validator_direction?.uppercase() ?: ""
                        val validatorActive = panel?.validator_active == true
                        val hasValidator = validatorActive && (validatorDir == "LONG" || validatorDir == "SHORT")

                        val livePrice = PriceStreamManager.prices[pair.symbol]
                            ?: PriceStreamManager.prices[pair.symbol.replace("/", "")]
                            ?: MarketDataStore.pairSnapshot(pair.symbol)?.let { PriceStreamManager.prices[it.symbol] }
                        val displayPrice = livePrice ?: pair.price

                        val liq = signal?.liquidity
                        val qualityTier = signal?.chart_panel?.quality_tier

                        AssetRow(
                            pair = pair,
                            displayPrice = displayPrice,
                            votePct = votePct,
                            voteDir = voteDir,
                            validatorDir = if (hasValidator) validatorDir else "INACTIVE",
                            hasValidator = hasValidator,
                            liq = liq,
                            qualityTier = qualityTier,
                            onClick = { onDeepDive(pair.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    pair: ForexPair,
    displayPrice: Double,
    votePct: Double,
    voteDir: String?,
    validatorDir: String,
    hasValidator: Boolean,
    liq: ASCLiquidityData?,
    qualityTier: String?,
    onClick: () -> Unit
) {
    val voteColor = when (voteDir?.uppercase()) {
        "BUY" -> EmeraldSuccess
        "SELL" -> RoseError
        else -> SlateText
    }
    val validatorColor = when (validatorDir) {
        "LONG", "BUY" -> EmeraldSuccess
        "SHORT", "SELL" -> RoseError
        else -> SlateText
    }

    val flagChips = buildList {
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

    InfoBox(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        curve = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Column(Modifier.weight(1.2f)) {
            Text(
                text = pair.symbol,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (flagChips.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(0.9f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatPrice(displayPrice, pair.symbol),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
            val arrow = when (voteDir?.uppercase()) {
                "BUY" -> "▲"
                "SELL" -> "▼"
                else -> "—"
            }
            Text(
                text = "VOTE ${String.format(java.util.Locale.US, "%.0f", votePct)}% $arrow",
                color = voteColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }

        Column(
            modifier = Modifier.weight(0.7f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = validatorDir,
                color = validatorColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
        }
    }
}

private fun formatPrice(price: Double, symbol: String = ""): String {
    val decimals = if (price >= 1000) 2 else if (price >= 10) 3 else 4
    return String.format(java.util.Locale.US, "%,.${decimals}f", price)
}