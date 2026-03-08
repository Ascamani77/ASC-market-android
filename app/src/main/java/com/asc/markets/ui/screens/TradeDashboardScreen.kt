package com.asc.markets.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asc.markets.ui.screens.tradeDashboard.ui.TradeDashboardApp

/**
 * TradeDashboardScreen: Professional Trading Mission Control Center
 *
 * Layout Hierarchy:
 * A. Fixed Header: Logo, System Status, UTC Clock
 * B. Account Stats Row: Balance, Equity, Margin, Free Margin
 * C. Main Dashboard Grid:
 *    - Market Visualizer (Candlestick Chart + Timeframe Picker)
 *    - Active Positions Table (Health Score, P&L)
 *    - AI Advisory Panel
 *    - Market Intelligence
 *    - Live AI Alerts Feed
 * D. Fixed Footer: Latency, Connection Status, Heartbeat
 *
 * Design: Bento-Grid / Technical Brutalism with deep black background,
 * emerald green accents, and rose red risk indicators. Monospace fonts
 * for all numerical data to ensure precision alignment.
 */
@Composable
fun TradeDashboardScreen() {
    TradeDashboardApp(modifier = Modifier)
}




