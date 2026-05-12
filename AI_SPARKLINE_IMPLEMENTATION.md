# AI Pre-Move Sparkline Implementation

## Overview
Added a **Market Readiness Engine** sparkline to the Pre-Move AI Chart that measures hidden market preparation on a 0-100 scale.

## Key Concept
**NOT a price prediction line** - it's a market readiness meter that measures:
- How close the market is to expansion
- Hidden market preparation
- Structural pressure building

## Visual Design
- **White dashed line** overlaid on the price chart
- **Percentage badge** on the right showing current AI readiness score
- **Smooth bezier-style line** with momentum memory (no instant jumps)

## AI Score Components (Weighted)

| Component | Weight | What It Measures |
|-----------|--------|------------------|
| Structure Quality | 20% | Trend formation, higher lows/highs |
| Compression Quality | 20% | Range tightening, volatility shrinking |
| Volatility Alignment | 15% | Volatility patterns |
| Momentum Buildup | 15% | Acceleration and positive changes |
| Liquidity Pressure | 10% | Volume-like pressure from price action |
| Confluence Score | 10% | Alignment of multiple factors |
| Expansion Probability | 10% | Likelihood of breakout |

## Phase Behavior

### NOISE (0-20)
- **Market**: Random, weak, no liquidity alignment
- **AI Line**: Low + messy + weak
- **Example**: 3 → 5 → 4 → 7 → 5

### STRUCTURE (20-40)
- **Market**: Trend attempting to form, context improving
- **AI Line**: Gradual climb, more stability
- **Example**: 20 → 24 → 28 → 30 → 35

### COMPRESSION (40-60)
- **Market**: Range tightening, volatility shrinking, pressure building
- **AI Line**: Flattens slightly, oscillates tightly (coiling energy)
- **Example**: 52 → 54 → 53 → 55 → 54

### PRE-MOVE (60-80)
- **Market**: Confluence increasing, momentum aligning, volume appearing
- **AI Line**: Accelerates upward (probability increasing)
- **Example**: 63 → 66 → 70 → 74 → 78

### EXPANSION (80-100)
- **Market**: Breakout, strong momentum, execution zone
- **AI Line**: Fast, volatile, explosive spike behavior
- **Example**: 82 → 91 → 96 → 88 → 94

## Technical Implementation

### Smoothing (Momentum Memory)
```kotlin
smoothed = previous * 0.8f + current * 0.2f
```
Prevents instant jumps from 20 → 80, creates natural transitions.

### Calculation Functions
1. **calculateStructureScore()** - Detects higher highs/lows
2. **calculateCompressionScore()** - Measures range tightening
3. **calculateVolatilityScore()** - Analyzes volatility patterns
4. **calculateMomentumScore()** - Tracks acceleration
5. **calculateLiquidityScore()** - Simulates volume pressure
6. **calculateConfluenceScore()** - Checks factor alignment
7. **calculateExpansionProbability()** - Predicts breakout likelihood

### Drawing
- Dashed white line (6px dash, 4px gap)
- 2dp stroke width with rounded caps
- Percentage badge with black background (70% opacity)
- Text centered in badge, 11sp font size

## Important Notes
- **AI rises BEFORE price expansion** - that's the edge
- Each timeframe recalculates independently (15m ≠ 1H ≠ 4H)
- Line does NOT copy price movement
- Measures market state evolution, not price ticks

## Files Modified
- `MyRealApp/app/src/main/java/com/asc/markets/ui/screens/dashboard/PreMoveAiMock.kt`
  - Added AI sparkline drawing code (lines ~670-740)
  - Added 7 calculation functions (lines ~1677-1900)
  - Added helper function for standard deviation

## Status
✅ Compilation successful
✅ AI readiness engine implemented
✅ Sparkline overlay rendering
✅ Percentage badge display
✅ Momentum memory smoothing
✅ All 7 component calculations working
