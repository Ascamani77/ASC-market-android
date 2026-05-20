# AI Context Service - Usage Guide

## Overview

The AI Context Service is a centralized singleton that provides real-time AI intelligence to all pages in your application. It polls the AI backend every 30 seconds and exposes reactive StateFlow for UI updates.

## Quick Start

### 1. Observe AI Context in Composables

```kotlin
@Composable
fun MyScreen() {
    // Observe AI context - automatically updates when data changes
    val aiContext by AIContextService.contextState.collectAsState()
    
    // Access AI decisions
    val decision = aiContext.decisions["EURUSD"]
    
    // Access news impacts
    val highImpactNews = aiContext.newsImpacts.filter { 
        it.impact == ImpactLevel.HIGH 
    }
    
    // Check connection status
    if (aiContext.isConnected) {
        Text("AI: Live", color = Color.Green)
    } else {
        Text("AI: Cached", color = Color.Yellow)
    }
}
```

### 2. Synchronous Access (Non-Composable)

```kotlin
// Get decision for specific asset
val decision = AIContextService.getDecisionForAsset("EUR/USD")
if (decision != null) {
    println("Direction: ${decision.direction}")
    println("Confidence: ${decision.confidence}")
    println("Score: ${decision.score}/100")
}

// Get all decisions
val allDecisions = AIContextService.getAllDecisions()
println("Total assets: ${allDecisions.size}")

// Get all news
val allNews = AIContextService.getAllNewsImpacts()
val highImpactNews = allNews.filter { it.impact == ImpactLevel.HIGH }
```

### 3. Manual Refresh

```kotlin
// Force immediate refresh (useful for pull-to-refresh)
AIContextService.refresh()
```

## Data Models

### AIDecision

Represents an AI decision for a specific asset.

```kotlin
data class AIDecision(
    val asset: String,              // "EURUSD", "BTCUSDT"
    val direction: String,          // "LONG", "SHORT", "NEUTRAL"
    val confidence: Double,         // 0.0 to 1.0
    val score: Int,                 // 0 to 100
    val reason: String,             // Human-readable explanation
    val deploymentBucket: String,   // "HIGH", "MEDIUM", "LOW"
    val timestamp: Long             // Unix timestamp
)
```

**Example**:
```kotlin
AIDecision(
    asset = "EURUSD",
    direction = "LONG",
    confidence = 0.85,
    score = 85,
    reason = "Strong momentum and liquidity bias",
    deploymentBucket = "HIGH",
    timestamp = 1778890430000
)
```

### NewsImpact

Represents a news item with AI-generated impact rating.

```kotlin
data class NewsImpact(
    val headline: String,
    val source: String,
    val timestamp: String,          // "8h ago", "Just now"
    val assetType: String,          // "forex", "crypto", "stocks", etc.
    val assetSymbol: String,        // "EUR/USD", "BTC/USDT"
    val impact: ImpactLevel,        // HIGH, MEDIUM, LOW
    val affectedAssets: List<String>,
    val aiConfidence: Double,       // 0.0 to 1.0
    val imageUrl: String = ""
)

enum class ImpactLevel {
    HIGH,    // Red - Major market-moving event
    MEDIUM,  // Yellow - Moderate impact
    LOW      // Gray - Minor/informational
}
```

**Example**:
```kotlin
NewsImpact(
    headline = "Dollar elevated near multi-year highs",
    source = "Reuters",
    timestamp = "8h ago",
    assetType = "forex",
    assetSymbol = "USD/JPY",
    impact = ImpactLevel.HIGH,
    affectedAssets = listOf("USDJPY", "EURUSD"),
    aiConfidence = 0.85,
    imageUrl = ""
)
```

### AIContextState

Overall state of the AI Context Service.

```kotlin
data class AIContextState(
    val decisions: Map<String, AIDecision>,     // Key: normalized symbol
    val newsImpacts: List<NewsImpact>,
    val lastUpdated: Long,                      // Unix timestamp
    val isConnected: Boolean,
    val errorMessage: String? = null
)
```

## Symbol Normalization

All symbols are automatically normalized for consistent matching:

```kotlin
// These all match the same asset:
AIContextService.getDecisionForAsset("EUR/USD")
AIContextService.getDecisionForAsset("EURUSD")
AIContextService.getDecisionForAsset("EUR-USD")
AIContextService.getDecisionForAsset("eur/usd")

// All return the same AIDecision for "EURUSD"
```

**Normalization Rules**:
- Removes: `/`, `-`, ` `, `.`
- Converts to uppercase
- Trims whitespace

**Examples**:
- `EUR/USD` → `EURUSD`
- `BTC/USDT` → `BTCUSDT`
- `XAU/USD` → `XAUUSD`
- `NVDA.US` → `NVDAUS`

## Common Use Cases

### 1. Display News with Impact Badges

