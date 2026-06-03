package com.asc.markets.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.TradingAssistantEngine
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.SlateText
import com.trading.app.data.PaperTradingAccountSnapshot
import com.trading.app.data.PaperTradingSnapshotStore
import com.trading.app.models.Position
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PortfolioManagerScreen(viewModel: ForexViewModel = viewModel()) {
    val snapshot = PaperTradingSnapshotStore.snapshot
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("trading_prefs", Context.MODE_PRIVATE) }
    
    val hasAccount = snapshot.hasLiveAccountData || snapshot.balance != 0.0 || snapshot.equity != 0.0
    val hasOpenInventory = snapshot.allPositions.isNotEmpty() || snapshot.activeTrades > 0
    val pnlColor = if (snapshot.floatingPnl >= 0.0) Color(0xFF2EE08A) else Color(0xFFE53935)
    val marginUsedPct = if (snapshot.equity > 0.0) ((snapshot.margin + snapshot.ordersMargin) / snapshot.equity) * 100.0 else 0.0
    val exposureLabel = inventoryExposureLabel(snapshot)
    val exposureColor = inventoryExposureColor(snapshot)
    
    val lastUpdatedText = remember(snapshot.lastUpdatedMillis) {
        if (snapshot.lastUpdatedMillis > 0) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            "REFRESHED AT ${sdf.format(Date(snapshot.lastUpdatedMillis))}"
        } else "WAITING FOR STREAM"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PureBlack,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Briefcase icon
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = Color(0xFF0F3A7D),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "💼",
                                            fontSize = 28.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        "INVENTORY\nMANAGER",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 22.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        "OPERATIONAL EXPOSURE HUB",
                                        color = SlateText,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                Text(
                                    lastUpdatedText,
                                    color = IndigoAccent.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "UNREALIZED PNL",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (hasAccount || hasOpenInventory) formatInventoryMoney(snapshot.floatingPnl) else "WAITING",
                                        color = pnlColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "MARGIN USED",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (hasAccount || hasOpenInventory) String.format(Locale.US, "%.2f%%", marginUsedPct) else "WAITING",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Inventory Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📈", fontSize = 16.sp)
                            Text(
                                "LIVE INVENTORY",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "${snapshot.activeTrades} POSITIONS • ${snapshot.activeOrders} ORDERS",
                            color = SlateText,
                            fontSize = 10.sp
                        )
                    }
                }

                if (hasOpenInventory) {
                    items(snapshot.allPositions) { position ->
                        LiveInventoryPositionCard(position)
                    }
                } else {
                    item {
                        EmptyInventoryState(snapshot)
                    }
                }

                // NET USD DELTA Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PureBlack,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚖️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "NET USD DELTA",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Institutional Load
                            Text(
                                "INSTITUTIONAL LOAD",
                                color = SlateText,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = (abs(snapshot.currentTradeVolume ?: 0.0) / 10.0).toFloat().coerceIn(0.0f, 1.0f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp),
                                    color = Color(0xFF5E9cff),
                                    trackColor = Color(0xFF1B1B2F)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    snapshot.currentTradeVolume?.let { String.format(Locale.US, "%.2fL", it) } ?: "0.00L",
                                    color = IndigoAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Leverage and Exposure
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "LEVERAGE",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        inventoryLeverageText(snapshot),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text(
                                        "EXPOSURE",
                                        color = SlateText,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        exposureLabel,
                                        color = exposureColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // PROTOCOL GUARD Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF1A0A0A),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        color = Color(0xFF1A0A0A),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header with warning icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚠️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "PROTOCOL GUARD",
                                    color = Color(0xFFE53935),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "LIVE DRAWDOWN ${String.format(Locale.US, "%.2f%%", inventoryDrawdownPct(snapshot))}. EMERGENCY VETO DISARMS AUTOMATED DISPATCHES WHEN LOCAL COLLATERAL RISK BREACHES POLICY.",
                                color = SlateText,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { 
                                    // Trigger Kill-Switch in TradingApp
                                    sharedPrefs.edit()
                                        .putLong("kill_switch_trigger", System.currentTimeMillis())
                                        .apply()
                                    
                                    // Disarm surveillance
                                    TradingAssistantEngine.armed = false
                                    TradingAssistantEngine.safetyLockActive = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (inventoryDrawdownPct(snapshot) >= 2.5) "KILL-SWITCH REQUIRED" else "GUARD ARMED",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // INVENTORY MANAGEMENT POLICY Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PureBlack,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header with info icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "ℹ️",
                                    fontSize = 18.sp
                                )
                                Text(
                                    "INVENTORY MANAGEMENT POLICY",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "INVENTORY REFLECTS THE CURRENT ACCOUNT AND PAPER-TRADING SNAPSHOT STREAM. WHEN NO LIVE POSITION STREAM IS AVAILABLE, OPEN INVENTORY STAYS EMPTY INSTEAD OF SHOWING SAMPLE POSITIONS.",
                                color = SlateText,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveInventoryPositionCard(position: Position) {
    val side = position.type.uppercase(Locale.US)
    val sideColor = if (side.contains("SELL") || side.contains("SHORT")) Color(0xFFE53935) else Color(0xFF2EE08A)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = PureBlack, shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        color = PureBlack,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(0xFF1A1A2E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (position.type.lowercase() == "buy") "▲" else "▼", color = if (position.type.lowercase() == "buy") Color(0xFF2EE08A) else Color(0xFFE53935), fontSize = 14.sp)
                    }
                    Column {
                        Text(position.symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${position.type.uppercase()} • ${String.format(Locale.US, "%.2f", position.volume)}L", color = SlateText, fontSize = 11.sp)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    // Note: PNL is aggregate in snapshot, individual position PNL not in basic model
                    Text(
                        "ACTIVE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "ENTRY: ${formatInventoryPrice(position.entryPrice.toDouble())}",
                        color = SlateText,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInventoryState(snapshot: PaperTradingAccountSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth().background(color = PureBlack, shape = RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        color = Color(0xFF11111F),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "NO OPEN INVENTORY",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (snapshot.isConnected) "Account stream connected. No live position is currently open." else "Waiting for the account or paper-trading stream to publish open positions.",
                color = SlateText,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

private fun formatInventoryMoney(value: Double): String {
    return String.format(Locale.US, "%s${'$'}%,.2f", if (value >= 0.0) "+" else "-", abs(value))
}

private fun formatInventoryPrice(value: Double): String {
    return when {
        value >= 1000.0 -> String.format(Locale.US, "%,.2f", value)
        value >= 100.0 -> String.format(Locale.US, "%.2f", value)
        value >= 1.0 -> String.format(Locale.US, "%.5f", value)
        else -> String.format(Locale.US, "%.6f", value)
    }
}

private fun inventoryDrawdownPct(snapshot: PaperTradingAccountSnapshot): Double {
    val equity = snapshot.equity.takeIf { it > 0.0 } ?: snapshot.balance.takeIf { it > 0.0 } ?: return 0.0
    return if (snapshot.floatingPnl < 0.0) abs(snapshot.floatingPnl) / equity * 100.0 else 0.0
}

private fun inventoryLeverageText(snapshot: PaperTradingAccountSnapshot): String {
    val equity = snapshot.equity.takeIf { it > 0.0 } ?: return "0.0x"
    val notional = abs(snapshot.currentTradePrice ?: snapshot.currentTradeEntryPrice ?: 0.0) * abs(snapshot.currentTradeVolume ?: 0.0)
    val leverage = if (notional > 0.0) notional / equity else 0.0
    return String.format(Locale.US, "%.1fx", leverage)
}

private fun inventoryExposureLabel(snapshot: PaperTradingAccountSnapshot): String {
    return when {
        snapshot.openRiskPct >= 5.0 || inventoryDrawdownPct(snapshot) >= 2.5 -> "HIGH"
        snapshot.openRiskPct >= 2.0 || snapshot.activeTrades >= 3 -> "MEDIUM"
        snapshot.activeTrades > 0 -> "LOW"
        else -> "FLAT"
    }
}

private fun inventoryExposureColor(snapshot: PaperTradingAccountSnapshot): Color {
    return when (inventoryExposureLabel(snapshot)) {
        "HIGH" -> Color(0xFFE53935)
        "MEDIUM" -> Color(0xFFFFA726)
        "LOW" -> Color(0xFF2EE08A)
        else -> SlateText
    }
}
