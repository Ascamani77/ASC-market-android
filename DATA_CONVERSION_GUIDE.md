# Data Conversion Guide: MQL5 (ASC EA) → Kotlin (MyRealApp)

This guide shows you how to convert data structures from your ASC Expert Advisor (MQL5) to match the format used in your MyRealApp Android application (Kotlin).

---

## Overview

Your **ASC EA (MQL5)** uses **struct** definitions for data modeling.  
Your **MyRealApp (Kotlin)** uses **data class** definitions with Kotlin serialization.

The conversion pattern is straightforward:
```
MQL5 struct → Kotlin data class + @Serializable annotation
```

---

## Core Conversion Rules

### 1. Basic Type Mappings

| MQL5 Type | Kotlin Type | Notes |
|-----------|-------------|-------|
| `int` | `Int` | Direct mapping |
| `double` | `Double` | Direct mapping |
| `string` | `String` | Direct mapping |
| `bool` | `Boolean` | Direct mapping |
| `datetime` | `Long` | Convert to Unix timestamp (milliseconds) |
| `ENUM_ORDER_TYPE` | `String` or custom enum | Use `"BUY"` / `"SELL"` strings or create enum |

### 2. Array Handling

| MQL5 | Kotlin |
|------|--------|
| `Type array[]` | `List<Type>` or `MutableList<Type>` |

### 3. Serialization

All data classes that need JSON serialization should:
- Add `@kotlinx.serialization.Serializable` annotation
- Use `@SerialName("field_name")` for custom JSON field names

---

## Conversion Examples from Your Code

### Example 1: External AI Signal

#### MQL5 (ASC_UNIFIED_AI.mq5)
```mql5
struct ExternalAISignal
{
    string direction;
    double confidence;
    string regime;
    bool valid;
};
ExternalAISignal externalAISignal;
```

#### Kotlin (MyRealApp equivalent)
```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ExternalAISignal(
    val direction: String,          // "BUY", "SELL", "WAIT"
    val confidence: Double,          // 0.0 - 1.0
    val regime: String,              // "TRENDING", "RANGING", etc.
    val valid: Boolean
)
```

---

### Example 2: Supply & Demand Zone

#### MQL5 (ASC_UNIFIED_AI.mq5)
```mql5
struct SDZone
{
    double topPrice;
    double bottomPrice;
    datetime startTime;
    int strength;        // Number of touches
    bool isSupply;       // true = supply (resistance), false = demand (support)
    bool isBroken;
};
SDZone supplyZones[];
SDZone demandZones[];
```

#### Kotlin (MyRealApp equivalent)
```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class SupplyDemandZone(
    val topPrice: Double,
    val bottomPrice: Double,
    val startTime: Long,              // Unix timestamp in milliseconds
    val strength: Int,                // Number of touches
    val isSupply: Boolean,            // true = supply (resistance), false = demand (support)
    val isBroken: Boolean
)

// In your ViewModel or Repository:
val supplyZones: List<SupplyDemandZone> = emptyList()
val demandZones: List<SupplyDemandZone> = emptyList()
```

---

### Example 3: Volume Zone

#### MQL5 (ASC_UNIFIED_AI.mq5)
```mql5
struct VolumeZone
{
    datetime startTime;
    datetime endTime;
    double topPrice;
    double bottomPrice;
    bool isBullish;  // true = accumulation (buying), false = distribution (selling)
    int strength;    // Number of high-volume candles in zone
    double totalVolume;
};
VolumeZone volumeZones[];
```

#### Kotlin (MyRealApp equivalent)
```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class VolumeZone(
    val startTime: Long,              // Unix timestamp in milliseconds
    val endTime: Long,                // Unix timestamp in milliseconds
    val topPrice: Double,
    val bottomPrice: Double,
    val isBullish: Boolean,           // true = accumulation (buying), false = distribution (selling)
    val strength: Int,                // Number of high-volume candles in zone
    val totalVolume: Double
)

// In your ViewModel or Repository:
val volumeZones: MutableStateFlow<List<VolumeZone>> = MutableStateFlow(emptyList())
```

