# Volatility Chart Grid Lines Fix

## Issue
The vertical grid lines in the volatility chart were incorrectly crossing the entire chart horizontally. According to the reference design, the lines should be:
- **Vertical only** (not horizontal/crossed)
- **Start from phase boundaries** (divisions between states like QUIET/BALANCED, BALANCED/BUILDING, etc.)
- **Dashed style** for subtle appearance

## Changes Made

### 1. Fixed Vertical Grid Lines
**Before:**
```kotlin
// Drew horizontal grid lines at regular intervals (6 divisions)
val gridCount = 6
for (i in 0..gridCount) {
    val x = (w / gridCount) * i
    drawLine(/* horizontal line */)
}
```

**After:**
```kotlin
// Draw vertical lines at phase boundaries only
phases.forEach { phase ->
    val x = w * phase.end  // End of each phase = start of next phase
    drawLine(
        color = Color.White.copy(alpha = 0.15f),
        start = Offset(x, 0f),
        end = Offset(x, h),  // Vertical line from top to bottom
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )
}
```

### 2. Moved Legend to Header
**Before:**
- Legend was drawn inside the Canvas at the top
- Used native canvas text rendering
- Positioned at negative Y offset

**After:**
- Legend moved to `VolatilityChartHeader` composable
- Uses Compose UI elements (Row, Text, Canvas)
- Positioned between title and chart
- Better alignment and spacing

### 3. Updated Layout Structure
```kotlin
Column {
    // Title row
    Row { "LIVE VOLATILITY LINE" + Timeframe selector }
    
    Spacer(8.dp)
    
    // Legend row (NEW)
    Row { 
        "─── Realized Volatility" 
        "- - - Projected" 
    }
}
```

### 4. Adjusted Padding
- Reduced top padding from 28.dp to 12.dp (legend no longer inside canvas)
- Maintains proper spacing for phase labels at bottom

## Visual Result

### Grid Lines
- 6 vertical dashed lines at phase boundaries:
  1. Between QUIET and BALANCED (14%)
  2. Between BALANCED and BUILDING (29%)
  3. Between BUILDING and COMPRESSED (43%)
  4. Between COMPRESSED and TENSION (57%)
  5. Between TENSION and IGNITION (71%)
  6. Between IGNITION and EXPANSION (86%)

### Legend Position
- Now appears below the title
- Centered horizontally
- Clear visual indicators for line types

## Phase Boundaries
The vertical lines align with these volatility thresholds:
- **14%** - QUIET → BALANCED
- **29%** - BALANCED → BUILDING
- **43%** - BUILDING → COMPRESSED
- **57%** - COMPRESSED → TENSION
- **71%** - TENSION → IGNITION
- **86%** - IGNITION → EXPANSION

## Testing
- [x] Vertical lines appear at correct positions
- [x] Lines are dashed (not solid)
- [x] Lines span from top to bottom of chart
- [x] Legend displays correctly in header
- [x] No compilation errors
- [ ] Verify visual appearance matches reference image
- [ ] Test with different screen sizes

## Notes
- The vertical lines help users identify which volatility phase the market is in
- Lines are subtle (15% opacity) to not distract from the main volatility line
- Phase boundaries are mathematically aligned with the phase definitions
