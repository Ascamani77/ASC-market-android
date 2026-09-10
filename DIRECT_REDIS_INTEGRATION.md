# Direct Redis Integration - No Middleman

## Architecture

```
AI Feeders (Python) → Redis (Podman) → Android App (Direct Connection)
                         ↑
                    Single source of truth
```

**No API, no HTTP, no caching, no delays. Just pure Redis.**

## Step 1: Add Redis Client to Android

**File: `app/build.gradle.kts`**

Add this dependency:
```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Lettuce Redis Client (Kotlin-friendly, coroutines support)
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
    
    // Or use Jedis (simpler, blocking)
    // implementation("redis.clients:jedis:5.1.0")
}
```

## Step 2: Create Direct Redis Client

**File: `app/src/main/kotlin/com/asc/markets/data/redis/RedisAiClient.kt`**

```kotlin
package com.asc.markets.data.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import android.util.Log

@Serializable
data class RedisAiDecision(
    val asset: String,
    val final_trade_label: String,
    val final_trade_direction: String,
    val final_trade_score: Double,
    val feeder_volatility_score: Double? = null,
    val structure_score: Double? = null,
    val chart_context_score: Double? = null,
    val pre_move_ai_score: Double? = null,
    val feeder_risk_score: Double? = null,
    val final_trading_timestamp: String? = null
)

class RedisAiClient(
    private val host: String = "192.168.1.198", // Your desktop IP
    private val port: Int = 6379
) {
    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var commands: RedisCommands<String, String>? = null
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun connect() {
        try {
            val redisUri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .build()
            
            client = RedisClient.create(redisUri)
            connection = client?.connect()
            commands = connection?.sync()
            
            Log.d("RedisAiClient", "Connected to Redis at $host:$port")
        } catch (e: Exception) {
            Log.e("RedisAiClient", "Failed to connect to Redis: ${e.message}", e)
            throw e
        }
    }

    suspend fun getAiDecisions(): List<RedisAiDecision> = withContext(Dispatchers.IO) {
        try {
            if (commands == null) {
                connect()
            }
            
            // Read directly from Redis hash
            val surfaceKey = "surface:final_trading_surface"
            val allData = commands?.hgetall(surfaceKey) ?: emptyMap()
            
            Log.d("RedisAiClient", "Read ${allData.size} assets from Redis")
            
            // Parse each asset's JSON data
            val decisions = allData.mapNotNull { (asset, jsonData) ->
                try {
                    val decision = json.decodeFromString<RedisAiDecision>(jsonData)
                    
                    // Calculate monitoring confidence from feeder scores
                    val scores = listOfNotNull(
                        decision.feeder_volatility_score,
                        decision.structure_score,
                        decision.chart_context_score,
                        decision.pre_move_ai_score,
                        decision.feeder_risk_score
                    ).filter { it > 0 }
                    
                    val monitoringConfidence = if (scores.isNotEmpty()) {
                        scores.average()
                    } else {
                        decision.final_trade_score
                    }
                    
                    decision.copy(
                        final_trade_score = if (decision.final_trade_score > 0) {
                            decision.final_trade_score
                        } else {
                            monitoringConfidence
                        }
                    )
                } catch (e: Exception) {
                    Log.w("RedisAiClient", "Failed to parse data for $asset: ${e.message}")
                    null
                }
            }
            
            Log.d("RedisAiClient", "Parsed ${decisions.size} valid decisions")
            decisions
            
        } catch (e: Exception) {
            Log.e("RedisAiClient", "Error reading from Redis: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getTTL(key: String = "surface:final_trading_surface"): Long? {
        return try {
            commands?.ttl(key)
        } catch (e: Exception) {
            Log.e("RedisAiClient", "Error getting TTL: ${e.message}")
            null
        }
    }
    
    fun isConnected(): Boolean {
        return connection?.isOpen == true
    }

    fun disconnect() {
        try {
            connection?.close()
            client?.shutdown()
            Log.d("RedisAiClient", "Disconnected from Redis")
        } catch (e: Exception) {
            Log.e("RedisAiClient", "Error disconnecting: ${e.message}")
        }
    }
}
```

## Step 3: Replace AiRepository with Direct Redis Access

**File: `app/src/main/kotlin/com/asc/markets/data/repository/AiRepository.kt`**