```kotlin
@Composable
fun NewsSection() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    LazyColumn {
        items(aiContext.newsImpacts) { news ->
            NewsCard(
                headline = news.headline,
                source = news.source,
                timestamp = news.timestamp,
                impact = news.impact,
                confidence = news.aiConfidence,
                impactColor = when (news.impact) {
                    ImpactLevel.HIGH -> Color.Red
                    ImpactLevel.MEDIUM -> Color.Yellow
                    ImpactLevel.LOW -> Color.Gray
                }
            )
        }
    }
}
```

### 2. Show AI Direction for Asset

```kotlin
@Composable
fun AssetCard(symbol: String) {
    val aiContext by AIContextService.contextState.collectAsState()
    val decision = aiContext.decisions[SymbolNormalizer.normalize(symbol)]
    
    if (decision != null) {
        Row {
            Text("Direction: ${decision.direction}")
            Text("Confidence: ${(decision.confidence * 100).toInt()}%")
            
            // Direction badge
            Badge(
                text = decision.direction,
                color = when (decision.direction) {
                    "LONG" -> Color.Green
                    "SHORT" -> Color.Red
                    else -> Color.Gray
                }
            )
        }
    }
}
```

### 3. Filter News by Asset Type

```kotlin
@Composable
fun ForexNews() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    val forexNews = remember(aiContext.newsImpacts) {
        aiContext.newsImpacts.filter { 
            it.assetType.equals("forex", ignoreCase = true) 
        }
    }
    
    LazyColumn {
        items(forexNews) { news ->
            NewsCard(news)
        }
    }
}
```

### 4. Show High Impact News Only

```kotlin
@Composable
fun HighImpactAlerts() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    val highImpactNews = remember(aiContext.newsImpacts) {
        aiContext.newsImpacts.filter { it.impact == ImpactLevel.HIGH }
    }
    
    if (highImpactNews.isNotEmpty()) {
        AlertBanner(
            text = "${highImpactNews.size} high impact events",
            color = Color.Red
        )
    }
}
```

### 5. Connection Status Indicator

```kotlin
@Composable
fun AIStatusIndicator() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (aiContext.isConnected) Color.Green else Color.Yellow,
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (aiContext.isConnected) "Live" else "Cached",
            fontSize = 12.sp
        )
    }
}
```

### 6. Enhance Calendar Events with AI

```kotlin
@Composable
fun CalendarEventCard(event: CalendarEvent) {
    val aiContext by AIContextService.contextState.collectAsState()
    
    val aiDecision = remember(event.affectedAssets, aiContext.decisions) {
        event.affectedAssets.firstOrNull()?.let { asset ->
            aiContext.decisions[SymbolNormalizer.normalize(asset)]
        }
    }
    
    Column {
        Text(event.title)
        
        if (aiDecision != null) {
            Row {
                Badge(text = aiDecision.direction)
                Text("${(aiDecision.confidence * 100).toInt()}%")
            }
        }
    }
}
```

## Error Handling

### Graceful Degradation

The service handles errors gracefully:

```kotlin
@Composable
fun MyScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    when {
        aiContext.isConnected -> {
            // Show live data
            Text("AI: Live", color = Color.Green)
        }
        aiContext.newsImpacts.isNotEmpty() -> {
            // Show cached data
            Text("AI: Cached", color = Color.Yellow)
        }
        else -> {
            // Show fallback
            Text("AI: Offline", color = Color.Gray)
        }
    }
    
    // Error message (if any)
    aiContext.errorMessage?.let { error ->
        Text("Error: $error", color = Color.Red, fontSize = 10.sp)
    }
}
```

### Checking Data Freshness

```kotlin
@Composable
fun DataFreshnessIndicator() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    val ageMinutes = remember(aiContext.lastUpdated) {
        (System.currentTimeMillis() - aiContext.lastUpdated) / 60000
    }
    
    Text(
        text = when {
            ageMinutes < 1 -> "Just updated"
            ageMinutes < 60 -> "${ageMinutes}m ago"
            else -> "${ageMinutes / 60}h ago"
        },
        color = if (ageMinutes < 2) Color.Green else Color.Yellow
    )
}
```

## Performance Tips

### 1. Use remember for Derived State

```kotlin
@Composable
fun MyScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    // Good: Computed once per aiContext change
    val highImpactNews = remember(aiContext.newsImpacts) {
        aiContext.newsImpacts.filter { it.impact == ImpactLevel.HIGH }
    }
    
    // Bad: Computed on every recomposition
    val highImpactNews = aiContext.newsImpacts.filter { it.impact == ImpactLevel.HIGH }
}
```

### 2. Use derivedStateOf for Complex Computations

```kotlin
@Composable
fun MyScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    val sortedNews by remember {
        derivedStateOf {
            aiContext.newsImpacts.sortedByDescending { 
                when (it.impact) {
                    ImpactLevel.HIGH -> 3
                    ImpactLevel.MEDIUM -> 2
                    ImpactLevel.LOW -> 1
                }
            }
        }
    }
}
```

### 3. Avoid Unnecessary Observations

