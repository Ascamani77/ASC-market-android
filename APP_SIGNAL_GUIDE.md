# Trading Signal Guide - Where to Find Bullish/Bearish Signals

## Overview
Your app uses an AI-powered decision system that analyzes markets and provides trading signals. This guide explains where to find these signals and what they mean.

---

## 🎯 Main Signal Locations

### 1. **Market Overview Tab** (Primary Signal Page)
**Location:** Dashboard → Market Overview Tab

#### What You'll See:
- **Universal Overview Box** - Shows the AI's current bias (Bullish/Bearish)
- **Bias Label** - Displays: "BULLISH", "BEARISH", or "NEUTRAL"
- **Confidence Score** - Shows how confident the AI is (0-100%)
- **Direction Reason** - Explains why the AI chose this direction

#### How to Read It:
```
Bias: BULLISH (Green) → Buy Signal
Confidence: 85% → High confidence
Reason: "EUR/USD momentum and liquidity bias"
```

```
Bias: BEARISH (Red) → Sell Signal  
Confidence: 72% → Medium-high confidence
Reason: "Selling pressure and flow detected"
```

```
Bias: NEUTRAL (White) → No Trade
Confidence: 45% → Low confidence
Reason: "Mixed signals, wait for clarity"
```

---

### 2. **Pre-Move AI Section** (Detailed Analysis)
**Location:** Dashboard → Scroll down to Pre-Move AI Mock section

#### What You'll See:
- **AI Pre-Move Score** (0-100%)
  - 0-30%: NOISE (No Trade)
  - 30-45%: STRUCTURE (Forming)
  - 45-60%: COMPRESSION (Building)
  - 60-80%: PRE-MOVE (Ready)
  - 80-100%: EXPANSION (Execute!)

- **Phase Indicator**
  - Shows current market phase
  - Color-coded for easy identification

- **Entry Style Section**
  - Tells you HOW to enter (Momentum Pullback, Displacement, etc.)
  - Shows which entry style matches current conditions

#### How to Use It:
1. **Check the Score:**
   - Below 60% → Wait, no trade yet
   - 60-80% → Prepare, signal building
   - Above 80% → Execute, strong signal

2. **Check the Phase:**
   - NOISE/STRUCTURE → Too early
   - COMPRESSION → Getting ready
   - PRE-MOVE → Signal forming
   - EXPANSION → Trade now!

3. **Check Entry Style:**
   - Shows the best way to enter the trade
   - "HIGH MATCH" = Use this method

---

### 3. **Asset List** (Quick Scan)
**Location:** Dashboard → Market Overview → Asset rows

#### What You'll See:
Each asset row shows:
- **Pre-Move Score** (percentage)
- **State** (NOISE, STRUCTURE, COMPRESSION, PRE-MOVE, EXPANSION)
- **Color Indicator**
  - Red: NOISE (No trade)
  - Orange: STRUCTURE (Forming)
  - Yellow: COMPRESSION (Building)
  - Green: PRE-MOVE (Ready)
  - Blue: EXPANSION (Execute)

#### How to Use It:
- Scroll through assets
- Look for GREEN or BLUE colors
- Click on asset to see full details

---

### 4. **Event Stream** (Real-Time Updates)
**Location:** Dashboard → Event Stream Tab

#### What You'll See:
- Real-time AI decisions
- Market events as they happen
- Signal changes and updates

#### How to Use It:
- Monitor for new signals
- See when AI changes its mind
- Track signal evolution

---

## 📊 Understanding the AI Decision System

### The AI Analyzes:
1. **Price Action** - How price is moving
2. **Volatility** - Market energy/momentum
3. **Structure** - Support/resistance levels
4. **Confluence** - Multiple factors aligning
5. **Liquidity** - Market depth and flow
6. **Session** - Time of day effects
7. **Macro Events** - News and economic data

### The AI Outputs:
1. **Direction** - Bullish, Bearish, or Neutral
2. **Confidence** - How sure it is (0-100%)
3. **Phase** - Where in the cycle we are
4. **Entry Style** - How to enter the trade
5. **Key Levels** - Important price points
6. **Invalidation** - When the signal is wrong

---

## 🚦 Signal Interpretation Guide

### Strong Bullish Signal
```
✅ Bias: BULLISH
✅ Confidence: 75%+
✅ Phase: PRE-MOVE or EXPANSION
✅ Entry Style: HIGH MATCH
✅ Color: Green or Blue
→ ACTION: Look for buy opportunities
```

### Strong Bearish Signal
```
✅ Bias: BEARISH
✅ Confidence: 75%+
✅ Phase: PRE-MOVE or EXPANSION
✅ Entry Style: HIGH MATCH
✅ Color: Green or Blue
→ ACTION: Look for sell opportunities
```

### Weak/No Signal
```
❌ Bias: NEUTRAL
❌ Confidence: <60%
❌ Phase: NOISE or STRUCTURE
❌ Entry Style: WATCH
❌ Color: Red or Orange
→ ACTION: Wait, don't trade yet
```

---

## 📱 Step-by-Step: Finding a Signal

