package com.asc.markets.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import com.asc.markets.ui.screens.dashboard.*
import com.asc.markets.ui.theme.*
import com.asc.markets.data.ForexPair
import androidx.compose.ui.platform.LocalContext
import android.os.Vibrator
import android.os.VibrationEffect

enum class DashboardTab { MARKET_OVERVIEW, TECHNICAL_VITALS, STRATEGY_SIGNALS, ANALYTICAL_QUALITY, EXECUTION_LEDGER, MARKET_PSYCHOLOGY, METHODOLOGY }

@Composable
fun DashboardScreen(viewModel: ForexViewModel) {
    var activeTab by remember { mutableStateOf(DashboardTab.MARKET_OVERVIEW) }
    val selectedPair by viewModel.selectedPair.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Content
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.animation.Crossfade(targetState = activeTab) { tab ->
                        when (tab) {
                        DashboardTab.MARKET_OVERVIEW -> MarketOverviewTab(selectedPair) { pair ->
                            viewModel.selectPair(pair)
                            viewModel.navigateTo(AppView.TRADING_ASSISTANT)
                        }
                        DashboardTab.TECHNICAL_VITALS -> TechnicalVitalsTab()
                        DashboardTab.STRATEGY_SIGNALS -> StrategySignalsTab()
                        DashboardTab.ANALYTICAL_QUALITY -> AnalyticalQualityTab()
                        DashboardTab.EXECUTION_LEDGER -> ExecutionLedgerTab()
                        DashboardTab.MARKET_PSYCHOLOGY -> MarketPsychologyTab()
                        DashboardTab.METHODOLOGY -> EducationTab()
                    }
                }
            }
            
            // Fixed Top Tab Switcher (Floating style)
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), contentAlignment = Alignment.Center) {
                val context = LocalContext.current
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.wrapContentWidth().height(44.dp).padding(horizontal = 16.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(DashboardTab.values()) { tab ->
                            val active = activeTab == tab
                            Surface(
                                color = if (active) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .padding(horizontal = 2.dp)
                                    .clickable {
                                        val vib = context.getSystemService(Vibrator::class.java)
                                        vib?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                        activeTab = tab
                                    }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val resId = when (tab) {
                                        DashboardTab.MARKET_OVERVIEW -> R.drawable.lucide_line_chart
                                        DashboardTab.TECHNICAL_VITALS -> R.drawable.lucide_activity
                                        DashboardTab.STRATEGY_SIGNALS -> R.drawable.lucide_list_filter
                                        DashboardTab.ANALYTICAL_QUALITY -> R.drawable.lucide_pie_chart
                                        DashboardTab.EXECUTION_LEDGER -> R.drawable.lucide_arrow_left_right
                                        DashboardTab.MARKET_PSYCHOLOGY -> R.drawable.lucide_binary
                                        DashboardTab.METHODOLOGY -> R.drawable.lucide_book_open
                                    }
                                    Icon(
                                        painter = painterResource(id = resId), 
                                        contentDescription = null, 
                                        tint = if (active) Color.Black else Color.Gray, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (active) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tab.name.replace("_", " ").toLowerCase().capitalize(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}