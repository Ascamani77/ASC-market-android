# AI Data Source Integration Guide

## Current Status

### ✅ What's Working
- **AI Backend**: Running on `http://10.164.138.133:8000`
- **Redis Streams**: AI consumes from `market.ticks.stream`
- **Binance Data**: BinanceWebSocketManager publishes crypto ticks to Redis
- **Market Overview**: Uses MarketDataStore (Pepperstone) + BinanceDataStore

### ❌ What's Missing
- **Pepperstone/Forex data NOT being published to Redis** for AI consumption
- AI only sees Binance crypto data, missing forex pairs from Pepperstone

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID APP                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │ Pepperstone      │         │ Binance          │         │
│  │ (cTrader Bridge) │         │ WebSocket        │         │
│  └────────┬─────────┘         └────────┬─────────┘         │
│           │                            │                    │
│           │ Updates                    │ Updates +          │
│           │ MarketDataStore            │ Publishes to Redis │
│           ▼                            ▼                    │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │ MarketDataStore  │         │ BinanceDataStore │         │
│  │ (Forex/Stocks)   │         │ (Crypto USDT)    │         │
│  └──────────────────┘         └──────────────────┘         │
│           │                            │                    │
│           └────────────┬───────────────┘                    │
│                        ▼                                    │
│              Market Overview Page                           │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Only Binance publishes
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      REDIS                                   │
│  Stream: market.ticks.stream                                │
│  Hash: market.latest                                        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│                   AI BACKEND (Python)                        │
│  - Consumes from Redis Streams                              │
│  - Runs AI pipeline on market data                          │
│  - Exposes /latest-ai endpoint                              │
└─────────────────────────────────────────────────────────────┘
```

## Solution: Add Pepperstone Publisher

### Option 1: Modify PepperstoneChartService (Recommended)

Add Redis publishing to `PepperstoneChartService.kt` similar to `BinanceWebSocketManager`:

```kotlin
// In PepperstoneChartService.kt
private var jedisPool: JedisPool? = null
private val streamName: String = "market.ticks.stream"
private val fieldName: String = "data"

private fun setupRedis() {
    try {
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = 10
            maxIdle = 5
            minIdle = 1
            jmxEnabled = false
        }
        jedisPool = JedisPool(poolConfig, redisHost, redisPort)
        Log.i(TAG, "Redis Pool initialized for Pepperstone")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Redis Pool: ${e.message}")
    }
}

