# Quick Start: AI Integration with Pepperstone + Binance

## ✅ What Was Done

Your AI backend now receives market data from **both sources**:
1. **Binance** - Crypto pairs (BTCUSDT, ETHUSDT)
2. **Pepperstone** - Forex pairs (EURUSD, GBPUSD, USDJPY, etc.)

## 🚀 How to Test

### Step 1: Verify AI Backend is Running

Your AI backend is already running at `http://10.164.138.133:8000`

Check health:
```bash
curl http://10.164.138.133:8000/health
```

### Step 2: Run Verification Script

```bash
cd c:\Users\HP\Documents\NEW_ASC
python verify_pepperstone_integration.py
```

This will show:
- ✅ Which data sources are publishing to Redis
- 📊 Recent ticks from each source
- 🔴 Live monitoring of incoming data

### Step 3: Connect Pepperstone in Android App

1. Open your Android app
2. Go to Trading/Stream screen
3. Select **Pepperstone** as chart feed
4. Connect to cTrader bridge
5. Subscribe to forex symbols (EURUSD, GBPUSD, etc.)

### Step 4: Verify Data Flow

Watch the verification script output - you should see:
```
🔵 12:34:56 | pepperstone_ctrader  | EURUSD     |      1.08510
🟢 12:34:57 | binance              | BTCUSDT    |  79170.00000
🔵 12:34:58 | pepperstone_ctrader  | GBPUSD     |      1.26340
```

### Step 5: Check AI Decisions

```bash
curl http://10.164.138.133:8000/latest-ai
```

You should see AI decisions for both crypto AND forex pairs.

## 📱 Android App Changes

### Modified Files:
1. **PepperstoneChartService.kt** - Now publishes to Redis
2. **TradingApp.kt** - Passes Redis config to Pepperstone service

### No Breaking Changes:
- Market Overview page works exactly as before
- All existing functionality preserved
- Redis publishing happens in background

## 🔧 Configuration

### Redis Settings

The app uses these defaults (can be changed in SharedPreferences):
- **Host**: `10.164.138.133` (your PC's IP)
- **Port**: `6379`
- **Stream**: `market.ticks.stream`

### To Change Redis Host:

```kotlin
// In Android app settings or SharedPreferences
val prefs = context.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
prefs.edit()
    .putString("redis_host", "YOUR_IP_HERE")
    .putInt("redis_port", 6379)
    .apply()
```

## 🎯 What the AI Sees Now

### Data Sources:
```
┌─────────────────┬──────────────────┬─────────────────┐
│ Source          │ Asset Class      │ Symbols         │
├─────────────────┼──────────────────┼─────────────────┤
│ Binance         │ Crypto           │ BTCUSDT, ETHUSDT│
│ Pepperstone     │ Forex            │ EURUSD, GBPUSD, │
│ (cTrader)       │                  │ USDJPY, etc.    │
└─────────────────┴──────────────────┴─────────────────┘
```

### AI Pipeline:
```
Market Data → Redis Stream → AI Backend → Analysis → Decisions
```

## 🐛 Troubleshooting

### Problem: No Pepperstone data in Redis

**Solution:**
1. Check cTrader bridge is running
2. Verify connection in Android app
3. Make sure symbols are subscribed
4. Check Android logs for errors:
   ```
   adb logcat | grep PepperstoneChartService
   ```

### Problem: Redis connection fails

**Solution:**
1. Verify Redis is running: `redis-cli ping`
2. Check firewall allows port 6379
3. Verify IP address is correct
4. Test connection: `redis-cli -h 10.164.138.133 ping`

### Problem: AI not processing data

**Solution:**
1. Check AI backend logs for errors
2. Verify environment variables:
   - `LIVE_MODE=true`
   - `REDIS_USE_STREAMS=true`
3. Restart AI backend if needed

## 📊 Monitoring Commands

### Check Redis Stream Length:
```bash
redis-cli XLEN market.ticks.stream
```

### View Recent Ticks:
```bash
redis-cli XREVRANGE market.ticks.stream + - COUNT 10
```

### Monitor Live:
```bash
redis-cli XREAD BLOCK 0 STREAMS market.ticks.stream $
```

### Check market.latest Hash:
```bash
redis-cli HGETALL market.latest
```

## 🎉 Success Indicators

You'll know it's working when:

1. ✅ Verification script shows both Binance and Pepperstone ticks
2. ✅ AI backend logs show "Dequeued live message" with both sources
3. ✅ `/latest-ai` endpoint returns decisions for forex pairs
4. ✅ Android app shows live quotes updating
5. ✅ Redis stream length is growing

## 📚 Documentation

- **Full Integration Guide**: `AI_DATA_SOURCE_INTEGRATION.md`
- **Implementation Details**: `AI_PEPPERSTONE_INTEGRATION_COMPLETE.md`
- **Verification Script**: `verify_pepperstone_integration.py`

## 🔄 Next Steps

1. **Test the integration** - Run verification script
2. **Monitor performance** - Watch for any lag or errors
3. **Expand symbols** - Add more forex pairs as needed
4. **Add more sources** - Apply same pattern to MT5, Deriv, etc.
5. **Optimize** - Adjust Redis pool settings if needed

## 💡 Tips

- Keep the verification script running while testing
- Check both Android logs and AI backend logs
- Start with a few symbols, then expand
- Monitor Redis memory usage if streaming many symbols
- Use Redis persistence if you need historical data

---

**Ready to test!** Run the verification script and connect Pepperstone in your app. 🚀
