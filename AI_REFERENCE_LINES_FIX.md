# AI Reference Lines - Compilation Fix

## Issue
The AI reference lines code had multiple compilation errors:
1. Wrong destructuring syntax for Triple
2. `rememberTextMeasurer()` called inside Canvas scope (not allowed)
3. `drawText()` not available in Canvas scope
4. Type inference failures
5. Missing import for `nativeCanvas`

## Solution
Fixed by using Android's native Canvas text drawing instead of Compose text measurer:

### Changes Made:
1. **Added missing import**: `import androidx.compose.ui.graphics.nativeCanvas`
2. **Proper Triple destructuring**: `(level, color, label)` works correctly with Triple
3. **Native Canvas text drawing**: 
   - Get native canvas: `drawContext.canvas.nativeCanvas`
   - Create Paint object: `android.graphics.Paint()`
   - Use `nativeCanvas.drawText()` instead of Compose `drawText()`
4. **Color conversion**: Use `.toArgb()` to convert Compose Color to Android color int

### Code Structure:
```kotlin
import androidx.compose.ui.graphics.nativeCanvas

// Inside Canvas scope:
val nativeCanvas = drawContext.canvas.nativeCanvas
val textPaint = android.graphics.Paint().apply {
    textSize = 10.sp.toPx()
    isFakeBoldText = true
    isAntiAlias = true
}

aiPhases.forEach { (level, color, label) ->
    val yPos = h - (level / 100f * h)
    
    // Draw dashed horizontal line
    drawLine(...)
    
    // Draw level number using native canvas
    textPaint.color = color.toArgb()
    nativeCanvas.drawText(levelText, x, y, textPaint)
    
    // Draw phase label
    textPaint.textSize = 8.sp.toPx()
    nativeCanvas.drawText(label, x, y, textPaint)
}
```

## Result
- 5 horizontal reference lines at AI phase boundaries (20, 40, 60, 80, 100)
- Color-coded: Red (NOISE), Orange (STRUCTURE), Yellow (COMPRESSION), Green (PRE-MOVE), Blue (EXPANSION)
- Dashed lines with 15% opacity
- Level numbers and phase labels on left side
- Lines render behind the price chart

## Status
✅ Compilation successful
✅ Build completed in 10m 36s
✅ Ready for testing