```kotlin
// Good: Only observe in screens that need it
@Composable
fun NewsScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    // Use aiContext
}

// Bad: Observing in every composable
@Composable
fun SmallComponent() {
    val aiContext by AIContextService.contextState.collectAsState()
    // Only uses one field - pass it as parameter instead
}

// Better: Pass only what's needed
@Composable
fun SmallComponent(isConnected: Boolean) {
    // Use isConnected
}
```

## Configuration

### Change AI Backend URL

```kotlin
// In AIContextService.kt
private const val AI_BASE_URL = "http://YOUR_IP:8000"
```

### Change Poll Interval

```kotlin
// In AIContextService.kt
private const val POLL_INTERVAL_MS = 30_000L  // 30 seconds
```

### Change Timeout

```kotlin
// In AIContextService.kt
private const val REQUEST_TIMEOUT_SEC = 10L  // 10 seconds
```

## Troubleshooting

### Service Not Starting

**Problem**: No logs from AIContextService

**Solution**:
1. Check `MyApp.kt` has `AIContextService.start()` in `onCreate()`
2. Check logcat: `adb logcat | grep AIContextService`
3. Verify app is using `MyApp` as Application class in AndroidManifest

### No Data Received

**Problem**: `aiContext.decisions` is empty

**Solution**:
1. Check AI backend is running: `curl http://10.164.138.133:8000/latest-ai`
2. Check network connectivity
3. Check logcat for errors: `adb logcat | grep AIContextService`
4. Verify AI backend returns valid JSON

### Stale Data

**Problem**: Data not updating

**Solution**:
1. Check `aiContext.lastUpdated` timestamp
2. Check `aiContext.isConnected` status
3. Verify polling is active: Look for "Polling AI backend" logs every 30s
4. Call `AIContextService.refresh()` to force update

### Symbol Not Found

**Problem**: `getDecisionForAsset("EUR/USD")` returns null

**Solution**:
1. Check symbol normalization: `SymbolNormalizer.normalize("EUR/USD")` → "EURUSD"
2. Check AI backend includes this symbol in response
3. Check `aiContext.decisions.keys` to see available symbols
4. Verify symbol spelling matches AI backend

## Best Practices

### 1. Always Handle Null Cases

```kotlin
val decision = AIContextService.getDecisionForAsset("EUR/USD")
if (decision != null) {
    // Use decision
} else {
    // Show fallback or placeholder
}
```

### 2. Use Sealed Classes for UI State

```kotlin
sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(val news: List<NewsImpact>) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}

@Composable
fun NewsScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    val uiState = remember(aiContext) {
        when {
            !aiContext.isConnected && aiContext.newsImpacts.isEmpty() -> 
                NewsUiState.Error("AI backend offline")
            aiContext.newsImpacts.isEmpty() -> 
                NewsUiState.Loading
            else -> 
                NewsUiState.Success(aiContext.newsImpacts)
        }
    }
    
    when (uiState) {
        is NewsUiState.Loading -> LoadingIndicator()
        is NewsUiState.Success -> NewsList(uiState.news)
        is NewsUiState.Error -> ErrorMessage(uiState.message)
    }
}
```

### 3. Provide Visual Feedback

```kotlin
@Composable
fun MyScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    
    // Show connection status
    AIStatusIndicator(isConnected = aiContext.isConnected)
    
    // Show data age
    DataFreshnessIndicator(lastUpdated = aiContext.lastUpdated)
    
    // Show error if any
    aiContext.errorMessage?.let { error ->
        ErrorBanner(message = error)
    }
}
```

### 4. Test with Mock Data

```kotlin
// For testing/preview
@Preview
@Composable
fun NewsCardPreview() {
    val mockNews = NewsImpact(
        headline = "Test headline",
        source = "Test",
        timestamp = "Just now",
        assetType = "forex",
        assetSymbol = "EUR/USD",
        impact = ImpactLevel.HIGH,
        affectedAssets = listOf("EURUSD"),
        aiConfidence = 0.85
    )
    
    NewsCard(news = mockNews)
}
```

## API Reference

### AIContextService

#### Properties

- `contextState: StateFlow<AIContextState>` - Observable state

#### Methods

- `start()` - Start polling (called in Application.onCreate())
- `stop()` - Stop polling (called in Application.onTerminate())
- `refresh()` - Force immediate refresh
- `getDecisionForAsset(symbol: String): AIDecision?` - Get decision for asset
- `getImpactForNews(headline: String): NewsImpact?` - Get news impact
- `getAllDecisions(): Map<String, AIDecision>` - Get all decisions
- `getAllNewsImpacts(): List<NewsImpact>` - Get all news

### SymbolNormalizer

#### Methods

- `normalize(symbol: String): String` - Normalize symbol
- `matches(symbol1: String, symbol2: String): Boolean` - Check if symbols match
- `findMatch(target: String, symbols: Collection<String>): String?` - Find matching symbol

## Examples

See the following files for real-world examples:
- `MarketOverviewTab.kt` - News with impact badges
- `EventStreamScreen.kt` - Calendar events with AI enhancement

---

**Last Updated**: 2026-05-16
**Version**: 1.0.0
