# AI Integration - Quick Reference Card

## 📊 Asset Distribution

```
┌─────────────────────────────────────────────────────────────┐
│                    PEPPERSTONE (22 assets)                   │
├─────────────────────────────────────────────────────────────┤
│ Forex (5)      │ EURUSD, GBPUSD, USDJPY, USDCHF, AUDUSD    │
│ Crypto USD (2) │ BTCUSD, ETHUSD                             │
│ Stocks (5)     │ NVDA, TSLA, AAPL, MSFT, AMZN              │
│ Commodities (4)│ XAUUSD, XAGUSD, USOIL, UKOIL              │
│ Indices (4)    │ DXY, NAS100, US30, SPX500                  │
│ Bonds (2)      │ US10Y, US02Y                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     BINANCE (2 assets)                       │
├─────────────────────────────────────────────────────────────┤
│ Crypto USDT (2)│ BTCUSDT, ETHUSDT                           │
└─────────────────────────────────────────────────────────────┘

TOTAL: 24 assets
```

## 🔑 Key Distinction

```
BTCUSD  (Pepperstone) ≠ BTCUSDT (Binance)
ETHUSD  (Pepperstone) ≠ ETHUSDT (Binance)

These are DIFFERENT assets from DIFFERENT sources!
```

## 🚀 Quick Test Commands

### 1. Verify Redis Connection
```bash
redis-cli -h 10.164.138.133 ping
```

### 2. Check Stream Length
```bash
redis-cli XLEN market.ticks.stream
```

### 3. View Recent Ticks
```bash
redis-cli XREVRANGE market.ticks.stream + - COUNT 10
```

### 4. Run Verification Script
```bash
cd c:\Users\HP\Documents\NEW_ASC
python verify_pepperstone_integration.py
```

### 5. Check AI Health
```bash
curl http://10.164.138.133:8000/health
```

### 6. Get AI Decisions
```bash
curl http://10.164.138.133:8000/latest-ai
```

## 📱 Android App Setup

### Connect Pepperstone
1. Open app → Trading/Stream screen
2. Select **Pepperstone** as chart feed
3. Connect to cTrader bridge
4. Subscribe to symbols

### Check Logs
```bash
adb logcat | grep PepperstoneChartService
```

Look for:
```
PepperstoneChartService: Redis Pool initialized at 10.164.138.133:6379
PepperstoneChartService: Published Pepperstone tick to Redis: EURUSD @ 1.0851
```

## 🔍 Verification Checklist

- [ ] Redis is running (`redis-cli ping`)
- [ ] AI backend is running (`curl http://10.164.138.133:8000/health`)
- [ ] Pepperstone is connected in app
- [ ] Binance WebSocket is active
- [ ] Stream has data (`XLEN market.ticks.stream > 0`)
- [ ] Verification script shows both sources
- [ ] Both BTCUSD and BTCUSDT appear (separately!)
- [ ] All 6 asset classes represented
- [ ] `/latest-ai` returns 23 assets

## 🎯 Expected Output

### Verification Script
```
✅ Binance data: PRESENT
   Symbols: BTCUSDT, ETHUSDT

✅ Pepperstone data: PRESENT
   Symbols: AAPL, AMZN, AUDUSD, BTCUSD, DXY, ETHUSD, EURUSD, ...

   Asset Class Coverage:
   ✅ FOREX          : 5/5 (EURUSD, GBPUSD, USDJPY, USDCHF, AUDUSD)
   ✅ CRYPTO_USD     : 2/2 (BTCUSD, ETHUSD)
   ✅ STOCKS         : 5/5 (NVDA, TSLA, AAPL, MSFT, AMZN)
   ✅ COMMODITIES    : 4/4 (XAUUSD, XAGUSD, USOIL, UKOIL)
   ✅ INDICES        : 4/4 (DXY, NAS100, US30, SPX500)
   ✅ BONDS          : 2/2 (US10Y, US02Y)

   Crypto Asset Distinction:
   ✅ BTCUSD (Pepperstone) and BTCUSDT (Binance) - CORRECT!
   ✅ ETHUSD (Pepperstone) and ETHUSDT (Binance) - CORRECT!

🎉 SUCCESS: Both Binance and Pepperstone data are flowing to AI!
   Total assets: 22 (Pepperstone) + 2 (Binance)
```

### AI Backend Logs
```
INFO:ai_api:Dequeued live message: {'ts': 1778890430023, 'symbol': 'EURUSD', 'bid': 1.0850, 'ask': 1.0852, 'last': 1.0851, 'volume': 0.0, 'source': 'pepperstone_ctrader'}
INFO:ai_api:Dequeued live message: {'ts': 1778890430024, 'symbol': 'BTCUSD', 'bid': 67420.0, 'ask': 67425.0, 'last': 67422.5, 'volume': 0.0, 'source': 'pepperstone_ctrader'}
INFO:ai_api:Dequeued live message: {'ts': 1778890430030, 'symbol': 'BTCUSDT', 'bid': 79169.99, 'ask': 79170.0, 'last': 79170.0, 'volume': 17316.96, 'source': 'binance'}
```

## 🐛 Common Issues

### Issue: Only Binance data, no Pepperstone
**Fix**: Connect Pepperstone in Android app, subscribe to symbols

### Issue: BTCUSD converted to BTCUSDT
**Fix**: Check `ai_api.py` - should NOT have conversion logic

### Issue: Redis connection fails
**Fix**: Check firewall, verify IP address, test with `redis-cli`

### Issue: No data in stream
**Fix**: Check both Pepperstone and Binance are connected and publishing

## 📚 Full Documentation

- **AI_INTEGRATION_FINAL_SUMMARY.md** - Complete summary
- **AI_DATA_SOURCE_CLARIFICATION.md** - Asset breakdown
- **verify_pepperstone_integration.py** - Verification script

## 💡 Pro Tips

1. Keep verification script running while testing
2. Monitor both Android logs and AI backend logs
3. Start with a few symbols, then expand
4. Check Redis memory if streaming many symbols
5. Both BTCUSD and BTCUSDT should coexist!

---

**Your AI now analyzes 23 assets across all major asset classes!** 🎉
