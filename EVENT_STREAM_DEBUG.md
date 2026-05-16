# Event Stream Setup Guide

## Current Status

Event Stream displays AI-analyzed intelligence events from the Economic Calendar. Currently, it requires the Calendar to be loaded first to populate the data.

## How It Works

### Data Flow
```
1. Calendar loads via TradingChart components
2. Calendar data stored in CalendarSnapshotStore
3. Event Stream reads from CalendarSnapshotStore
4. AI analyzes each event
5. Events displayed with intelligence metrics
```

### Usage Steps

1. **Load Calendar First**
   - Navigate to Calendar screen
   - Wait for calendar events to load (2-3 seconds)
   - Calendar data is now cached

2. **Open Event Stream**
   - Navigate to Intelligence Stream
   - Events will appear immediately from cached data
   - Shows AI-analyzed intelligence with BIAS, POSTURE, CONFIDENCE

3. **Refresh**
   - Return to Calendar and pull to refresh
   - New calendar data will update Event Stream automatically

## Configuration

### PC IP Address
Your PC IP: `10.164.138.133`

### Service Ports
- MT5 Service: Port 8081 ✓ (Running)
- cTrader Bridge: Port 8082

### Network Config
Located in: `NetworkConfig.kt`
- Default Host: `10.164.138.133`
- MT5 Port: `8081`
- cTrader Port: `8082`

## Debugging

### Check Logcat

**EventStreamViewModel logs:**
```
D/EventStreamViewModel: Display payload: true, events: 45
D/EventStreamViewModel: Total calendar events: 45
D/EventStreamViewModel: Filtered events: 12
D/EventStreamViewModel: Generated 12 intelligence events
```

### Common Issues

#### No Events Showing
**Cause**: Calendar data not loaded yet
**Solution**: 
1. Go to Calendar screen
2. Wait for events to load
3. Return to Event Stream

#### Empty After Calendar Load
**Cause**: Events might be older than 24 hours
**Solution**: Calendar filters events from last 24 hours + future events

#### AI Analysis Failing
**Cause**: Gemini API issue
**Solution**: Check Gemini API key in BuildConfig

## Files Modified

1. **EventStreamViewModel.kt**
   - Added import for `EconomicCalendarDisplayEvent`
   - Enhanced logging
   - 24-hour time filter
   - Reduced delay to 300ms

2. **EventStreamScreen.kt**
   - Added empty state UI
   - Clear user guidance
   - LaunchedEffect to check for data

3. **NetworkConfig.kt**
   - Already configured with correct IP
   - MT5 service running on port 8081

## Expected Behavior

When working correctly:
- Load Calendar → Events populate
- Open Event Stream → Events appear immediately
- Each event shows:
  - Asset class badge (MACRO, FOREX, STOCK, COMMODITY)
  - Status badge (LOCKED, MONITOR, ACTIVE)
  - Event title and AI summary
  - BIAS (LONG/SHORT/NEUTRAL)
  - POSTURE (AGGRESSIVE/DEFENSIVE/BALANCED)
  - CONFIDENCE (0-100% with visual bar)
  - Affected assets

## Future Enhancement

To make Event Stream fully independent, we would need to:
1. Extract calendar loading logic from TradingChart components
2. Create a shared calendar service
3. Allow Event Stream to trigger calendar loads directly

For now, the current approach works well and uses cached data efficiently.
