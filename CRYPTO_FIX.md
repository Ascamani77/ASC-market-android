# Crypto Symbol Fix

## Problem
The Crypto tab in Market Overview was showing 3 USDT symbols instead of the correct mix:
- BTCUSDT (Binance) ✓
- ETHUSDT (Binance) ✓
- BTCUSDT (duplicate - should be BTCUSD from Pepperstone) ✗
- ETHUSD (Pepperstone) ✓

## Solution

### Data Sources
1. **Binance** → BTC/USDT and ETH/USDT (Tether pairs)
2. **Pepperstone cTrader** → BTC/USD and ETH/USD (US Dollar pairs)

### Changes Made

#### 1. Bridge Configuration (`ctrader_bridge.py`)
Added BTC/USD and ETH/USD mappings:
```python
"BTCUSD": ["BTCUSD", "BTC/USD", "BITCOIN"],
"ETHUSD": ["ETHUSD", "ETH/USD", "ETHEREUM"],
```

Added CRYPTO category:
```python
"BTCUSD": "CRYPTO",
"ETHUSD": "CRYPTO",
```

#### 2. App Configuration (`ForexViewModel.kt`)
Updated cTrader symbol filter to include CRYPTO category:
```kotlin
val cTraderSymbols = FOREX_PAIRS
    .filter { pair ->
        !pair.symbol.endsWith("/USDT") &&  // ← Excludes USDT pairs (Binance)
            (pair.category == MarketCategory.FOREX ||
                pair.category == MarketCategory.COMMODITIES ||
                pair.category == MarketCategory.INDICES ||
                pair.category == MarketCategory.STOCK ||
                pair.category == MarketCategory.CRYPTO)  // ← Added CRYPTO
    }
```

The key filter `!pair.symbol.endsWith("/USDT")` ensures:
- **BTC/USDT** and **ETH/USDT** → Binance WebSocket
- **BTC/USD** and **ETH/USD** → Pepperstone cTrader

#### 3. Data Definitions (`Constants.kt`)
Already correctly defined:
```kotlin
// Binance USDT pairs (for most of the app)
ForexPair("BTC/USDT", "Bitcoin / Tether", ...)
ForexPair("ETH/USDT", "Ethereum / Tether", ...)

// Pepperstone cTrader USD pairs
ForexPair("BTC/USD", "Bitcoin / US Dollar", ...)
ForexPair("ETH/USD", "Ethereum / US Dollar", ...)
```

## Result

### Crypto Tab Now Shows:
1. **BTCUSDT** - Bitcoin / Tether (Binance)
2. **ETHUSDT** - Ethereum / Tether (Binance)
3. **BTCUSD** - Bitcoin / US Dollar (Pepperstone) ← Fixed!
4. **ETHUSD** - Ethereum / US Dollar (Pepperstone)

### Data Flow:
```
Binance WebSocket
  ↓
BTC/USDT, ETH/USDT
  ↓
Market Overview (Crypto Tab)

Pepperstone cTrader Bridge
  ↓
BTC/USD, ETH/USD
  ↓
Market Overview (Crypto Tab)
```

## How to Apply

### 1. Restart Bridge
```powershell
.\start_ctrader_bridge.ps1
```

Expected output:
```
[cTrader] subscribed: Subscribed to 19 Pepperstone cTrader symbols
```
(Previously 17, now 19 with BTC/USD and ETH/USD added)

### 2. Rebuild App
In Android Studio:
- Build → Clean Project
- Build → Rebuild Project
- Run → Run 'app'

### 3. Verify
Open the app → Markets → Crypto tab

You should now see:
- 2 USDT pairs (Binance)
- 2 USD pairs (Pepperstone)
- No duplicates

## Notes

- **USDT pairs** track Tether-based trading (typically higher volume)
- **USD pairs** track actual US Dollar trading (typically more stable)
- Both provide valuable market insights
- Pepperstone USD pairs are available 24/7 in demo mode
- Binance USDT pairs have real-time data

## Troubleshooting

### Still Seeing 3 USDT Symbols
1. Make sure you rebuilt the app (not just reinstalled)
2. Clear app data: Settings → Apps → Your App → Clear Data
3. Check ForexViewModel logs for "cTrader startup symbols"

### BTC/USD or ETH/USD Not Updating
1. Check bridge console for "BTCUSD" and "ETHUSD" in subscribed symbols
2. Verify Pepperstone has these symbols (they do in demo)
3. Check if crypto market is active (24/7 for crypto)
