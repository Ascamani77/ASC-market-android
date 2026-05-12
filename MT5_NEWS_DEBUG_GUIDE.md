# MT5 FXStreet News Debugging Guide

## Problem
MT5 Bridge is connected and sending live price data, but FXStreet news is not appearing in the Research page.

## Enhanced Logging Added

I've added comprehensive logging throughout the news data flow to help identify where the issue is.

### Log Tags to Monitor

Run `adb logcat` and filter for these tags:

```bash
adb logcat | grep -E "TradingApp|MT5_BRIDGE|Mt5NewsStore|NewsViewModel"
```

### Expected Log Flow

When everything works correctly, you should see this sequence:

#### 1. Connection & Request
```
I/TradingApp: MT5 connected, requesting news...
I/TradingApp: News request sent to MT5 Bridge
I/Mt5Service: Requesting news from MT5 Bridge via 'get_news' action
```

#### 2. MT5 Bridge Response
```
I/MT5_BRIDGE: === NEWS RESPONSE RECEIVED ===
I/MT5_BRIDGE: Raw news JSON: {"type":"news","items":[...]}
I/MT5_BRIDGE: Parsed news items: 25
```

#### 3. TradingApp Receives News
```
I/TradingApp: === MT5 NEWS RECEIVED ===
I/TradingApp: News items count: 25
I/TradingApp: News: EUR/USD Technical Analysis - Technical Analysis
I/TradingApp: News: Gold Price Forecast - Market Analysis
I/TradingApp: News stored in Mt5NewsStore
```

#### 4. Mt5NewsStore Updates
```
I/Mt5NewsStore: === UPDATING NEWS STORE ===
I/Mt5NewsStore: Received 25 news items
I/Mt5NewsStore:   - EUR/USD Technical Analysis
I/Mt5NewsStore:   - Gold Price Forecast
I/Mt5NewsStore: Store updated, current size: 25
```

#### 5. NewsViewModel Processes
```
I/NewsViewModel: Started observing Mt5NewsStore
I/NewsViewModel: Mt5NewsStore updated: 25 items
I/NewsViewModel: Converting 25 MT5 news items to NewsArticle format
I/NewsViewModel: Converted MT5 articles, first title: EUR/USD Technical Analysis
I/NewsViewModel: Total articles after merge: 45 (was 20)
```

## Troubleshooting by Log Output

### Scenario 1: No Connection Logs
**Symptoms:**
```
W/TradingApp: MT5 not connected, cannot request news
```

**Problem:** MT5 Bridge is not connected to the app

**Solutions:**
1. Check MT5 Bridge is running on your PC
2. Verify IP address and port in app settings
3. Check firewall allows port 8081
4. Verify network connectivity between phone and PC

### Scenario 2: Request Sent, No Response
**Symptoms:**
```
I/TradingApp: News request sent to MT5 Bridge
I/Mt5Service: Requesting news from MT5 Bridge via 'get_news' action
(no further logs)
```

**Problem:** MT5 Bridge received request but didn't send news back

**Solutions:**
1. Check MT5 Bridge Python logs for errors
2. Verify MT5 Bridge has `get_news` action handler
3. Check if MT5 has FXStreet news data available
4. Verify WebSocket connection is stable

### Scenario 3: Response Received, Parse Error
**Symptoms:**
```
I/MT5_BRIDGE: === NEWS RESPONSE RECEIVED ===
I/MT5_BRIDGE: Raw news JSON: {"type":"news"...
E/MT5_BRIDGE: Error parsing news: ...
```

**Problem:** News JSON format doesn't match expected structure

**Solutions:**
1. Check the raw JSON in logs
2. Verify NewsPayload and NewsItem models match MT5 Bridge format
3. Update models if MT5 Bridge sends different structure

### Scenario 4: News Received, Not Showing in UI
**Symptoms:**
```
I/NewsViewModel: Total articles after merge: 45 (was 20)
(but UI shows only 20 articles)
```

**Problem:** UI not refreshing or filtering out MT5 news

**Solutions:**
1. Check if Research page is observing `articles` StateFlow
2. Verify no category filter is hiding "fxstreet" category
3. Check if search is active and filtering results
4. Pull to refresh the Research page

## Manual Testing Steps

### Step 1: Install and Launch
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c  # Clear logs
adb logcat | grep -E "TradingApp|MT5_BRIDGE|Mt5NewsStore|NewsViewModel"
```

### Step 2: Navigate to Research Page
1. Open app
2. Open sidebar
3. Tap "Analysis & Opinion" (Research page)
4. Watch logs for news loading

### Step 3: Force News Refresh
1. Pull down to refresh on Research page
2. This should trigger a new news request
3. Watch logs for the full flow

### Step 4: Check MT5 Bridge
On your PC, check MT5 Bridge logs for:
```
Received action: get_news
Sending news: 25 items
```

## MT5 Bridge Requirements

Your MT5 Bridge Python server must:

1. **Handle `get_news` action:**
```python
if action == "get_news":
    news_items = get_fxstreet_news_from_mt5()
    send_news_to_app(news_items)
```

2. **Send news in this format:**
```json
{
  "type": "news",
  "items": [
    {
      "id": 12345,
      "title": "EUR/USD Technical Analysis",
      "timeLabel": "2h ago",
      "isoDateTime": "2026-05-09T08:30:00Z",
      "countryCode": "EU",
      "category": "Technical Analysis",
      "detailsUrl": "https://fxstreet.com/..."
    }
  ],
  "lastUpdatedIso": "2026-05-09T10:30:00Z"
}
```

3. **Have WebSocket connection stable:**
- Same WebSocket used for price data
- Port 8081 (default)
- Bidirectional communication

## Quick Diagnostic Command

Run this to see if news is flowing:

```bash
adb logcat -s TradingApp:I Mt5NewsStore:I NewsViewModel:I MT5_BRIDGE:I | grep -i news
```

## Expected Outcome

After installing the updated APK and navigating to Research page, you should see:
- MT5 FXStreet news articles with source "FXStreet (MT5)"
- Category labeled as "fxstreet"
- Author shown as "FXStreet"
- Mixed with other news sources in one unified feed

## Next Steps

1. Install the APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Run logcat with filters
3. Open Research page
4. Share the logs with me so I can see exactly where the flow breaks

The logs will tell us:
- ✅ Is MT5 connected?
- ✅ Is news request being sent?
- ✅ Is MT5 Bridge responding?
- ✅ Is the response being parsed?
- ✅ Is the data reaching the UI?
