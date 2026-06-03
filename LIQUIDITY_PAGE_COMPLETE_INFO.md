# Liquidity Page - Complete Information Display

## Enhancement
Added comprehensive information display for selected assets in the Liquidity page, showing all available analysis data.

## New Cards Added

### 1. **Scores Card**
Displays all key analysis scores with visual progress bars:
- **Direction Bias**: BULLISH/BEARISH/NEUTRAL with color coding
- **Pre-Move Score**: 0-100% with indigo progress bar
- **Ignition Score**: 0-100% with green progress bar
- **Structural Pressure**: 0-100% with orange progress bar
- **Liquidity Score**: 0-100% with indigo progress bar
- **Invalidation Level**: Price level that would invalidate the setup (if available)
- **State Badge**: ARMED/WATCH/COMPRESSING/FILTERING/LATE MOVE with color coding

### 2. **Layers Card**
Shows the 6 deterministic layers used for pre-move validation:
- **L1 STRUCTURE**: Structural pressure analysis
- **L3 REGIME**: Market regime validation
- **L5 PRESSURE**: Directional pressure measurement
- **L6A IDLE FILTER**: Late move prevention
- **L7 EXPANSION**: Expansion probability
- **ASC AI**: Live AI central verification

Each layer displays:
- Layer name and status badge (PASS/WATCH/FAIL/BLOCK/LIVE/PENDING)
- Score with progress bar (0-100%)
- Detailed explanation of the layer's assessment

### 3. **Trigger Conditions Card**
Lists the conditions to monitor before deployment:
- Numbered list of 4 key conditions
- Ignition score thresholds
- Liquidity pool confirmation requirements
- Invalidation level warnings
- News/calendar guard requirements

## Display Order
1. Liquidity Overview Card (existing)
2. **Scores Card** (NEW)
3. Liquidity Pools Card (existing)
4. Sweep Probability Card (existing)
5. **Layers Card** (NEW)
6. **Trigger Conditions Card** (NEW)
7. Correlation Gate Card (existing)

## Visual Design
- **Color Coding**:
  - Green (EmeraldSuccess): PASS, ARMED, BULLISH, positive states
  - Blue (IndigoAccent): WATCH, NEUTRAL, moderate states
  - Red (RoseError): FAIL, BLOCK, BEARISH, LATE MOVE, negative states
  - Orange: Structural pressure
  - Gray: PENDING, NO DATA states

- **Progress Bars**: Visual representation of all scores (0-100%)
- **Badges**: Status indicators with colored backgrounds and borders
- **Cards**: Consistent InfoBox styling with proper spacing

## Data Source
All information comes from the `PreMoveCandidate` data structure which includes:
- Backend AI analysis (pre_move_ai_score, ignition_probability, expansion_probability)
- Local deterministic calculations (structural pressure, liquidity scores)
- Multi-layer validation system
- Trigger conditions and invalidation levels

## Benefits
✅ Complete visibility into all analysis factors
✅ Clear understanding of why an asset is in a particular state
✅ Detailed layer-by-layer breakdown for validation
✅ Actionable trigger conditions for deployment decisions
✅ Visual progress bars for quick score assessment
✅ Color-coded status indicators for instant recognition

## Files Modified
- `app/src/main/java/com/asc/markets/ui/screens/LiquidityHubScreen.kt`
  - Added `ScoresCard` composable
  - Added `LayersCard` composable with `LayerRow` sub-composable
  - Added `TriggerConditionsCard` composable
  - Added `stateColorForCandidate` helper function
  - Added CircleShape import
  - Updated display order to include new cards

## Date
May 31, 2026
