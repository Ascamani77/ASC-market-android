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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

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
internal fun ExecutionLedgerSection(onOpenCompliance: (ExecutionTx) -> Unit) {
	val executionMetrics = rememberExecutionMetrics()
	val sample = remember {
		listOf(
			ExecutionTx(
				id = "TX-${Random.nextInt(100000, 999999)}",
				pair = "EUR/USD",
				side = "BUY",
				result = listOf("WON", "LOST", "OPEN").random(),
				pnl = "+43 PIPS",
				rationale = "Asian low sweep followed by M15 displacement",
				internalContext = "Range break after liquidity sweep; liquidity build-up at 1.0820.",
				outcomeProfile = "Closed at HTF confluence, +43 pips",
				relayNode = "PRIMARY-UK-L14",
				timestamp = System.currentTimeMillis(),
				latencyMs = 0.02,
				fills = listOf(FillRow(1.1023, "BUY", "AI Autonomous"), FillRow(1.1066, "SELL", "AI Autonomous"))
			),
			ExecutionTx(
				id = "TX-${Random.nextInt(100000, 999999)}",
				pair = "GBP/USD",
				side = "SELL",
				result = listOf("WON", "LOST", "OPEN").random(),
				pnl = "-12 PIPS",
				rationale = "M30 rejection from structural resistance",
				internalContext = "Failed retest, false breakout",
				outcomeProfile = "Stopped at liquidity pool",
				relayNode = "EDGE-EU-L02",
				timestamp = System.currentTimeMillis() - 60000,
				latencyMs = 0.11,
				fills = listOf(FillRow(1.3021, "SELL", "AI Autonomous"))
			),
			ExecutionTx(
				id = "TX-${Random.nextInt(100000, 999999)}",
				pair = "XAU/USD",
				side = "BUY",
				result = listOf("WON", "LOST", "OPEN").random(),
				pnl = "+120 TICKS",
				rationale = "Gold responded to safe-haven flow during risk-off microsession",
				internalContext = "VIX elevated; DXY softened",
				outcomeProfile = "Held through HTF resistance, partial take-profit",
				relayNode = "PRIMARY-UK-L14",
				timestamp = System.currentTimeMillis() - 120000,
				latencyMs = 0.05,
				fills = listOf(FillRow(1820.25, "BUY", "AI Autonomous"))
			),
			ExecutionTx(
				id = "TX-${Random.nextInt(100000, 999999)}",
				pair = "WTI/USD",
				side = "SELL",
				result = listOf("WON", "LOST", "OPEN").random(),
				pnl = "-8 TICKS",
				rationale = "Oil sold into inventory surprise; momentum faded",
				internalContext = "Inventory spike at 10:00 UTC",
				outcomeProfile = "Stopped out near support",
				relayNode = "EDGE-US-L03",
				timestamp = System.currentTimeMillis() - 180000,
				latencyMs = 0.09,
				fills = listOf(FillRow(68.12, "SELL", "AI Autonomous"))
			),
			ExecutionTx(
				id = "TX-${Random.nextInt(100000, 999999)}",
				pair = "AAPL",
				side = "BUY",
				result = listOf("WON", "LOST", "OPEN").random(),
				pnl = "+2.4%",
				rationale = "Earnings beat; institutional accumulation observed",
				internalContext = "Pre-market gap; high volume",
				outcomeProfile = "Rallied to intraday target",
				relayNode = "EDGE-US-L03",
				timestamp = System.currentTimeMillis() - 240000,
				latencyMs = 0.07,
				fills = listOf(FillRow(172.55, "BUY", "AI Autonomous"))
			),
			ExecutionTx(
				id = "TX-${Random.nextInt(100000, 999999)}",
				pair = "SPX",
				side = "SELL",
				result = listOf("WON", "LOST", "OPEN").random(),
				pnl = "-0.6%",
				rationale = "Macro unwind; rotation into bonds",
				internalContext = "Breadth deteriorating; large cap pressure",
				outcomeProfile = "Partial fill then scale-out",
				relayNode = "PRIMARY-US-L01",
				timestamp = System.currentTimeMillis() - 300000,
				latencyMs = 0.03,
				fills = listOf(FillRow(4567.2, "SELL", "AI Autonomous"))
			)
		)
	}

	Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
		ExecutionLedgerRealTimeHeader()
		Spacer(modifier = Modifier.height(8.dp))
		
		// Execution Metrics Summary
		InfoBox {
			Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				Column(modifier = Modifier.weight(1f)) {
					Text("Total Trades", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
					Text("${executionMetrics.totalTrades}", color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
				}
				Column(modifier = Modifier.weight(1f)) {
					Text("Win Rate", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
					Text("${executionMetrics.winRate}%", color = EmeraldSuccess, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
				}
				Column(modifier = Modifier.weight(1f)) {
					Text("Profit Factor", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
					Text("${String.format("%.2f", executionMetrics.profitFactor)}", color = EmeraldSuccess, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
				}
				Column(modifier = Modifier.weight(1f)) {
					Text("Avg Latency", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
					Text("${String.format("%.2f", executionMetrics.latencyAvg)}ms", color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
				}
			}
		}
		
		Spacer(modifier = Modifier.height(8.dp))

		LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
			items(sample) { tx ->
				ExecutionLedgerRow(tx = tx, onGenerateCompliance = { onOpenCompliance(tx) })
			}
		}
		Spacer(modifier = Modifier.height(12.dp))
		ExecutionLedgerProtocolIntegrityFooter()
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
