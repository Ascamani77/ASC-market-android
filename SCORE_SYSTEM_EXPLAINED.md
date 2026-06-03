# AI Score System Explained

## Two Types of Scores

Your app now displays **two different scores** depending on the asset's state:

### 1. Pre-Move AI Score (0-25%)
**When:** Asset is REJECTED (not ready to trade)
**Purpose:** Shows incremental progress toward trade readiness
**Range:** 0% to ~25%
**Source:** `pre_move_ai_score` field

This score is calculated from feeder states:
- Volatility state and score
- Structure score
- Chart context score
- Confluence score
- Entry quality score
- Expansion probability

**Example Current Scores:**
- ETHUSDT: 24.77% (highest progress)
- ETHUSD: 16.77%
- BTCUSD: 15.94%
- EURUSD: 13.25%
- GBPUSD: 12.78%
- XAGUSD: 12.64%
- XAUUSD: 12.15%
- USDJPY: 10.80%
- BTCUSDT: 9.82%
- EURJPY: 7.55%
- EURGBP: 7.31%
- BRENTCMDUSD: 7.31%
- USDCAD: 7.17%

### 2. Final Trade Score (68-100%)
**When:** Asset is TRADE_CANDIDATE (ready to trade)
**Purpose:** Shows trade conviction level
**Range:** 68% to 100%
**Source:** `final_trade_score` field

This score only appears when ALL 6 critical gates pass:
- ✓ ENTRY gate = READY
- ✓ CONFLUENCE gate = TRADEABLE_SETUP
- ✓ PLAN gate = PLAN_READY
- ✓ EXECUTION gate = READY
- ✓ SIGNAL QUALITY gate = STRONG_SIGNAL or ELITE_SIGNAL
- ✓ RISK gate ≠ RISK_OFF

**Score Levels:**
- 68-75%: Minimum tradeable setup
- 76-85%: Strong setup
- 86-95%: High conviction
- 96-100%: Elite setup

## How the App Displays Scores

### Market Overview Tab
Shows the appropriate score for each asset:
- **REJECTED assets**: Pre-Move AI Score (7-25%)
- **TRADE_CANDIDATE assets**: Final Trade Score (68-100%)

### Pre-Move AI Page
Shows the appropriate score in the header and charts:
- **REJECTED assets**: Pre-Move AI Score with fractional values
- **TRADE_CANDIDATE assets**: Final Trade Score

### AI Status Tab
Shows the appropriate score on each asset card:
- **REJECTED assets**: Pre-Move AI Score (shows progress)
- **TRADE_CANDIDATE assets**: Final Trade Score (shows conviction)

## Understanding the Progression

### Stage 1: REJECTED (0-25%)
**Current State:** All 14 assets are here
**Score Type:** Pre-Move AI Score
**What it means:** Asset is building structure but not ready yet

**Score Breakdown:**
- 0-7%: NOISE phase (very early, minimal structure)
- 8-15%: Early structure forming
- 16-25%: Structure building, getting closer

**Example:** ETHUSDT at 24.77% is the closest to being ready, but still needs all 6 gates to pass.

### Stage 2: TRADE_CANDIDATE (68-100%)
**Current State:** No assets here yet
**Score Type:** Final Trade Score
**What it means:** All 6 gates passed, ready to trade

**Score Breakdown:**
- 68-75%: Minimum tradeable (proceed with caution)
- 76-85%: Strong setup (good risk/reward)
- 86-95%: High conviction (excellent setup)
- 96-100%: Elite setup (rare, highest conviction)

## The Gap (25-68%)

You'll notice there's a gap between 25% and 68%. This is intentional:

- **Below 25%**: Asset is REJECTED, building structure
- **Above 68%**: Asset is TRADE_CANDIDATE, all gates passed

**The gap represents the binary nature of the system:**
- Either the asset is NOT ready (REJECTED, 0-25%)
- Or it IS ready (TRADE_CANDIDATE, 68-100%)

There's no "in-between" state. The system is deterministic - all conditions must be met.

## What Changed

### Before (Showing 0.0%)
```
ETHUSDT: 0.0%
BTCUSD: 0.0%
EURUSD: 0.0%
```

### After (Showing Fractional Progress)
```
ETHUSDT: 24.77%
BTCUSD: 15.94%
EURUSD: 13.25%
```

Now you can see which assets are making the most progress toward trade readiness!

## Interpreting the Scores

### High Pre-Move Score (20-25%)
**Example:** ETHUSDT at 24.77%
**Meaning:** 
- Strong volatility structure
- Good chart context
- Building confluence
- But still missing critical gates

**What to watch:**
- Entry state (currently NO_ENTRY)
- Signal quality (currently NO_SIGNAL_QUALITY)
- Risk state (currently CAPITAL_PRESERVATION)

### Medium Pre-Move Score (10-20%)
**Example:** BTCUSD at 15.94%
**Meaning:**
- Some structure present
- Moderate volatility
- Partial confluence
- Multiple gates still blocked

### Low Pre-Move Score (0-10%)
**Example:** USDCAD at 7.17%
**Meaning:**
- Minimal structure
- Low volatility
- Weak confluence
- All gates blocked

## When Will You See 68%+?

A score will jump from ~25% to 68%+ when:

1. **Entry Gate Passes**
   - Entry state changes from NO_ENTRY to READY
   - Price action confirms entry setup

2. **Confluence Gate Passes**
   - Multiple timeframes align
   - Confluence state = TRADEABLE_SETUP

3. **Plan Gate Passes**
   - Trade plan is generated
   - Plan state = PLAN_READY

4. **Execution Gate Passes**
   - Execution conditions met
   - Execution status = READY

5. **Signal Quality Gate Passes**
   - Signal quality = STRONG_SIGNAL or ELITE_SIGNAL
   - High confidence directional bias

6. **Risk Gate Passes**
   - Risk state ≠ RISK_OFF
   - Portfolio risk acceptable

**When all 6 pass simultaneously → Score jumps to 68%+**

## Visual Indicators

### Score Colors
- **0-15%**: Gray (NOISE)
- **16-30%**: Light Gray (STRUCTURE forming)
- **31-45%**: Yellow (COMPRESSION)
- **46-67%**: Orange (PRE-MOVE building)
- **68-100%**: Green (TRADE_CANDIDATE ready)

### Phase Labels
Based on score:
- 0-29%: NOISE
- 30-44%: STRUCTURE
- 45-59%: COMPRESSION
- 60-79%: PRE-MOVE
- 80-100%: EXPANSION

## Summary

✅ **Now showing fractional scores** (7.17% to 24.77%)
✅ **Can see which assets are closest** to being ready
✅ **Understand incremental progress** toward trade signals
✅ **Two-tier system**: Pre-Move (0-25%) → Final Trade (68-100%)

The system is working perfectly - you're now seeing the granular progress of each asset as it builds toward a trade signal!
