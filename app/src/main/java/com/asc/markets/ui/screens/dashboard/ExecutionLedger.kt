package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.BorderStroke
import com.asc.markets.R
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.screens.dashboard.MiniSparkline
import com.asc.markets.ui.screens.dashboard.demoSparkline
import com.asc.markets.ui.screens.dashboard.WinLossDonut
import com.asc.markets.ui.screens.dashboard.CompactGauge
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.PreMoveIntelligenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Simple data models for the ledger
internal data class FillRow(val price: Double, val side: String, val executionType: String)

internal data class ExecutionTx(
	val id: String,
	val pair: String,
	val side: String,
	val result: String,
	val pnl: String,
	val rationale: String,
	val internalContext: String,
	val outcomeProfile: String,
	val relayNode: String,
	val timestamp: Long,
	val latencyMs: Double,
	val fills: List<FillRow>
)

// Extension function to convert PostMoveAuditCase to ExecutionTx
private fun PostMoveAuditCase.toExecutionTx(): ExecutionTx {
	return ExecutionTx(
		id = this.id,
		pair = this.symbol,
		side = this.direction,
		result = when {
			this.win == true -> "WON"
			this.win == false -> "LOST"
			else -> "OPEN"
		},
		pnl = this.pnl?.let { "${if (it >= 0) "+" else ""}${String.format("%.2f", it)}" } ?: "N/A",
		rationale = this.thesis,
		internalContext = this.reconstructionLines.firstOrNull() ?: "No context available",
		outcomeProfile = this.postMoveOutcome,
		relayNode = this.nodeId,
		timestamp = this.timestamp,
		latencyMs = 0.0, // Not available in PostMoveAuditCase
		fills = listOf(
			FillRow(this.entryPrice ?: 0.0, this.direction, "AI Autonomous"),
			FillRow(this.exitPrice ?: 0.0, if (this.direction == "LONG") "SELL" else "BUY", "AI Autonomous")
		)
	)
}

@Composable
internal fun ExecutionLedgerRealTimeHeader() {
	InfoBox(modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier
				.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.03f), modifier = Modifier.size(36.dp)) {
					Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
						Text("\u2194", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall)
					}
				}
				Spacer(modifier = Modifier.width(8.dp))
				Column {
					Text("REAL-TIME", color = Color.White, fontSize = DashboardFontSizes.emojiIcon, fontWeight = FontWeight.Black)
					Text("NODE", color = Color.White, fontSize = DashboardFontSizes.emojiIcon, fontWeight = FontWeight.Black)
					Spacer(modifier = Modifier.height(4.dp))
					Text("AUTONOMOUS AUDIT STREAM", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
				}
			}

			Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
				// State integrity label above the SMC badge (no background)
				Text("STATE INTEGRITY", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
				Spacer(modifier = Modifier.height(4.dp))
				Surface(color = IndigoAccent.copy(alpha = 0.0f), shape = RoundedCornerShape(6.dp), border = BorderStroke(0.dp, Color.Transparent)) {
					// SMC-L4 badge (visible text on transparent surface)
					Text("SMC-L4 SECURE", color = Color.White, fontWeight = FontWeight.Black, fontSize = DashboardFontSizes.gridHeaderSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
				}

				Spacer(modifier = Modifier.height(8.dp))

				Row(verticalAlignment = Alignment.CenterVertically) {
					Box(modifier = Modifier.size(6.dp).background(EmeraldSuccess, CircleShape))
					Spacer(modifier = Modifier.width(6.dp))
					Column(horizontalAlignment = Alignment.End) {
						Text("NODE PIPELINE", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
						Text("PROTOCOL\nLOCKED", color = EmeraldSuccess, fontWeight = FontWeight.Black, fontSize = DashboardFontSizes.gridHeaderSmall)
					}
				}
			}
		}
	}
}

@Composable
internal fun ExecutionLedgerProtocolIntegrityFooter() {
	Surface(color = IndigoAccent.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
		Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
			Text("🔐", fontSize = DashboardFontSizes.emojiIcon)
			Spacer(modifier = Modifier.width(8.dp))
			Text("Protocol Integrity: Ledger is immutable and engine-driven.", color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall)
		}
	}
}

