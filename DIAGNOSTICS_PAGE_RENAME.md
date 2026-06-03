# Diagnostics Page - Name Update

## Change Made
Renamed the page from **"Micro-Jitter Monitor"** to **"System Diagnostics"** in navigation menus.

## Reason
The page now contains TWO major sections:
1. **Micro-Jitter Monitor** - Tick instability detection
2. **AI Decision Diagnostics** - Backend AI pipeline status

The old name "Micro-Jitter Monitor" only described the first section, making it misleading since the page also shows comprehensive AI decision diagnostics.

## What Changed

### Sidebar Menu:
- **Before**: "Micro-Jitter Monitor"
- **After**: "System Diagnostics"

### Quick Access:
- **Before**: "Micro-Jitter Monitor"
- **After**: "System Diagnostics"

### Icon:
- Kept the same: Shield icon (Icons.Default.Shield)

### View:
- Same: `AppView.DIAGNOSTICS`

## Why "System Diagnostics"?

The name "System Diagnostics" better represents the page because it shows:

1. **Micro-Jitter Diagnostics**:
   - Pre-ignition tick instability
   - Tick burst analysis
   - Spread jitter
   - Micro volatility
   - Feed validity

2. **AI Decision Diagnostics**:
   - Final trade state
   - AI confidence scores
   - Feeder gate status (Entry, Confluence, Plan, Execution, Signal Quality, Risk)
   - Decision reasoning

3. **Integration Status**:
   - Alignment between jitter and AI
   - System health overview

## Alternative Names Considered
- ❌ "Micro-Jitter Monitor" - Too narrow, only describes first section
- ❌ "AI & Jitter Diagnostics" - Too technical, awkward
- ❌ "Pre-Move Diagnostics" - Doesn't cover AI decision pipeline
- ✅ **"System Diagnostics"** - Clear, comprehensive, professional

## Files Modified
1. `app/src/main/java/com/asc/markets/ui/components/Sidebar.kt`
   - Updated menu item text

2. `app/src/main/java/com/asc/markets/data/QuickAccessManager.kt`
   - Updated quick access item text and ID

## User Impact
- ✅ More accurate description of page content
- ✅ Users know they'll see both jitter AND AI diagnostics
- ✅ Professional naming that matches enterprise trading platforms
- ✅ No functional changes - same page, better name

## Date
May 31, 2026