---

### Example 4: Backtest Trade (Complex Example)

#### MQL5 (ASC_Types.mqh)
```mql5
struct BacktestTrade
{
    datetime open_time;
    datetime close_time;
    
    string symbol;
    
    ENUM_ORDER_TYPE direction;
    
    double entry_price;
    double exit_price;
    
    double stop_loss;
    double take_profit;
    
    double profit_loss;
    
    string decision_reason;
    
    double confidence;
};
```

#### Kotlin (MyRealApp equivalent)
```kotlin
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BacktestTrade(
    @SerialName("open_time")
    val openTime: Long,                 // Unix timestamp in milliseconds
    
    @SerialName("close_time")
    val closeTime: Long,                // Unix timestamp in milliseconds
    
    val symbol: String,                 // e.g., "BTCUSD", "EURUSD"
    
    val direction: String,              // "BUY" or "SELL"
    
    @SerialName("entry_price")
    val entryPrice: Double,
    
    @SerialName("exit_price")
    val exitPrice: Double,
    
    @SerialName("stop_loss")
    val stopLoss: Double,
    
    @SerialName("take_profit")
    val takeProfit: Double,
    
    @SerialName("profit_loss")
    val profitLoss: Double,
    
    @SerialName("decision_reason")
    val decisionReason: String,
    
    val confidence: Double              // 0.0 - 1.0
)
```

**Note:** Using `@SerialName` ensures snake_case JSON fields map correctly to camelCase Kotlin properties.

---

### Example 5: Decision Log

#### MQL5 (ASC_Types.mqh)
```mql5
struct DecisionLog
{
    datetime time;
    string decision;
    double confidence;
    string regime;
    string structure;
    string liquidity;
    string volatility;
    string momentum;
    string reasons;
};
```

#### Kotlin (MyRealApp equivalent)
```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class DecisionLog(
    val time: Long,                     // Unix timestamp in milliseconds
    val decision: String,               // "BUY", "SELL", "WAIT", "EXIT", "REDUCE", "HOLD"
    val confidence: Double,             // 0.0 - 1.0
    val regime: String,                 // e.g., "TRENDING", "RANGING"
    val structure: String,              // e.g., "BULLISH", "BEARISH", "NEUTRAL"
    val liquidity: String,              // e.g., "HIGH", "MEDIUM", "LOW"
    val volatility: String,             // e.g., "EXPANDING", "CONTRACTING"
    val momentum: String,               // e.g., "STRONG_UP", "WEAK_DOWN"
    val reasons: String                 // Detailed explanation
)
```

---

## Existing MyRealApp Models Reference

Your MyRealApp already has well-defined models. Here's how they align with common ASC EA patterns:

### AutomatedTrade (from Models.kt)
```kotlin
data class AutomatedTrade(
    val id: String,
    val pair: String,
    val side: String,                    // "BUY" or "SELL"
    val status: String,                  // "OPEN", "CLOSED", "PENDING"
    val entryPrice: String,
    val exitPrice: String? = null,
    val pnl: String? = null,
    val pnlAmount: Double? = null,
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis(),
    val preTradeContext: String = "",
    val postTradeOutcome: String = "",
    val relayId: String = "PRIMARY-UK-L14",
    val latencyMs: Double = 0.02
)
```

**This matches your MQL5 trade records!** You can reuse this structure.

---

### MarketSignal (from Models.kt)
```kotlin
data class MarketSignal(
    val pair: String,
    val direction: String,
    val status: String,
    val entry: String,
    val stopLoss: String,
    val takeProfits: List<String>,
    val riskReward: String,
    val timeframe: String,
    val signalType: String,
    val confidenceScore: Int,
    val reasoning: String,
    val confluence: List<String>,
    val liquidityEvent: String,
    val newsWarning: String? = null
)
```

**This maps to your EA's signal generation logic!**

---

## Step-by-Step Conversion Process