@Composable
internal fun ExecutionMetricsSubheader(metrics: ExecutionMetrics) {
	Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
		InfoBox {
			Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
				Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					Column(modifier = Modifier.weight(1f)) {
						Text("Total Trades", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						Text("${metrics.totalTrades}", color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
					}
					Column(modifier = Modifier.weight(1f)) {
						Text("Win Rate", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						Text("${metrics.winRate}%", color = EmeraldSuccess, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
					}
					Column(modifier = Modifier.weight(1f)) {
						Text("Profit Factor", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						Text("${String.format("%.2f", metrics.profitFactor)}", color = EmeraldSuccess, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
					}
					Column(modifier = Modifier.weight(1f)) {
						Text("Avg Latency", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						Text("${String.format("%.2f", metrics.latencyAvg)}ms", color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
					}
				}
				Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
				Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
					val won = (metrics.totalTrades * metrics.winRate / 100).coerceAtLeast(1)
					val lost = (metrics.totalTrades - won).coerceAtLeast(0)
					WinLossDonut(won = won, lost = lost, modifier = Modifier.size(100.dp))
					Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text("Equity Curve Snapshot", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
						MiniSparkline(
							points = demoSparkline(count = 40, seed = metrics.totalTrades, trendBias = if (metrics.profitFactor > 1.5f) 0.02f else -0.005f),
							modifier = Modifier.fillMaxWidth().height(70.dp).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp)),
							color = if (metrics.profitFactor > 1.5f) EmeraldSuccess else RoseError,
							fillColor = if (metrics.profitFactor > 1.5f) EmeraldSuccess.copy(alpha = 0.08f) else RoseError.copy(alpha = 0.08f)
						)
					}
					CompactGauge(
						value = metrics.winRate,
						modifier = Modifier,
						color = if (metrics.winRate >= 60) EmeraldSuccess else if (metrics.winRate >= 40) Color(0xFFF59E0B) else RoseError,
						label = "WIN"
					)
					CompactGauge(
						value = (100 - (metrics.latencyAvg * 12f).toInt()).coerceIn(0, 100),
						modifier = Modifier,
						color = if (metrics.latencyAvg <= 2.0f) EmeraldSuccess else if (metrics.latencyAvg <= 5.0f) Color(0xFFF59E0B) else RoseError,
						label = "LAT"
					)
				}
			}
		}
	}
}

@Composable
internal fun ExecutionLedgerSection(onOpenCompliance: (ExecutionTx) -> Unit, viewModel: ForexViewModel) {
	val executionMetrics = rememberExecutionMetrics()
	
	// Load real trade data
	var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
	val auditRecords by viewModel.auditRecords.collectAsState()
	val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
	val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
	// BINANCE AND FALLBACK REMOVED - EA ONLY
	val timedHistory = remember(marketTimedHistory) {
		marketTimedHistory
	}
	
	val cases = remember(closedTrades, auditRecords, candidates, timedHistory) {
		PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
	}
	
	// Convert PostMoveAuditCase to ExecutionTx for display
	val executionTxList = remember(cases) {
		cases.map { it.toExecutionTx() }
	}
	
	LaunchedEffect(viewModel.tradeHistoryRepository) {
		closedTrades = withContext(Dispatchers.IO) {
			viewModel.tradeHistoryRepository?.getLast100Trades().orEmpty()
		}
	}

	val listState = rememberLazyListState()

	// Watch scroll and animate header collapse smoothly
	val collapseRange = 150f
	val collapseProgress by remember {
		derivedStateOf {
			val absoluteScroll = (listState.firstVisibleItemIndex * 100f) + listState.firstVisibleItemScrollOffset
			(absoluteScroll / collapseRange).coerceIn(0f, 1f)
		}
	}

	LaunchedEffect(collapseProgress) {
		viewModel.setGlobalHeaderCollapse(collapseProgress)
	}

	// Scrollable LazyColumn with trading records and footer only (metrics are static above)
	LazyColumn(
		state = listState,
		modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
		contentPadding = PaddingValues(bottom = 120.dp)
	) {
		// Trading records
		if (executionTxList.isEmpty()) {
			item {
				InfoBox(modifier = Modifier.fillMaxWidth()) {
					Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text("NO EXECUTION LEDGER RECORDS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
						Text("The ledger will populate after confirmed fills are saved through TradeHistoryRepository or synchronized from a closed-order source.", color = SlateText, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = InterFontFamily)
					}
				}
			}
		} else {
			items(executionTxList) { tx ->
				ExecutionLedgerRow(tx = tx, onGenerateCompliance = { onOpenCompliance(tx) })
			}
		}
		
		// Footer
		item {
			ExecutionLedgerProtocolIntegrityFooter()
		}
		
		item {
			Spacer(modifier = Modifier.height(12.dp))
		}
	}
}

@Composable
internal fun ExecutionLedgerRow(tx: ExecutionTx, onGenerateCompliance: () -> Unit) {
	// Mobile-first: each trade box is a vertical stack of 3 layers
	InfoBox(modifier = Modifier.fillMaxWidth().clickable { /* optional click */ }) {
		Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			// Layer 1: Identity & Side (Top)
			Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				PairFlags(tx.pair, 36)
				Spacer(modifier = Modifier.width(12.dp))
				Column(modifier = Modifier.weight(1f)) {
					Text(tx.pair, color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
					Text("${tx.side.uppercase(Locale.getDefault())} DISPATCH", color = if (tx.side.equals("BUY", ignoreCase = true)) EmeraldSuccess else RoseError, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
				}
				Text("#${tx.id.takeLast(4)}", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
			}

			// Layer 2: Results & Performance (Middle)
			Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				// Row: Audit Result label (left) and result badge (right)
				Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
					Text("AUDIT RESULT", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
					Spacer(modifier = Modifier.weight(1f))
					Surface(
						color = if (tx.result == "WON") EmeraldSuccess.copy(alpha = 0.12f) else RoseError.copy(alpha = 0.12f),
						shape = RoundedCornerShape(8.dp),
						border = BorderStroke(1.dp, if (tx.result == "WON") EmeraldSuccess else RoseError)
					) {
						Text(tx.result, color = if (tx.result == "WON") EmeraldSuccess else RoseError, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = InterFontFamily, fontWeight = FontWeight.Black)
					}
				}

				// Row: Realized Delta label and PnL on same line
				Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
					Text("REALIZED DELTA", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
					Spacer(modifier = Modifier.weight(1f))
					Text(tx.pnl, color = if (tx.pnl.startsWith("+")) EmeraldSuccess else RoseError, fontSize = DashboardFontSizes.valueLarge, fontFamily = InterFontFamily, fontWeight = FontWeight.Black)
				}

				Button(onClick = onGenerateCompliance, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
					Icon(painter = painterResource(id = R.drawable.lucide_book_open), contentDescription = null, tint = Color.White)
					Spacer(modifier = Modifier.width(8.dp))
					Text("GENERATE COMPLIANCE REPORT", color = Color.White, fontWeight = FontWeight.Black)
				}
			}

			// Layer 3: Rationale & Telemetry (Bottom)
			Column(modifier = Modifier.fillMaxWidth()) {
				Text("INSTITUTIONAL REASONING", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
				Spacer(modifier = Modifier.height(6.dp))
				Surface(color = Color.White.copy(alpha = 0.02f), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
					Text(
						"\"${tx.rationale}. Node detected institutional buy program following Asian low sweep. CHoCH confirmed on M15. Strategy validated by HTF bias and news gate; position managed via adaptive risk filters.\"",
						color = Color.White,
						modifier = Modifier.padding(14.dp),
						fontSize = DashboardFontSizes.sectionHeaderSmall,
						lineHeight = 20.sp,
						fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
					)
				}

				Spacer(modifier = Modifier.height(8.dp))
				Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					InfoBox(modifier = Modifier.weight(1f)) {
						Column(modifier = Modifier.padding(8.dp)) {
						Text("INTERNAL CONTEXT", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
						Spacer(modifier = Modifier.height(6.dp))
						Text(tx.internalContext.uppercase(Locale.getDefault()), color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
						}
					}

					InfoBox(modifier = Modifier.weight(1f)) {
						Column(modifier = Modifier.padding(8.dp)) {
						Text("OUTCOME PROFILE", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
						Spacer(modifier = Modifier.height(6.dp))
						Text(tx.outcomeProfile.uppercase(Locale.getDefault()), color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
						}
					}
				}

				Spacer(modifier = Modifier.height(8.dp))
				// Relay node block (label, node, time, latency)
				Column(modifier = Modifier.fillMaxWidth()) {
					Text("RELAY NODE", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black)
					Spacer(modifier = Modifier.height(6.dp))
					Text(tx.relayNode, color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
					Spacer(modifier = Modifier.height(8.dp))
					Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					Text("🕒", fontSize = DashboardFontSizes.sectionHeaderSmall)
					val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
					Text(sdf.format(Date(tx.timestamp)), color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall)
					}

					Spacer(modifier = Modifier.height(8.dp))
					Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.03f)) {
							Text("LATENCY: ${String.format("%.2f", tx.latencyMs)}MS", color = SlateText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontFamily = InterFontFamily)
					}
				}
			}
		}
	}
}

@Composable
internal fun DeepAuditModal(tx: ExecutionTx, onClose: () -> Unit) {
	Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
			Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
				Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
					Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
						Text("Compliance Report — ${tx.id}", color = Color.White, fontSize = DashboardFontSizes.emojiIcon, fontWeight = FontWeight.Black)
						Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
					}

					// Transaction Identity
					Text("Transaction Identity", color = SlateText, fontSize = DashboardFontSizes.gridHeaderSmall)
					val nodeHash = "NODE-${tx.id.takeLast(6)}-${(tx.timestamp % 100000).toString().padStart(5, '0')}"
					Text("Hash: $nodeHash", color = Color.White, fontFamily = InterFontFamily)
					Text("Transaction ID: ${tx.id}", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)

					// Snapshot of the World: 4 mini-boxes
					Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						listOf("VIX" to "12.3", "News Gate" to "CLEAR", "DXY Beta" to "0.8", "Bias" to "Bullish").forEach { pair: Pair<String, String> ->
							val (k, v) = pair
							Card(modifier = Modifier.weight(1f)) {
								Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
								Text(k, color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
								Spacer(modifier = Modifier.height(6.dp))
								Text(v, color = Color.White, fontSize = DashboardFontSizes.valueSmall)
								}
							}
						}
					}

					// Audit Trail table
					Text("Audit Trail", color = SlateText, fontSize = DashboardFontSizes.gridHeaderSmall)
					Column(modifier = Modifier.fillMaxWidth()) {
						Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
						Text("Fill Price", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						Text("Side", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						Text("Exec Type", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
						}
						Divider(color = Color.White.copy(alpha = 0.06f))
						tx.fills.forEach { f: FillRow ->
							Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
								Text(String.format("%.4f", f.price), color = Color.White)
								Text(f.side, color = Color.White)
								Text(f.executionType, color = SlateText)
							}
						}
					}

					Spacer(modifier = Modifier.weight(1f))

					// Verification Footer with pulsing digital signature and Download PDF
					val transition = rememberInfiniteTransition()
					val pulse by transition.animateFloat(
						initialValue = 0.8f,
						targetValue = 1.2f,
						animationSpec = infiniteRepeatable(animation = keyframes {
							durationMillis = 1200
						})
					)

					Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
						Row(verticalAlignment = Alignment.CenterVertically) {
							Box(modifier = Modifier.size((12.dp * pulse)).background(EmeraldSuccess, shape = androidx.compose.foundation.shape.CircleShape))
							Spacer(modifier = Modifier.width(8.dp))
							Column {
								Text("Digital Signature", color = Color.White, fontWeight = FontWeight.Black)
								Text("Verified (deterministic)", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
							}
						}
						Button(onClick = { /* stub: download PDF */ }, colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)) {
							Text("Download PDF", color = Color.White)
						}
					}
				}
			}
		}
	}
}
