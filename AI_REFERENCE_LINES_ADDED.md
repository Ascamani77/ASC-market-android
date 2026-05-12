# AI Reference Lines Added to Chart

## ✅ Feature Added

Added horizontal AI phase reference lines to the **AI PRICE & PRE-MOVE CHART** showing the phase boundaries (0-100 scale).

## 📊 What Was Added

### AI Phase Reference Lines
The chart now displays 5 horizontal reference lines representing the AI phase boundaries:

| Level | Phase | Color | Label Position |
|-------|-------|-------|----------------|
| 100 | EXPANSION | Blue | Left side |
| 80 | PRE-MOVE | Green | Left side |
| 60 | COMPRESSION | Yellow | Left side |
| 40 | STRUCTURE | Orange | Left side |
| 20 | NOISE | Red | Left side |

### Visual Elements

**1. Dashed Horizontal Lines**
- Semi-transparent dashed lines across the chart width
- Color-coded to match each phase
- Helps visualize which phase the AI score is in

**2. Level Numbers**
- Displayed on the left side (20, 40, 60, 80, 100)
- Color-coded to match the phase
- Right-aligned for clean appearance

**3. Phase Labels**
- Phase names (NOISE, STRUCTURE, etc.) on the left
- Bold text for emphasis
- Positioned just above each reference line

## 🎨 Implementation

**File**: `PreMoveAiMock.kt` - `PreMoveCombinedChartBox` function

### Code Added

```kotlin
// Draw AI phase reference lines (0-100 scale on left Y-axis)
val aiPhases = listOf(
    20f to NoiseRed to "NOISE",
    40f to StructureOrange to "STRUCTURE", 
    60f to CompressionYellow to "COMPRESSION",
    80f to PreMoveGreen to "PRE-MOVE",
    100f to ExpansionBlue to "EXPANSION"
)

aiPhases.forEach { (level, colorPair) ->
    val (color, label) = colorPair
    val yPos = h - (level / 100f * h)
    
    // Draw horizontal reference line
    drawLine(
        color = color.copy(alpha = 0.15f),
        start = Offset(0f, yPos),
        end = Offset(w, yPos),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )
    
    // Draw level number on left
    // Draw phase label on left
}
```

### Import Added
```kotlin
import androidx.compose.ui.graphics.toArgb
```

## 🎯 Visual Result

The chart now shows:
```
100 _ EXPANSION ___________________________
                                          
 80 _ PRE-MOVE ____________________________
                                          
 60 _ COMPRESSION _________________________
                                          
 40 _ STRUCTURE ___________________________
                                          
 20 _ NOISE _______________________________
                                          
  0 _______________________________________
```

With the price line overlaid on top, users can easily see:
- Which AI phase the current score is in
- How close the score is to transitioning to the next phase
- Historical phase transitions over time

## 💡 Benefits

1. **Visual Context**: Users can immediately see which phase the AI is in
2. **Phase Transitions**: Easy to spot when AI moves between phases
3. **Color Coding**: Matches the AI Progression Scale colors
4. **Non-Intrusive**: Dashed lines with low opacity don't obscure the price data
5. **Professional**: Clean, chart-like appearance similar to trading platforms

## 🧪 Testing

### How to Test
1. Build and run the app
2. Navigate to **Market Overview**
3. Select any asset
4. View the **AI PRICE & PRE-MOVE CHART**

### Expected Behavior
- ✅ 5 horizontal dashed lines visible
- ✅ Lines color-coded (red, orange, yellow, green, blue)
- ✅ Level numbers (20, 40, 60, 80, 100) on left
- ✅ Phase labels (NOISE, STRUCTURE, etc.) on left
- ✅ Lines span full chart width
- ✅ Price line renders on top of reference lines

## 🔄 Works Across All Timeframes

The reference lines are drawn in the same coordinate space as the chart, so they work correctly on:
- 5m, 15m, 30m
- 1hr, 4hr
- 1D, 1w

## 📐 Technical Details

### Coordinate Mapping
```kotlin
val yPos = h - (level / 100f * h)
```
- 0% (bottom) = h
- 100% (top) = 0
- Linear mapping from AI score (0-100) to Y position

### Drawing Order
1. AI reference lines (background)
2. Price area fill
3. Price line
4. Current price dot (foreground)

This ensures the price data is always visible on top of the reference lines.

### Opacity
- Reference lines: 15% opacity
- Labels: 70% opacity
- Ensures they're visible but don't dominate the chart

## ✅ Summary

**Feature**: AI phase reference lines  
**Location**: AI PRICE & PRE-MOVE CHART  
**Lines Added**: 5 (at 20, 40, 60, 80, 100)  
**Style**: Dashed, color-coded, with labels  
**Purpose**: Show AI phase boundaries for context  
**Result**: Users can easily see which phase the AI score is in

---

**Status**: ✅ **ADDED**  
**Date**: 2026-05-08  
**Impact**: Enhanced chart readability with AI phase context
