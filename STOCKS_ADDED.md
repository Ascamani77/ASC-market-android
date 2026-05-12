# Stock Symbols Added to cTrader Bridge

## Summary
Successfully added 5 US stock symbols to the Pepperstone cTrader bridge integration.

## Stocks Added

| Symbol | Company Name | Pepperstone Symbol |
|--------|-------------|-------------------|
| AAPL   | Apple Inc.  | AAPL.US           |
| MSFT   | Microsoft Corporation | MSFT.US |
| AMZN   | Amazon.com Inc. | AMZN.US      |
| NVDA   | NVIDIA Corporation | NVDA.US   |
| TSLA   | Tesla Inc.  | TSLA.US           |

## Changes Made

### 1. Bridge Configuration (`ctrader_bridge.py`)
Added stock symbol mappings:
```python
"AAPL": ["AAPL.US", "AAPL", "APPLE"],
"MSFT": ["MSFT.US", "MSFT", "MICROSOFT"],
"AMZN": ["AMZN.US", "AMZN", "AMAZON"],
"NVDA": ["NVDA.US", "NVDA", "NVIDIA"],
"TSLA": ["TSLA.US", "TSLA", "TESLA"],
```

Added STOCKS category:
```python
"AAPL": "STOCKS",
"MSFT": "STOCKS",
"AMZN": "STOCKS",
"NVDA": "STOCKS",
"TSLA": "STOCKS",
```

### 2. App Configuration (`ForexViewModel.kt`)
Updated cTrader symbol filter to include STOCK category:
```kotlin
val cTraderSymbols = FOREX_PAIRS
    .filter { pair ->
        !pair.symbol.endsWith("/USDT") &&
            (pair.category == MarketCategory.FOREX ||
                pair.category == MarketCategory.COMMODITIES ||
                pair.category == MarketCategory.INDICES ||
                pair.category == MarketCategory.STOCK)  // ← Added STOCK
    }
```

### 3. Stock Definitions
Stocks were already defined in `Constants.kt`:
- NVDA - NVIDIA Corp.
- TSLA - Tesla Inc.
- AAPL - Apple Inc.
- MSFT - Microsoft Corp.
- AMZN - Amazon.com Inc.

## How to Use

### 1. Restart the Bridge
```powershell
.\start_ctrader_bridge.ps1
```

You should see:
```
[cTrader] subscribed: Subscribed to 17 Pepperstone cTrader symbols
```
(Previously 12, now 17 with the 5 stocks added)

### 2. Rebuild and Install App
In Android Studio:
- Build → Clean Project
- Build → Rebuild Project
- Run → Run 'app'

### 3. Verify Stock Data
The app will now receive live stock prices from Pepperstone cTrader:
- Real-time price updates
- Bid/Ask spreads
- Price changes
- Historical data

## Complete Symbol List

The bridge now provides live data for:

**Forex (5):**
- EURUSD, GBPUSD, USDJPY, USDCHF, AUDUSD

**Stocks (5):**
- AAPL, MSFT, AMZN, NVDA, TSLA

**Commodities (4):**
- XAUUSD (Gold), XAGUSD (Silver), USOIL (Crude Oil), DXY (Dollar Index)

**Indices (3):**
- NAS100 (Nasdaq 100), US30 (Dow Jones), SPX500 (S&P 500)

**Total: 17 symbols**

## Notes

- Stock symbols use the `.US` suffix in Pepperstone (e.g., `AAPL.US`)
- The bridge automatically maps app symbols to broker symbols
- Stock prices are available during US market hours (9:30 AM - 4:00 PM ET)
- After-hours trading may have limited liquidity
- Demo account provides delayed quotes (typically 15-20 minutes)

## Troubleshooting

### Stocks Not Showing Data
1. Check bridge console for subscription confirmation
2. Verify US market is open
3. Check if demo account has stock access
4. Look for "Unresolved cTrader symbols" messages

### Bridge Shows "Unresolved"
If you see stocks in the unresolved list:
- Verify the Pepperstone symbol name (use `list_ctrader_symbols.py`)
- Update the symbol mapping in `ctrader_bridge.py`
- Restart the bridge

### App Not Receiving Stock Updates
1. Rebuild the app (Clean + Rebuild)
2. Check ForexViewModel logs for "cTrader startup symbols"
3. Verify STOCK category is included in the filter
4. Check network connectivity between phone and bridge
