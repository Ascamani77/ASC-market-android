# Oil Symbol Update - Pepperstone Naming

## Summary
Updated oil commodity symbols to match Pepperstone's cTrader naming convention for live data compatibility.

## Changes Made

### 1. Constants.kt - Primary Symbol Names
- **USOIL** → **Crude-F** (WTI Crude Oil)
- **UKOIL** → **Brent-F** (Brent Crude Oil)

### 2. ctrader_bridge.py - Symbol Mapping
Added UKOIL mapping:
```python
"UKOIL": ["UKOIL", "Brent-F", "BRENTCMDUSD", "BRENT", "UK OIL", "UKOil"]
```

Updated USOIL mapping already included "Crude-F"

### 3. Files Updated

#### Core Data Files
- `Constants.kt` - Changed display symbols to Crude-F and Brent-F
- `ctrader_bridge.py` - Added UKOIL mapping, updated category

#### UI Components
- `AiScoreGuideScreen.kt` - Updated commodity list display
- `SettingsPanel.kt` - Changed USOIL to Crude-F
- `MarketOverviewTab.kt` - Added Brent-F icon mapping
- `OrderBookStore.kt` - Added Crude-F and Brent-F tick size handling

#### Trading App Components
- `TradingChart.kt` - Added Crude-F and Brent-F name mappings
- `RightPanel.kt` - Added both old and new symbol names
- `Quotes.kt` - Added Brent-F symbol info
- `AssetIcons.kt` - Extended oil commodity icon logic
- `ChartFeedType.kt` - Added Crude-F and Brent-F for both Exness and Pepperstone
- `DerivService.kt` - Updated symbol resolution to include new names

#### ViewModel
- `ForexViewModel.kt` - Updated Deriv subscription list to use Crude-F and Brent-F

### 4. AI System Mapping

The AI system has 3 oil-related assets:
- **USOIL** - WTI Crude (maps to app's Crude-F)
- **BRENTCMDUSD** - Brent Crude (maps to app's Brent-F)
- **UKOIL** - Brent Crude duplicate (also maps to app's Brent-F)

The app now uses Pepperstone's naming:
- **Crude-F** receives data from Pepperstone and AI scores from USOIL
- **Brent-F** receives data from Pepperstone and AI scores from BRENTCMDUSD/UKOIL

## Asset Count Verification

**AI System**: 28 assets
**App**: 27 unique assets (BRENTCMDUSD and UKOIL in AI both map to Brent-F in app)

All 28 AI assets are now covered:
- 8 Forex pairs (including EURGBP, EURJPY, USDCAD)
- 5 Stocks
- 4 Commodities (XAUUSD, XAGUSD, Crude-F/USOIL, Brent-F/BRENTCMDUSD)
- 4 Crypto
- 4 Indices
- 2 Bonds

## Next Steps

1. Start the 3 production services:
   ```powershell
   cd c:\Users\HP\AndroidStudioProjects\MyRealApp
   .\start_ctrader_bridge.ps1
   
   cd c:\Users\HP\AndroidStudioProjects\MyRealApp
   python ctrader_to_redis.py
   
   cd c:\Users\HP\Documents\NEW_ASC
   .\start_production_live.ps1
   ```

2. Verify live data flows for Crude-F and Brent-F from Pepperstone
3. Verify AI scores appear for all 28 assets in the app
4. Test that final_trade_score displays correctly (not confluence_score)

## Benefits

✅ App now uses Pepperstone's actual symbol names (Crude-F, Brent-F)
✅ Live data will flow correctly from cTrader
✅ AI scores will map correctly via the bridge
✅ All 28 AI assets are now represented in the app
✅ Backward compatibility maintained (old names still work in mappings)
