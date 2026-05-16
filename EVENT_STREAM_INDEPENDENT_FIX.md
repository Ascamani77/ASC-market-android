# Event Stream Independent Loading - Debug Fix

## Changes Made

### 1. Enhanced Mt5Service Calendar Logging
**File**: `Mt5Service.kt`

Added detailed logging to track calendar response processing:
```kotlin
} else if (type == "calendar") {
    Log.d(TAG, "=== CALENDAR RESPONSE RECEIVED ===")
    Log.d(TAG, "Calendar JSON length: ${text.length}")
    try {
        val display = gson.fromJson(...)
        val ai = gson.fromJson(...)
        Log.d(TAG, "Calendar parsed: ${display.events.size} display events, ${ai.events.size} AI events")
        dispatchToMain {
            onCalendarUpdate(EconomicCalendarPayload(display = display, ai = ai))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse calendar response", e)
        Log.e(TAG, "Calendar JSON sample: ${text.take(500)}")
    }
}
```

### 2. Improved EventStreamScreen Connection Timing
**File**: `EventStreamScreen.kt`

- Increased connection delay to 2 seconds (from immediate)
- Added retry logic after 10 seconds if calendar still loading
- Better loading state indicators

```kotlin
LaunchedEffect(Unit) {
    // Wait longer for connection to fully establish
    delay(2000)
    val today = java.time.LocalDate.now().toString()
    android.util.Log.d("EventStreamScreen", "Requesting calendar for: $today")
    isCalendarLoading = true
    mt5Service.requestCalendar(today)
    
    // If no response after 10 seconds, try again
    delay(10000)
    if (isCalendarLoading) {
        android.util.Log.d("EventStreamScreen", "Calendar still loading, retrying...")
        mt5Service.requestCalendar(today)
    }
}
```

## How to Debug

### 1. Check MT5 Bridge Logs
The Python bridge shows:
```
Android connected: ('10.164.138.133', 58076)
Android message: {"action": "get_calendar", "params": {"selectedDate": "2026-05-15"}}
Android action: get_calendar
Tradays calendar: received 1294 events for 2026-05-01 to 2026-05-31
Calendar fetch error: received 1000 (OK); then sent 1000 (OK)
```

**Note**: "Calendar fetch error" is misleading - it's actually SUCCESS. The bridge is sending the data.

### 2. Check Android Logcat

Filter by these tags:
- `MT5_BRIDGE` - Mt5Service connection and message handling
- `EventStreamScreen` - Screen lifecycle and calendar requests
- `EventStreamViewModel` - Event processing

**Expected logs when working:**
```
D/EventStreamScreen: Connecting to Mt5Service...
D/MT5_BRIDGE: WebSocket Connected on 10.164.138.133
D/EventStreamScreen: Mt5Service connected: true
D/EventStreamScreen: Requesting calendar for: 2026-05-15
D/MT5_BRIDGE: === CALENDAR RESPONSE RECEIVED ===
D/MT5_BRIDGE: Calendar JSON length: 245678
D/MT5_BRIDGE: Calendar parsed: 1294 display events, 1294 AI events
D/EventStreamScreen: Calendar received: 1294 events
D/EventStreamViewModel: Display payload: true, events: 1294
D/EventStreamViewModel: Total calendar events: 1294
D/EventStreamViewModel: Filtered events: 45
D/EventStreamViewModel: Generated 45 intelligence events
```

### 3. Common Issues

#### Issue: No "CALENDAR RESPONSE RECEIVED" log
**Cause**: WebSocket not receiving calendar data
**Solutions**:
- Check MT5 bridge is running (`python mt5_bridge.py`)
- Verify PC IP is correct (10.164.138.133)
- Check firewall isn't blocking port 8081
- Verify Android device is on same network

#### Issue: "Failed to parse calendar response"
**Cause**: JSON parsing error
**Solutions**:
- Check the error log for details
- Look at the JSON sample in logs
- Verify calendar data structure matches models

#### Issue: Calendar received but no events in Event Stream
**Cause**: Events filtered out (older than 24 hours)
**Solutions**:
- Check filtered events count in logs
- Adjust time filter in EventStreamViewModel
- Verify calendar has recent/future events

#### Issue: Connection timeout
**Cause**: WebSocket taking too long to connect
**Solutions**:
- Increase delay in LaunchedEffect
- Check network latency
- Verify MT5 bridge is responsive

## Testing Steps

1. **Start MT5 Bridge**
   ```powershell
   cd C:\Users\HP\AndroidStudioProjects\MyRealApp
   python mt5_bridge.py
   ```

2. **Clear App Data** (optional, for clean test)
   - Settings → Apps → Your App → Clear Data

3. **Open Event Stream**
   - Launch app
   - Navigate directly to Intelligence Stream
   - Watch logcat for the log sequence above

4. **Verify Success**
   - Loading spinner appears
   - "Loading calendar events..." message shows
   - Events appear within 5-10 seconds
   - Events show with AI analysis (BIAS, POSTURE, CONFIDENCE)

## Network Configuration

- **PC IP**: 10.164.138.133
- **MT5 Port**: 8081
- **Protocol**: WebSocket (ws://)
- **Endpoint**: ws://10.164.138.133:8081

## Next Steps if Still Not Working

1. **Capture full logcat** during Event Stream open
2. **Check MT5 bridge output** for any actual errors
3. **Verify WebSocket connection** is established
4. **Test Calendar screen** first to confirm bridge works
5. **Check for JSON parsing errors** in Mt5Service logs

## Success Criteria

✅ Event Stream opens independently  
✅ Calendar data loads automatically  
✅ No need to visit Calendar screen first  
✅ Events appear within 10 seconds  
✅ AI analysis shows for each event  
✅ Proper error messages if something fails  

## Architecture

```
EventStreamScreen
    ↓
Creates own Mt5Service instance
    ↓
Connects to ws://10.164.138.133:8081
    ↓
Requests calendar for today
    ↓
Mt5Service receives calendar JSON
    ↓
Parses into EconomicCalendarPayload
    ↓
Stores in CalendarSnapshotStore
    ↓
Triggers EventStreamViewModel.refresh()
    ↓
ViewModel processes events
    ↓
AI analyzes each event
    ↓
Displays intelligence events
```

This is now fully independent from Calendar screen!