private fun publishTickToRedis(quote: ChartQuote) {
    scope.launch(Dispatchers.IO) {
        try {
            jedisPool?.resource?.use { jedis ->
                val tick = MarketTick(
                    ts = quote.time,
                    symbol = quote.symbol,
                    bid = quote.bid.toDouble(),
                    ask = quote.ask.toDouble(),
                    last = quote.lastPrice.toDouble(),
                    volume = 0.0, // cTrader doesn't provide volume in quotes
                    source = "pepperstone_ctrader"
                )
                val tickJson = Json.encodeToString(tick)
                val params = mapOf(fieldName to tickJson)
                jedis.xadd(streamName, XAddParams.xAddParams(), params)
                Log.d(TAG, "Published Pepperstone tick to Redis: ${quote.symbol}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Redis publish error: ${e.message}")
        }
    }
}

// Call in onQuoteUpdate:
private fun handleQuote(quote: ChartQuote) {
    publishTickToRedis(quote)  // Add this line
    mainHandler.post { onQuoteUpdate(quote) }
}
```

### Option 2: Backend Proxy Endpoint (Alternative)

If you don't want direct Redis access from Android, add an endpoint to `ai_api.py`:

```python
@app.post("/publish_tick")
async def publish_tick(tick: dict, x_api_key: str = Header(None)):
    """Accept ticks from Android app and publish to Redis stream."""
    # Optional: validate API key
    if x_api_key != os.getenv("PUBLISH_API_KEY"):
        raise HTTPException(status_code=401, detail="Invalid API key")
    
    redis_url = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    r = await aioredis.from_url(redis_url, encoding="utf-8", decode_responses=True)
    
    try:
        stream_name = os.getenv("LIVE_STREAM_NAME", "market.ticks.stream")
        await r.xadd(stream_name, {"data": json.dumps(tick)})
        return {"ok": True}
    finally:
        await r.close()
```

Then modify `PepperstoneChartService` to POST to this endpoint instead of direct Redis.

## Implementation Steps

### Step 1: Add Redis Dependencies to Android

In `app/build.gradle.kts`:
```kotlin
dependencies {
    // ... existing dependencies
    implementation("redis.clients:jedis:5.1.0")  // Already added for Binance
}
```

### Step 2: Modify PepperstoneChartService

Add the Redis publishing code shown in Option 1 above.

### Step 3: Configure Redis Connection

In `ForexViewModel.kt`, ensure Pepperstone service gets Redis config:

```kotlin
private val pepperstoneChartService: PepperstoneChartService by lazy {
    val prefs = getApplication<Application>().getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
    val redisHost = prefs.getString("redis_host", "10.164.138.133") ?: "10.164.138.133"
    val redisPort = prefs.getInt("redis_port", 6379)
    
    PepperstoneChartService(
        host = cTraderHost,
        port = cTraderPort,
        redisHost = redisHost,
        redisPort = redisPort,
        onQuoteUpdate = { /* ... */ }
    )
}
```

### Step 4: Verify Data Flow

1. **Check Redis Stream**:
   ```bash
   redis-cli
   XLEN market.ticks.stream
   XREAD COUNT 10 STREAMS market.ticks.stream 0
   ```

2. **Check AI Backend Logs**:
   Look for "Dequeued live message" with `source: pepperstone_ctrader`

3. **Test AI Endpoint**:
   ```bash
   curl http://10.164.138.133:8000/latest-ai
   ```

## Data Format

### MarketTick Schema
```kotlin
@Serializable
data class MarketTick(
    val ts: Long,              // Timestamp in milliseconds
    val symbol: String,        // e.g., "EURUSD", "BTCUSDT"
    val bid: Double,
    val ask: Double,
    val last: Double,          // Last traded price (mid for forex)
    val volume: Double,        // 0.0 for forex quotes
    val source: String         // "binance", "pepperstone_ctrader", etc.
)
```

### Redis Stream Entry
```json
{
  "data": "{\"ts\":1778890430023,\"symbol\":\"EURUSD\",\"bid\":1.0850,\"ask\":1.0852,\"last\":1.0851,\"volume\":0.0,\"source\":\"pepperstone_ctrader\"}"
}
```

## Current Data Sources

### Binance (✅ Publishing to Redis)
- **Symbols**: BTCUSDT, ETHUSDT (crypto pairs)
- **Source**: `wss://stream.binance.com:9443`
- **Update Frequency**: Real-time WebSocket
- **Publisher**: `BinanceWebSocketManager.kt`

### Pepperstone (❌ NOT Publishing to Redis)
- **Symbols**: EURUSD, GBPUSD, USDJPY, etc. (forex pairs)
- **Source**: cTrader Bridge WebSocket
- **Update Frequency**: Real-time WebSocket
- **Publisher**: None (needs to be added)

## Testing Checklist

- [ ] Pepperstone quotes appear in Redis stream
- [ ] AI backend logs show Pepperstone ticks
- [ ] `/latest-ai` endpoint includes forex pairs
- [ ] Market Overview page still works (no regression)
- [ ] AI decisions include both crypto and forex

## Troubleshooting

### Redis Connection Fails
- Verify Redis is running: `redis-cli ping`
- Check firewall allows port 6379
- Verify IP address matches your machine's IP

### No Pepperstone Ticks in Redis
- Check cTrader bridge is connected
- Verify `publishTickToRedis()` is being called
- Check Android logs for Redis errors

### AI Not Processing Ticks
- Check `LIVE_MODE=true` in AI backend
- Verify `REDIS_USE_STREAMS=true`
- Check AI backend logs for consumer errors

## Next Steps

1. Implement Option 1 (modify PepperstoneChartService)
2. Test with a single forex pair (e.g., EURUSD)
3. Verify AI receives and processes the data
4. Expand to all forex pairs
5. Monitor performance and adjust buffer sizes if needed