### 1. Identify the MQL5 Struct
Find the struct in your EA code (e.g., `ASC_UNIFIED_AI.mq5` or `Include/*.mqh`)

### 2. Create Kotlin Data Class
Create a new file or add to existing Models.kt:

```kotlin
// In app/src/main/java/com/asc/markets/data/Models.kt

@Serializable
data class YourNewModel(
    // Add fields here
)
```

### 3. Map Field Types
Use the type mapping table above to convert each field.

### 4. Handle datetime Fields
Convert MQL5 `datetime` to Kotlin `Long` (Unix timestamp in milliseconds):

**MQL5:**
```mql5
datetime eventTime = TimeCurrent();
```

**Kotlin:**
```kotlin
val eventTime: Long = System.currentTimeMillis()
```

### 5. Add Default Values (Optional but Recommended)
```kotlin
@Serializable
data class YourModel(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Double = 0.0,
    val valid: Boolean = false
)
```

### 6. Add Enums if Needed
If your MQL5 code uses enums, create Kotlin enums:

**MQL5:**
```mql5
enum ENUM_DECISION
{
    DECISION_BUY,
    DECISION_SELL,
    DECISION_WAIT
};
```

**Kotlin:**
```kotlin
enum class Decision {
    BUY, SELL, WAIT, EXIT, REDUCE, HOLD
}

// Or use strings directly:
data class Signal(
    val decision: String  // "BUY", "SELL", "WAIT"
)
```

---

## Working with Backend API

Your MyRealApp polls a backend at `http://10.164.138.133:8000`. When converting EA structs, ensure they match your backend's JSON format.

### Example: Backend Response
Your backend `/latest-ai` returns:
```json
{
  "symbol": "BTCUSD",
  "pre_move_ai_score": 0.129,
  "direction": "BUY",
  "confidence": 0.67,
  "regime": "RANGING"
}
```

### Corresponding Kotlin Model
```kotlin
@Serializable
data class AIDecision(
    val symbol: String,
    @SerialName("pre_move_ai_score")
    val preMoveScore: Double,
    val direction: String,
    val confidence: Double,
    val regime: String
)
```

### Parsing with Ktor
```kotlin
val response: HttpResponse = client.get("http://10.164.138.133:8000/latest-ai")
val decisions: List<AIDecision> = response.body()
```

---

## Quick Conversion Checklist

- [ ] Identify MQL5 struct in EA code
- [ ] Create Kotlin data class with same fields
- [ ] Add `@Serializable` annotation
- [ ] Convert `datetime` → `Long`
- [ ] Convert arrays → `List<T>`
- [ ] Add `@SerialName` for snake_case JSON fields
- [ ] Add default values if needed
- [ ] Test with real backend data
- [ ] Update ViewModel/Repository to use new model

---

## Common Patterns You Already Use

Looking at your MyRealApp code, you already follow these best practices:

### 1. Serialization-Ready Models
```kotlin
@Serializable
data class ForexDataPoint(
    @SerialName("time") val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)
```

### 2. UUID Generation for IDs
```kotlin
val id: String = UUID.randomUUID().toString()
```

### 3. Timestamps
```kotlin
val timestamp: Long = System.currentTimeMillis()
```

### 4. Enums for Categories
```kotlin
enum class MarketCategory {
    FOREX, CRYPTO, COMMODITIES, INDICES, STOCK, BONDS, FUTURES
}
```

---

## Example: Full Conversion (Supply/Demand Zones)

Let's do a complete example of adding Supply/Demand zones from your EA to MyRealApp:

### Step 1: Add to Models.kt
```kotlin
// File: app/src/main/java/com/asc/markets/data/Models.kt

@Serializable
data class SupplyDemandZone(
    val id: String = UUID.randomUUID().toString(),
    val topPrice: Double,
    val bottomPrice: Double,
    val startTime: Long,
    val strength: Int,
    val isSupply: Boolean,
    val isBroken: Boolean,
    val symbol: String,                    // Add this for multi-asset support
    val createdAt: Long = System.currentTimeMillis()
)
```

