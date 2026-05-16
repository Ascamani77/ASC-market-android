# Binance Position Sync Debug Guide

## Problem
Positions in app don't match positions on Binance Futures.

## Quick Debug Steps

### 1. Check Logcat for Position Data
Filter by tag `BinanceBalance`:
```bash
adb logcat -s BinanceBalance
```

Look for:
```
BinanceBalance: Trading Mode: DEMO or LIVE
BinanceBalance: Positions: X
BinanceBalance:   BTCUSDT: LONG 0.001 @ 45000.00, PnL: 1.50, Leverage: 10x
```

### 2. Check User Data Stream Connection
Filter by tag `BinanceFutures`:
```bash
adb logcat -s BinanceFutures
```

Look for:
```
BinanceFutures: User data stream started, listenKey: xxx...
BinanceFutures: User data stream connected
BinanceFutures: ACCOUNT_UPDATE: X positions changed
```

### 3. Verify Trading Mode
- **Demo** = `https://demo-fapi.binance.com` (testnet)
- **Live** = `https://fapi.binance.com` (mainnet)

Check which API keys you have configured:
- `BINANCE_DEMO_API_KEY` / `BINANCE_DEMO_SECRET_KEY` → Testnet
- `BINANCE_API_KEY` / `BINANCE_SECRET_KEY` → Mainnet

### 4. Manual API Test
Test position endpoint directly:

**For LIVE:**
```bash
curl -X GET "https://fapi.binance.com/fapi/v2/positionRisk?timestamp=$(date +%s)000&signature=YOUR_SIGNATURE" -H "X-MBX-APIKEY: YOUR_API_KEY"
```

**For DEMO:**
```bash
curl -X GET "https://demo-fapi.binance.com/fapi/v2/positionRisk?timestamp=$(date +%s)000&signature=YOUR_SIGNATURE" -H "X-MBX-APIKEY: YOUR_DEMO_API_KEY"
```

### 5. Position Mode Check
Check your position mode (One-way vs Hedge):
```bash
curl -X GET "https://fapi.binance.com/fapi/v1/positionSide/dual?timestamp=$(date +%s)000&signature=YOUR_SIGNATURE" -H "X-MBX-APIKEY: YOUR_API_KEY"
```

Response:
- `{"dualSidePosition":false}` = One-way mode (positionSide = BOTH)
- `{"dualSidePosition":true}` = Hedge mode (positionSide = LONG or SHORT)

## Common Issues

### Issue 1: Demo vs Live Confusion
- You're viewing Live positions on Binance website but app is connected to Demo
- Solution: Check `binanceTradingMode` in app settings

### Issue 2: Position Mode Mismatch
- Orders placed with wrong `positionSide` parameter
- In one-way mode, use `positionSide=BOTH`
- In hedge mode, use `positionSide=LONG` or `positionSide=SHORT`

### Issue 3: WebSocket Not Receiving Updates
- ListenKey expires after 60 minutes if not renewed
- App sends keepalive every 30 minutes
- Check for `listenKeyExpired` event in logs

### Issue 4: Positions Not Refreshing
- Pull-to-refresh or restart app to trigger fresh API call
- Positions are fetched in `LaunchedEffect` on startup

## API Endpoints Used

| Purpose | Endpoint |
|---------|----------|
| Account Info | `GET /fapi/v2/account` |
| Position Info | `GET /fapi/v2/positionRisk` |
| Open Orders | `GET /fapi/v1/openOrders` |
| ListenKey | `POST /fapi/v1/listenKey` |
| User Data Stream | `wss://fstream.binance.com/private/ws/{listenKey}` |

## WebSocket Events

| Event | Description |
|-------|-------------|
| `ACCOUNT_UPDATE` | Balance and position changes |
| `ORDER_TRADE_UPDATE` | Order status changes |
| `listenKeyExpired` | Need to restart stream |

## Data Flow

```
1. App starts → getAccountInfo() → positions list populated
2. User Data Stream connects → WebSocket receives ACCOUNT_UPDATE
3. onPositionUpdate callback → positions list updated in real-time
4. Positions displayed in UI → PositionsTab component
```

## Next Steps
1. Check logcat output when app loads
2. Verify which trading mode you're in
3. Compare API response with Binance website