```kotlin
package com.asc.markets.data.repository

import com.asc.markets.data.redis.RedisAiClient
import com.asc.markets.data.remote.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

class AiRepository {
    private val redisClient = RedisAiClient(
        host = "192.168.1.198", // Your desktop IP where Podman runs
        port = 6379
    )
    
    private val _deployments = MutableStateFlow<LatestDeploymentsResponse?>(null)
    val deployments: StateFlow<LatestDeploymentsResponse?> = _deployments.asStateFlow()

    init {
        // Connect to Redis on init
        try {
            redisClient.connect()
            Log.d("AiRepository", "Redis connected: ${redisClient.isConnected()}")
        } catch (e: Exception) {
            Log.e("AiRepository", "Redis connection failed: ${e.message}", e)
        }
    }

    suspend fun fetchLatestDeployments(): Result<LatestDeploymentsResponse> {
        return try {
            // Read DIRECTLY from Redis - no HTTP, no API
            val redisDecisions = redisClient.getAiDecisions()
            
            // Convert Redis format to app format
            val finalDecisions = redisDecisions.map { redisDecision ->
                FinalDecisionItem(
                    asset_1 = redisDecision.asset,
                    journal_label = redisDecision.final_trade_label,
                    journal_direction = redisDecision.final_trade_direction,
                    journal_score = redisDecision.final_trade_score,
                    monitoring_confidence = redisDecision.final_trade_score,
                    pre_move_ai_score = redisDecision.pre_move_ai_score ?: redisDecision.final_trade_score,
                    journal_timestamp = redisDecision.final_trading_timestamp,
                    feeder_volatility_score = redisDecisions.feeder_volatility_score,
                    structure_score = redisDecision.structure_score,
                    chart_context_score = redisDecision.chart_context_score,
                    feeder_risk_score = redisDecision.feeder_risk_score
                )
            }
            
            val response = LatestDeploymentsResponse(
                success = true,
                last_updated = java.time.Instant.now().toString(),
                count = finalDecisions.size,
                final_decision = finalDecisions
            )
            
            _deployments.value = response
            Log.d("AiRepository", "Loaded ${finalDecisions.size} decisions directly from Redis")
            
            Result.success(response)
            
        } catch (e: Exception) {
            Log.e("AiRepository", "Error reading from Redis: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Stub methods (not needed with direct Redis)
    suspend fun runAiPipeline() = Result.success(RunAiResponse(false, "Not needed - AI runs independently"))
    suspend fun updateMarketData(request: MarketUpdateRequest) = Result.success(MarketUpdateResponse(false))
    suspend fun syncCalendarEvents(payload: CalendarEventsPayload) = Result.success(CalendarSyncResponse(false))
    suspend fun healthCheck() = Result.success(mapOf(
        "status" to "connected",
        "redis" to redisClient.isConnected(),
        "ttl" to (redisClient.getTTL() ?: -1)
    ))
    suspend fun getScalpingSignals() = Result.success(ScalpingSignalsResponse(true, "Direct Redis", emptyList()))
    suspend fun getSwingSignals() = Result.success(SwingSignalsResponse(true, "Direct Redis", emptyList()))
    
    fun disconnect() {
        redisClient.disconnect()
    }
}
```

## Step 4: Network Permissions

**File: `app/src/main/AndroidManifest.xml`**

Ensure you have internet permission:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

And allow cleartext traffic for local IP:
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

## Step 5: Test Redis Connection

Add this test function to verify:

```kotlin
suspend fun testRedisConnection(): String {
    return try {
        val decisions = redisClient.getAiDecisions()
        "✓ Connected! Found ${decisions.size} assets. TTL: ${redisClient.getTTL()}s"
    } catch (e: Exception) {
        "✗ Failed: ${e.message}"
    }
}
```

## How It Works Now

1. **Python AI feeders** run every 2-5 min → write to Redis
2. **Android app** reads directly from Redis (no API)
3. **Same Redis** (asc-redis in Podman)
4. **Real-time data** - no caching, no delays

## Benefits

✅ No API server needed
✅ No HTTP overhead
✅ No caching issues
✅ Direct access to Redis
✅ Faster (local network only)
✅ Simpler architecture
✅ One less thing to break

## Your AI feeders stay the same

They already write to Redis. Nothing changes on the Python side.

---

**This is pure, direct connection. No middleman. No bullshit.**