### Step 2: Create Repository/Service
```kotlin
// File: app/src/main/java/com/asc/markets/data/SupplyDemandStore.kt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SupplyDemandStore {
    private val _supplyZones = MutableStateFlow<List<SupplyDemandZone>>(emptyList())
    val supplyZones: StateFlow<List<SupplyDemandZone>> = _supplyZones
    
    private val _demandZones = MutableStateFlow<List<SupplyDemandZone>>(emptyList())
    val demandZones: StateFlow<List<SupplyDemandZone>> = _demandZones
    
    suspend fun fetchZones(symbol: String) {
        // Call backend API
        val response = /* your API call */
        _supplyZones.value = response.supplyZones
        _demandZones.value = response.demandZones
    }
}
```

### Step 3: Use in Composable
```kotlin
@Composable
fun SupplyDemandView(symbol: String) {
    val supplyZones by SupplyDemandStore.supplyZones.collectAsState()
    val demandZones by SupplyDemandStore.demandZones.collectAsState()
    
    LazyColumn {
        item {
            Text("Supply Zones (Resistance)", style = MaterialTheme.typography.titleMedium)
        }
        items(supplyZones.filter { it.symbol == symbol && !it.isBroken }) { zone ->
            ZoneCard(zone)
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Demand Zones (Support)", style = MaterialTheme.typography.titleMedium)
        }
        items(demandZones.filter { it.symbol == symbol && !it.isBroken }) { zone ->
            ZoneCard(zone)
        }
    }
}

@Composable
fun ZoneCard(zone: SupplyDemandZone) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${if (zone.isSupply) "Resistance" else "Support"} Zone")
            Text("Price Range: ${zone.bottomPrice} - ${zone.topPrice}")
            Text("Strength: ${zone.strength} touches")
            Text("Time: ${Date(zone.startTime)}")
        }
    }
}
```

---

## Testing Your Conversions

### 1. Test JSON Parsing
```kotlin
val json = """
{
    "topPrice": 1.2345,
    "bottomPrice": 1.2300,
    "startTime": 1735401600000,
    "strength": 3,
    "isSupply": true,
    "isBroken": false,
    "symbol": "EURUSD"
}
""".trimIndent()

val zone = Json.decodeFromString<SupplyDemandZone>(json)
println(zone)
```

### 2. Test Backend Integration
```kotlin
// In your AIContextService or similar
suspend fun testZoneFetch() {
    try {
        val response = client.get("$AI_BASE_URL/zones/EURUSD")
        val zones: List<SupplyDemandZone> = response.body()
        Log.d("SupplyDemand", "Fetched ${zones.size} zones")
    } catch (e: Exception) {
        Log.e("SupplyDemand", "Error fetching zones", e)
    }
}
```

---

## Summary

| What | MQL5 | Kotlin |
|------|------|--------|
| **Data Structure** | `struct` | `data class` |
| **Serialization** | Manual JSON writing | `@Serializable` annotation |
| **Arrays** | `Type array[]` | `List<Type>` |
| **Time** | `datetime` | `Long` (milliseconds) |
| **Nullable Fields** | Check for default values | `Type?` with `= null` |
| **State Management** | Global variables | `StateFlow` / `MutableStateFlow` |

---

## Next Steps

1. **Identify** which EA structs you want to expose in MyRealApp
2. **Convert** them to Kotlin data classes in `Models.kt`
3. **Update** your backend to serve these structures via REST API
4. **Create** a service/store to fetch and cache the data
5. **Build** UI components to display the data

You already have excellent examples in your codebase (e.g., `AutomatedTrade`, `MarketSignal`, `ForexDataPoint`). Follow the same patterns!

---

## Questions?

If you need help converting a specific struct from your EA, just show me the MQL5 code and I'll provide the exact Kotlin equivalent. Your existing code structure in MyRealApp is already well-organized and follows best practices! 🚀
