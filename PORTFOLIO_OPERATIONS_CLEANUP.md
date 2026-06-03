# PORTFOLIO & OPERATIONS CLEANUP

**Date**: June 1, 2026  
**Status**: Completed  
**Action**: Removed redundant and broken pages

---

## PAGES REMOVED

### 1. ❌ Terminal Desk (TRADING_ASSISTANT)
**Reason**: Redundant with AI Intel (Chat)

**Why it was removed**:
- ARM/DISARM button already exists in status bar
- All commands (ACCOUNT, ASC STATUS, RUN AI) can be asked in AI Intel
- AI Intel provides better conversational interface
- Terminal Desk was just a worse version of AI Intel with terminal theme
- No unique functionality that justifies separate page

**Files modified**:
- `Sidebar.kt` - Removed menu item
- `MainActivity.kt` - Removed route and imports
- Changed Watchlist "View Chart" to navigate to STREAM instead

---

### 2. ❌ Raw Feed (NEWS)
**Reason**: Broken and redundant with Analysis & Opinion

**Why it was removed**:
- Completely broken - shows empty screen with no content
- Not connected to any RSS feeds or news API
- Analysis & Opinion already provides news with:
  - ✅ 14 RSS feeds
  - ✅ Article reading
  - ✅ Bookmarks
  - ✅ Proper date filtering
  - ✅ Categories and search

**Files modified**:
- `Sidebar.kt` - Removed menu item
- `MainActivity.kt` - Removed route, back handler, bottom bar exclusion, header exclusion

---

## UPDATED PORTFOLIO & OPERATIONS SECTION

### Before (6 items):
1. Active Inventory ✅
2. Live Trade ✅
3. Raw Feed ❌ (broken)
4. Event Calendar ⚠️
5. Terminal Desk ❌ (redundant)
6. Push Notification ✅

### After (4 items):
1. **Active Inventory** ✅ - Excellent, connected to live data
2. **Live Trade** ✅ - Fully functional trading interface
3. **Event Calendar** ⚠️ - Needs MT5 backend verification
4. **Push Notification** ✅ - Working settings page

---

## BENEFITS

### Cleaner Navigation:
- Removed 2 redundant/broken pages
- Reduced menu clutter
- Easier to find useful features

### Better User Experience:
- No confusion between Terminal Desk and AI Intel
- No broken Raw Feed page
- Focus on working, useful features

### Easier Maintenance:
- Less code to maintain
- Fewer pages to test
- Clearer app structure

---

## WHAT'S LEFT TO CHECK

### Event Calendar Status:
- ⚠️ Depends on MT5 backend
- Need to verify if MT5 service is running
- If not working, consider:
  - Replacing with web-based calendar API (ForexFactory, Investing.com)
  - Or removing if not essential

---

## NEXT STEPS

1. ✅ Remove Terminal Desk - DONE
2. ✅ Remove Raw Feed - DONE
3. ⏳ Test Event Calendar (verify MT5 backend)
4. ⏳ Continue auditing other sidebar sections

---

## FILES MODIFIED

### Sidebar.kt
- Removed "Raw Feed" menu item
- Removed "Terminal Desk" menu item
- Portfolio & Operations now has 4 items instead of 6

### MainActivity.kt
- Removed `AppView.TRADING_ASSISTANT -> TerminalScreen(viewModel)` route
- Removed `AppView.NEWS -> MainScreen(...)` route
- Removed `import com.researchcenter.ui.screens.MainScreen`
- Updated back handler (removed NEWS exclusion)
- Updated bottom bar exclusion (removed TRADING_ASSISTANT and NEWS)
- Updated header exclusion (removed NEWS)
- Changed Watchlist navigation from TRADING_ASSISTANT to STREAM

---

**Cleanup completed**: June 1, 2026  
**Result**: Portfolio & Operations section now has 4 focused, working pages