### Method 1: Quick Scan
1. Open app → Go to Dashboard
2. Look at **Market Overview Tab**
3. Scroll through asset list
4. Find assets with **GREEN** or **BLUE** colors
5. Click on asset to see details
6. Check **Bias** and **Confidence**
7. If both are strong → You have a signal!

### Method 2: Deep Analysis
1. Open app → Go to Dashboard
2. Select an asset you want to trade
3. Scroll down to **Pre-Move AI Section**
4. Check the **AI Pre-Move Score**
5. Read the **Phase Details**
6. Look at **Entry Style** recommendations
7. Check **Bias Engine** for direction
8. If everything aligns → You have a signal!

### Method 3: Real-Time Monitoring
1. Open app → Go to **Event Stream**
2. Watch for new AI decisions
3. Look for "EXPANSION" or "PRE-MOVE" events
4. Click on event to see full details
5. Check if it matches your trading criteria

---

## 🎨 Color Code Reference

| Color | Phase | Score | Action |
|-------|-------|-------|--------|
| 🔴 Red | NOISE | 0-30% | ❌ No Trade |
| 🟠 Orange | STRUCTURE | 30-45% | ⏳ Wait |
| 🟡 Yellow | COMPRESSION | 45-60% | 👀 Watch |
| 🟢 Green | PRE-MOVE | 60-80% | ✅ Prepare |
| 🔵 Blue | EXPANSION | 80-100% | 🚀 Execute |

---

## 💡 Pro Tips

### 1. Don't Chase
- If score is already at 95%+, you might be late
- Best entries are at 65-85% (PRE-MOVE to early EXPANSION)

### 2. Check Multiple Timeframes
- Swipe between charts (Pre-Move and Volatility)
- Both should align for strongest signals

### 3. Use Confluence
- Don't rely on just one indicator
- Check: Bias + Confidence + Phase + Entry Style
- All should agree

### 4. Respect Invalidation
- Every signal has an invalidation level
- If price hits it, the signal is wrong
- Exit immediately

### 5. Monitor Event Stream
- Real-time updates are crucial
- AI can change its mind quickly
- Stay informed

---

## 🔍 Common Questions

### Q: Where is the main signal page?
**A:** Dashboard → Market Overview Tab → Universal Overview Box (top section)

### Q: What does "journal_direction" mean?
**A:** It's the AI's directional bias: "BULLISH", "BEARISH", or "NEUTRAL"

### Q: When should I actually trade?
**A:** When:
- Bias is clear (BULLISH or BEARISH)
- Confidence is 70%+
- Phase is PRE-MOVE or EXPANSION
- Entry Style shows "HIGH MATCH"

### Q: What if signals conflict?
**A:** Don't trade. Wait for clarity. Conflicting signals = uncertainty.

### Q: How often do signals update?
**A:** Real-time. The AI continuously analyzes and updates.

### Q: Can I get notifications for signals?
**A:** Currently, you need to check the app. (Feature request: Add push notifications)

---

## 🎯 Quick Reference Card

### Where to Look:
1. **Market Overview Tab** → Bias + Confidence
2. **Pre-Move AI Section** → Score + Phase
3. **Asset List** → Quick color scan
4. **Event Stream** → Real-time updates

### What to Look For:
- ✅ Clear bias (BULLISH/BEARISH)
- ✅ High confidence (70%+)
- ✅ Active phase (PRE-MOVE/EXPANSION)
- ✅ Green or Blue color
- ✅ Entry style match

### When to Trade:
- All indicators align
- Confidence is high
- Phase is active
- Entry style is clear

### When NOT to Trade:
- Bias is NEUTRAL
- Confidence is low (<60%)
- Phase is NOISE or STRUCTURE
- Signals conflict

---

## 📈 Example Walkthrough

### Scenario: Looking for EUR/USD Signal

1. **Open App** → Dashboard
2. **Go to Market Overview Tab**
3. **Find EUR/USD** in asset list
4. **Check Color** → Green (Good sign!)
5. **Click on EUR/USD** → Opens details
6. **Check Universal Overview Box:**
   - Bias: BULLISH ✅
   - Confidence: 84% ✅
   - Reason: "Momentum and liquidity bias" ✅
7. **Scroll to Pre-Move AI:**
   - Score: 78% ✅
   - Phase: PRE-MOVE ✅
   - Entry Style: Momentum Pullback (HIGH MATCH) ✅
8. **Decision:** Strong bullish signal → Look for buy entry

---

## 🚀 Next Steps

1. **Explore the App:**
   - Open each section mentioned above
   - Familiarize yourself with the layout
   - Practice finding signals

2. **Paper Trade First:**
   - Don't use real money yet
   - Track signals and outcomes
   - Build confidence

3. **Understand Your Risk:**
   - Every signal has risk
   - Use stop losses (invalidation levels)
   - Don't risk more than you can afford to lose

4. **Keep Learning:**
   - Watch how signals evolve
   - Note what works and what doesn't
   - Refine your strategy

---

## 📞 Need Help?

If you're still confused:
1. Take screenshots of what you see
2. Ask specific questions about each section
3. We can walk through the app together

Remember: The AI is a tool, not a guarantee. Always use proper risk management!
