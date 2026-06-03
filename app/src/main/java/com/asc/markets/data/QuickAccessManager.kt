package com.asc.markets.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuickAccessItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val appView: AppView
)

object QuickAccessManager {
    private const val PREFS_NAME = "quick_access_prefs"
    private const val KEY_QUICK_ACCESS_IDS = "quick_access_ids"
    
    // Default Quick Access items
    private val defaultItems = listOf(
        QuickAccessItem("post_move", Icons.Outlined.GppGood, "Post-Move Recon", AppView.TRADE_RECONSTRUCTION),
        QuickAccessItem("ai_intel", Icons.Outlined.Memory, "AI Intel", AppView.CHAT),
        QuickAccessItem("trade_dashboard", Icons.Outlined.GridView, "Trade Dashboard", AppView.TRADE_DASHBOARD),
        QuickAccessItem("event_calendar", Icons.Outlined.CalendarMonth, "Event Calendar", AppView.CALENDAR),
        QuickAccessItem("vigilance_setup", Icons.Outlined.NotificationsNone, "Vigilance Setup", AppView.ALERTS),
        QuickAccessItem("event_stream", Icons.Default.Language, "Event Stream", AppView.INTELLIGENCE_STREAM),
        QuickAccessItem("ai_simulation", Icons.Outlined.PlayCircleOutline, "AI Simulation", AppView.SIMULATION),
        QuickAccessItem("my_simulation", Icons.Outlined.ShowChart, "Backtest", AppView.MY_SIMULATION),
        QuickAccessItem("ai_terminal", Icons.Default.Terminal, "AI Terminal", AppView.AI_TERMINAL),
        QuickAccessItem("ai_sentiment", Icons.AutoMirrored.Filled.ShowChart, "AI Sentiment", AppView.SENTIMENT)
    )
    
    // All available items that can be added to Quick Access
    val allAvailableItems = listOf(
        QuickAccessItem("post_move", Icons.Outlined.GppGood, "Post-Move Recon", AppView.TRADE_RECONSTRUCTION),
        QuickAccessItem("ai_intel", Icons.Outlined.Memory, "AI Intel", AppView.CHAT),
        QuickAccessItem("trade_dashboard", Icons.Outlined.GridView, "Trade Dashboard", AppView.TRADE_DASHBOARD),
        QuickAccessItem("event_calendar", Icons.Outlined.CalendarMonth, "Event Calendar", AppView.CALENDAR),
        QuickAccessItem("vigilance_setup", Icons.Outlined.NotificationsNone, "Vigilance Setup", AppView.ALERTS),
        QuickAccessItem("event_stream", Icons.Default.Language, "Event Stream", AppView.INTELLIGENCE_STREAM),
        QuickAccessItem("ai_simulation", Icons.Outlined.PlayCircleOutline, "AI Simulation", AppView.SIMULATION),
        QuickAccessItem("my_simulation", Icons.Outlined.ShowChart, "Backtest", AppView.MY_SIMULATION),
        QuickAccessItem("ai_terminal", Icons.Default.Terminal, "AI Terminal", AppView.AI_TERMINAL),
        QuickAccessItem("ai_sentiment", Icons.AutoMirrored.Filled.ShowChart, "AI Sentiment", AppView.SENTIMENT),
        QuickAccessItem("my_alerts", Icons.Default.List, "My Alerts", AppView.MY_ALERTS),
        QuickAccessItem("markets_overview", Icons.Default.BarChart, "Markets Overview", AppView.MARKETS),
        QuickAccessItem("quotes_feed", Icons.Default.List, "Quotes Feed", AppView.QUOTES),
        QuickAccessItem("market_status", Icons.Default.Schedule, "Market Status", AppView.MARKET_STATUS),
        QuickAccessItem("analysis_opinion", Icons.Outlined.MenuBook, "Analysis & Opinion", AppView.ANALYSIS_OPINION),
        QuickAccessItem("market_watch", Icons.Default.Visibility, "Market Watch", AppView.MARKET_WATCH),
        QuickAccessItem("chart_analysis", Icons.Default.AddPhotoAlternate, "Chart Analysis Node", AppView.CHART_ANALYSIS),
        QuickAccessItem("analysis_node", Icons.Default.Timeline, "Analysis Node", AppView.ANALYSIS_RESULTS),
        QuickAccessItem("liquidity_maps", Icons.Default.Layers, "Liquidity Maps", AppView.LIQUIDITY_HUB),
        QuickAccessItem("multi_timeframe", Icons.Default.GridView, "Multi-Timeframe Analysis", AppView.MULTI_TIMEFRAME),
        QuickAccessItem("diagnostics", Icons.Default.Shield, "System Diagnostics", AppView.DIAGNOSTICS),
        QuickAccessItem("data_bus", Icons.Default.List, "Market Data Bus", AppView.DATA_HUB),
        QuickAccessItem("vigilance_nodes", Icons.Default.Notifications, "Vigilance Nodes", AppView.ALERTS),
        QuickAccessItem("logic_simulation", Icons.Default.History, "Logic Simulation", AppView.BACKTEST),
        QuickAccessItem("data_vault", Icons.Default.Lock, "Node Data Vault", AppView.DATA_VAULT)
    )
    
    private val _quickAccessItems = MutableStateFlow<List<QuickAccessItem>>(defaultItems)
    val quickAccessItems: StateFlow<List<QuickAccessItem>> = _quickAccessItems.asStateFlow()
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(KEY_QUICK_ACCESS_IDS, null)
        
        if (savedIds != null) {
            val items = savedIds.mapNotNull { id ->
                allAvailableItems.find { it.id == id }
            }
            _quickAccessItems.value = items
        } else {
            _quickAccessItems.value = defaultItems
        }
    }
    
    fun addToQuickAccess(context: Context, item: QuickAccessItem) {
        val currentItems = _quickAccessItems.value.toMutableList()
        if (!currentItems.any { it.id == item.id }) {
            currentItems.add(item)
            _quickAccessItems.value = currentItems
            saveToPrefs(context, currentItems)
        }
    }
    
    fun removeFromQuickAccess(context: Context, itemId: String) {
        val currentItems = _quickAccessItems.value.toMutableList()
        currentItems.removeAll { it.id == itemId }
        _quickAccessItems.value = currentItems
        saveToPrefs(context, currentItems)
    }
    
    fun isInQuickAccess(appView: AppView): Boolean {
        return _quickAccessItems.value.any { it.appView == appView }
    }
    
    fun getItemByAppView(appView: AppView): QuickAccessItem? {
        return allAvailableItems.find { it.appView == appView }
    }
    
    private fun saveToPrefs(context: Context, items: List<QuickAccessItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = items.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_QUICK_ACCESS_IDS, ids).apply()
    }
}
