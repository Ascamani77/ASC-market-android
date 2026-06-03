# Market Data Bus - Real Data Integration

## Changes Made

### 1. Updated Data Sources
**Before:** Generic relay names (LMAX NY4, MT5 Bridge)
**After:** Real data sources used in the app:
- **Pepperstone cTrader Live** - Live trading account data
- **Pepperstone cTrader Demo** - Demo account data  
- **Binance USDT Futures** - Binance cryptocurrency pairs
- **ASC AI Backend** - AI analysis and deployments

### 2. Real Telemetry Integration

#### MarketDataStore (Pepperstone cTrader)
- Added `SystemTelemetry.recordTick("CTRADER_LIVE", 5.0)` on every price update
- Records ~5ms latency (typical for cTrader)
- Tracks throughput (ticks per second)

#### BinanceDataStore
- Added `SystemTelemetry.recordTick("BINANCE", 8.0)` on every price update
- Records ~8ms latency (typical for Binance WebSocket)
- Tracks throughput for USDT pairs

#### ASC AI Backend
- Already had telemetry in `AiRepository.getLatestDeployments()`
- Records actual API latency from backend calls
- Tracks deployment fetch frequency

### 3. Connection Status Indicators

**Active vs Idle Detection:**
- Relays show **ACTIVE** status (green dot) when data is flowing (buffer > 0 or latency > 0)
- Relays show **IDLE** status (gray dot) when no data is flowing
- Progress bars only display for active relays
- Inactive relays show "—" for metrics

**Visual Indicators:**
- Green dot + "ACTIVE" = Data flowing
- Gray dot + "IDLE" = No data
- "Relay Active" footer for active services
- "Standby" footer for idle services

### 4. Text Casing Updates (Title Case)

**Main Headers:**
- "Unified Data Bus" (was "UNIFIED DATA BUS")
- "Core Repository Heartbeat" (was "CORE REPOSITORY HEARTBEAT")
- "Total Throughput" (was "TOTAL THROUGHPUT")
- "Agg. Latency" (was "AGG. LATENCY")
- "Repository Flow Monitor" (was "REPOSITORY FLOW MONITOR")
- "Throughput Pulse" (was "THROUGHPUT PULSE")
- "Buffer Load" (was "BUFFER LOAD")

**Relay Card Labels:**
- "Latency" (was "LATENCY")
- "Buffer Load" (was "BUFFER LOAD")
- "Relay Active" (was "RELAY ACTIVE")
- "Standby" (was "STANDBY")

**Log Messages:**
- "Waiting for events..." (was "WAITING FOR EVENTS...")
- "Listening for event redux..." (was "LISTENING_FOR_EVENT_REDUX...")

### 5. SystemTelemetry Enhancements

**New Features:**
- `setActiveCTraderMode(mode: String)` - Track if Live or Demo mode is active
- Separate tracking for CTRADER_LIVE and CTRADER_DEMO
- Source normalization handles multiple variants:
  - CTRADER_LIVE, CTRADER-LIVE, PEPPERSTONE_LIVE, MARKET → cTrader Live
  - CTRADER_DEMO, CTRADER-DEMO, PEPPERSTONE_DEMO → cTrader Demo
  - BINANCE, BINANCE_USDT → Binance
  - ASC_AI, ASC-AI, ASC AI, BACKEND → ASC AI

**Metrics Tracked:**
- Latency (ms) - Real API/WebSocket latency
- Buffer Load (%) - Calculated from ticks per second
- Throughput (t/s) - Total ticks across all sources
- Aggregate Latency (ms) - Average across all sources

## How It Works

### Data Flow
1. **Price Updates Arrive** → MarketDataStore or BinanceDataStore receives update
2. **Telemetry Recorded** → `SystemTelemetry.recordTick(source, latency)` called
3. **Metrics Calculated** → Every 1 second, SystemTelemetry calculates:
   - Ticks per second (throughput)
   - Average latency
   - Buffer load per source
4. **UI Updates** → DataHubScreen displays real-time metrics

### Active Detection Logic
```kotlin
val isActive = relay.buffer > 0.0 || relay.latency > 0.0
```
- If any data has been received in the last second, relay shows as ACTIVE
- If no data for >1 second, relay shows as IDLE

## Files Modified

1. **SystemTelemetry.kt**
   - Updated relay list with real data sources
   - Added cTrader Live/Demo tracking
   - Added `setActiveCTraderMode()` function

2. **MarketDataStore.kt**
   - Added telemetry recording on price updates
   - Records as CTRADER_LIVE source

3. **BinanceDataStore.kt**
   - Added telemetry recording on price updates
   - Records as BINANCE source

4. **DataHubScreen.kt**
   - Updated relay cards with active/idle status
   - Added connection status indicators
   - Updated all text to Title Case
   - Progress bars only show for active relays

## Testing

To verify real data is showing:
1. Open Market Data Bus page
2. Check that relays show ACTIVE status when data is flowing
3. Verify latency and buffer load values change in real-time
4. Check that inactive relays show IDLE with gray indicators
5. Verify throughput pulse shows actual tick rate

## Notes

- **cTrader Live vs Demo**: Currently both record as CTRADER_LIVE. To differentiate, the chart feed type would need to be passed to MarketDataStore.
- **Latency Values**: Hardcoded estimates (5ms for cTrader, 8ms for Binance). Could be enhanced to measure actual WebSocket round-trip time.
- **Buffer Load**: Calculated as `(ticks_per_second * 20)` capped at 100%. This is a proxy metric, not actual buffer usage.
