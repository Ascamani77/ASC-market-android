# Macro Stream InfoBox Update - News Sources Added

## Changes Made

### Enhanced InfoBox Display
Added comprehensive news source information to the Macro Stream InfoBox to show users where the data is coming from.

---

## New InfoBox Layout

### Before:
```
[Node Status] ────────────────── [Events Captured Progress]
```

### After:
```
[Node Status] ────────────────── [Events Captured Progress]
─────────────────────────────────────────────────────────────
NEWS SOURCES: 14 RSS Feeds • Fed, ECB, BOE, BOJ • ForexLive, FXStreet
[Central Banks] [Forex] [Commodities] [Macro Data]
```

---

## New Components

### 1. News Sources Row
Displays:
- **Total feeds**: "14 RSS Feeds"
- **Key sources**: "Fed, ECB, BOE, BOJ"
- **Forex sources**: "ForexLive, FXStreet"

### 2. Source Category Badges
Four color-coded badges showing data categories:

| Badge | Color | Category |
|-------|-------|----------|
| Central Banks | Blue (#1E88E5) | Fed, ECB, BOE, BOJ, RBA, BOK, SNB |
| Forex | Green (#43A047) | ForexLive, FXStreet, MyFxBook |
| Commodities | Orange (#FFA726) | EIA, IEA, OPEC, CFTC |
| Macro Data | Purple (#AB47BC) | St. Louis Fed, IMF |

### 3. Visual Divider
Added horizontal divider between node status and news sources for better visual separation.

---

## Visual Design

### InfoBox Structure:
```
┌─────────────────────────────────────────────────────────┐
│ ● Node: NY4          Events Captured: 45               │
│                      ████████████░░░░░░░░ 75%          │
├─────────────────────────────────────────────────────────┤
│ NEWS SOURCES: 14 RSS Feeds • Fed, ECB, BOE, BOJ •      │
│               ForexLive, FXStreet                       │
│                                                         │
│ [Central Banks] [Forex] [Commodities] [Macro Data]     │
└─────────────────────────────────────────────────────────┘
```

### Color Scheme:
- **Background**: LoadingGrey900 (dark)
- **Text**: SlateText (gray)
- **Accent**: IndigoAccent (blue)
- **Divider**: White 10% opacity
- **Badges**: Category-specific colors with 15% opacity background

---

## Code Changes

### File: `MacroStreamScreen.kt`

#### 1. Updated InfoBox Content
Changed from single-row layout to multi-row with sections:
- Row 1: Node status + Event stats
- Divider
- Row 2: News sources text
- Row 3: Category badges

#### 2. Added SourceBadge Composable
```kotlin
@Composable
private fun SourceBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

#### 3. Added HorizontalDivider Import
```kotlin
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
```

---

## Benefits

### 1. Transparency
Users can see exactly where news is coming from:
- 14 RSS feeds
- Major central banks
- Forex news sites
- Commodities sources

### 2. Trust Building
Showing reputable sources builds confidence:
- Federal Reserve
- European Central Bank
- Bank of England
- Bank of Japan
- ForexLive
- FXStreet

### 3. Educational Value
Students learn which sources professional traders monitor:
- Central bank announcements
- Economic data releases
- Forex market news
- Commodity reports

### 4. Visual Clarity
Color-coded badges make it easy to understand data categories at a glance.

---

## Source Breakdown

### Central Banks (7 feeds):
- Federal Reserve (monetary policy)
- European Central Bank
- Bank of England
- Bank of Japan
- Swiss National Bank
- Reserve Bank of Australia
- Bank of Korea

### Forex (3 feeds):
- ForexLive
- FX Street
- MyFxBook Economic Calendar

### Commodities (4 feeds):
- EIA (Energy Information Administration)
- IEA (International Energy Agency)
- OPEC
- CFTC (Commodity Futures Trading Commission)

### Macro Data (2 feeds):
- St. Louis Fed (economic data)
- IMF (International Monetary Fund)

**Total: 14 RSS Feeds** (reduced from 40+ for trading focus)

---

## User Experience

### What Users See:
1. **Node health indicator** - System status
2. **Event capture progress** - How many events tracked
3. **News sources label** - Clear heading
4. **Source count** - "14 RSS Feeds"
5. **Key institutions** - "Fed, ECB, BOE, BOJ"
6. **Forex sources** - "ForexLive, FXStreet"
7. **Category badges** - Visual breakdown by type

### Information Hierarchy:
1. **Primary**: Node status + Event stats (most important)
2. **Secondary**: News sources (context)
3. **Tertiary**: Category badges (detail)

---

## Responsive Design

### Layout Adapts:
- **Wide screens**: All content fits in single row
- **Narrow screens**: Text wraps naturally
- **Badges**: Wrap to next line if needed

### Font Sizes:
- **Heading**: 11sp (NEWS SOURCES)
- **Body**: 11sp (feed count)
- **Detail**: 10sp (source names)
- **Badges**: 10sp (category labels)

---

## Future Enhancements

### Option 1: Dynamic Source Count
Show actual number of active sources:
```kotlin
val activeSources = macroEvents.map { it.source }.distinct().size
Text("$activeSources Active Sources")
```

### Option 2: Source Health Indicators
Show which sources are responding:
```kotlin
SourceBadge("Central Banks ✓", Color.Green)
SourceBadge("Forex ⚠", Color.Yellow)
```

### Option 3: Last Update Time
Show when sources were last polled:
```kotlin
Text("Last updated: 2 min ago", fontSize = 9.sp)
```

### Option 4: Expandable Source List
Tap badge to see full list of sources in that category:
```kotlin
if (expandedCategory == "Central Banks") {
    // Show: Fed, ECB, BOE, BOJ, SNB, RBA, BOK
}
```

---

## Testing

### Verify Display:
1. Open Macro Stream
2. Check InfoBox shows:
   - ✅ Node status
   - ✅ Event capture progress
   - ✅ Divider line
   - ✅ "NEWS SOURCES:" label
   - ✅ "14 RSS Feeds"
   - ✅ Central bank names
   - ✅ Forex source names
   - ✅ Four category badges

### Check Colors:
- Central Banks badge: Blue
- Forex badge: Green
- Commodities badge: Orange
- Macro Data badge: Purple

### Verify Layout:
- InfoBox expands vertically
- Content is readable
- Badges don't overlap
- Divider is visible

---

## Summary

The Macro Stream InfoBox now clearly shows:
- ✅ Where news is coming from (14 RSS feeds)
- ✅ Which institutions (Fed, ECB, BOE, BOJ)
- ✅ Which forex sources (ForexLive, FXStreet)
- ✅ Data categories (Central Banks, Forex, Commodities, Macro Data)

This transparency helps users understand the quality and breadth of the news intelligence system, building trust and providing educational value for your trading course.
