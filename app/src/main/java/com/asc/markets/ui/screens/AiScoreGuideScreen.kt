package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScoreGuideScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Score Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Understanding Your AI Scores",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your AI system analyzes 24 assets across multiple dimensions to identify high-probability trading opportunities.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pre-Move AI Score Breakdown
            GuideSection("Pre-Move AI Score Breakdown")
            Text(
                "Your main AI score is calculated from 6 key components:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            ScoreComponentCard(
                name = "Ignition Probability",
                weight = "24%",
                description = "Measures how likely a price move will ignite based on momentum, volume, and structural pressure.",
                color = Color(0xFFE57373)
            )

            ScoreComponentCard(
                name = "Expansion Probability",
                weight = "22%",
                description = "Evaluates the likelihood that an initiated move will expand into a significant trend.",
                color = Color(0xFFBA68C8)
            )

            ScoreComponentCard(
                name = "Confluence Score",
                weight = "22%",
                description = "Counts how many technical factors align (support/resistance, patterns, indicators, etc.).",
                color = Color(0xFF64B5F6)
            )

            ScoreComponentCard(
                name = "Entry Quality Score",
                weight = "14%",
                description = "Assesses the timing and quality of the entry point based on risk/reward and market structure.",
                color = Color(0xFF4DB6AC)
            )

            ScoreComponentCard(
                name = "Chart Context Score",
                weight = "10%",
                description = "Analyzes the broader chart structure, key levels, and multi-timeframe alignment.",
                color = Color(0xFF81C784)
            )

            ScoreComponentCard(
                name = "Volatility Score",
                weight = "8%",
                description = "Evaluates current volatility state and whether conditions favor explosive moves.",
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // What Updates Every 5 Minutes
            GuideSection("What Updates Every 5 Minutes")
            
            UpdateCategoryCard(
                title = "Market Regime",
                items = listOf(
                    "Trending vs Ranging",
                    "Explosive vs Compressed",
                    "Bullish vs Bearish bias",
                    "Regime persistence probability"
                )
            )

            UpdateCategoryCard(
                title = "Volatility State",
                items = listOf(
                    "Normal, Expanding, Burst, or Dead",
                    "ATR ratio and volatility trends",
                    "Compression/expansion cycles",
                    "Breakout probability"
                )
            )

            UpdateCategoryCard(
                title = "Technical Indicators",
                items = listOf(
                    "RSI, EMA, ATR, Momentum",
                    "MACD, Bollinger Bands",
                    "Volume analysis",
                    "Divergence detection"
                )
            )

            UpdateCategoryCard(
                title = "Signal Confluence",
                items = listOf(
                    "Support/Resistance alignment",
                    "Pattern recognition",
                    "Multi-timeframe confirmation",
                    "Indicator agreement"
                )
            )

            UpdateCategoryCard(
                title = "Entry Quality",
                items = listOf(
                    "Risk/Reward ratio",
                    "Stop loss placement",
                    "Entry timing precision",
                    "Market structure quality"
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score Interpretation
            GuideSection("Score Interpretation")
            
            ScoreRangeCard(
                range = "80-100%",
                label = "EXCEPTIONAL",
                description = "Rare, high-conviction setups with strong confluence across all factors. Maximum position size recommended.",
                color = Color(0xFF4CAF50)
            )

            ScoreRangeCard(
                range = "60-79%",
                label = "STRONG",
                description = "High-quality setups with good confluence. Standard position sizing appropriate.",
                color = Color(0xFF8BC34A)
            )

            ScoreRangeCard(
                range = "40-59%",
                label = "MODERATE",
                description = "Decent setups but lacking full confluence. Reduced position size or wait for improvement.",
                color = Color(0xFFFFC107)
            )

            ScoreRangeCard(
                range = "20-39%",
                label = "WEAK",
                description = "Low-quality setups with minimal confluence. Avoid trading or wait for better conditions.",
                color = Color(0xFFFF9800)
            )

            ScoreRangeCard(
                range = "0-19%",
                label = "NO TRADE",
                description = "Poor conditions with no meaningful setup. Stay out of the market.",
                color = Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Data Sources
            GuideSection("Live Data Sources")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    DataSourceItem("Pepperstone cTrader", "Forex, Commodities, Indices, Stocks, Crypto")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    DataSourceItem("Binance", "USDT Crypto Pairs (BTCUSDT, ETHUSDT)")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    DataSourceItem("Update Frequency", "Market data: Real-time | AI Analysis: Every 5 minutes")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Assets Covered
            GuideSection("24 Assets Analyzed")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    AssetCategoryItem("Forex (5)", "EURUSD, GBPUSD, USDJPY, USDCHF, AUDUSD")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AssetCategoryItem("Crypto (4)", "BTCUSD, ETHUSD, BTCUSDT, ETHUSDT")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AssetCategoryItem("Commodities (4)", "XAUUSD (Gold), XAGUSD (Silver), Crude-F (WTI), Brent-F (Brent)")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AssetCategoryItem("Indices (4)", "US30, SPX500, NAS100, DXY")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AssetCategoryItem("Stocks (5)", "AAPL, AMZN, MSFT, NVDA, TSLA")
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    AssetCategoryItem("Bonds (2)", "US02Y, US10Y")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // System Status
            GuideSection("System Architecture")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Your AI system runs 40 specialized feeders:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 6 Fast Feeders: Run every 5 minutes for trading signals", style = MaterialTheme.typography.bodySmall)
                    Text("• 34 Full Feeders: Run every 15 minutes for complete analysis", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This ensures you get fresh AI scores while maintaining comprehensive market analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GuideSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ScoreComponentCard(
    name: String,
    weight: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    weight,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun UpdateCategoryCard(
    title: String,
    items: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text("• ", style = MaterialTheme.typography.bodyMedium)
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ScoreRangeCard(
    range: String,
    label: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    range,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DataSourceItem(title: String, description: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AssetCategoryItem(category: String, assets: String) {
    Column {
        Text(
            category,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            assets,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
