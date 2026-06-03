# Chart Analysis Node - Enhanced with Groq AI

## Problem
The Chart Analysis Node was throwing errors after image upload. The AI was supposed to:
1. Analyze the uploaded chart image
2. Compare with current market data for the asset
3. Use user's trading parameters
4. Provide actionable trading signals and advice

## Root Cause
The original implementation only analyzed the chart image in isolation without:
- Current live market data comparison
- User account parameters (balance, risk settings, active trades)
- Actionable trading signals (entry, stop loss, take profit)

## Solution Implemented

### 1. Enhanced Groq Prompt (AscAiTextExplainer.kt)
Updated `explainChartAnalysis()` to accept additional parameters:
- `currentMarketData`: Live price data, trends, and AI deployments
- `userParameters`: Account balance, active trades, risk settings

**New Prompt Structure:**
```
🎯 SIGNAL: [LONG/SHORT/NEUTRAL]

📊 CHART ANALYSIS:
[What the uploaded chart shows]

📈 CURRENT MARKET:
[How current price action compares to the chart]

💡 TRADE SETUP:
• Entry: [specific level or condition]
• Stop Loss: [specific level]
• Take Profit: [target zones]
• Confidence: [0-100%]

🔍 REASONING:
[3-4 bullets explaining the setup]

⚠️ RISK NOTE:
[Any warnings or conditions to watch]
```

### 2. Market Data Integration (ChartAnalysisViewModel.kt)
Added `buildCurrentMarketData()` function that gathers:

**For Each Asset in Chart:**
- Current live price from MarketDataStore/BinanceDataStore
- 24h price change percentage
- Price direction (UP/DOWN)
- Recent price trend (RISING/FALLING)
- Price range from recent history

**Current AI Deployments:**
- AI direction and label
- Pre-move score
- Ignition probability
- Risk state

### 3. User Parameters Integration (ChartAnalysisViewModel.kt)
Added `buildUserParameters()` function that gathers:

**Account Information:**
- Account balance
- Account equity
- Free margin
- Margin level

**Active Trades:**
- Number of active trades
- Current exposure (symbol, type, volume, entry price)

**Risk Management:**
- Max concurrent trades
- Risk per trade (1-2%)
- Stop loss requirements

**Context:**
- Current view
- Selected asset and price

### 4. Updated Analysis Flow

**Before:**
```
Upload Image → ASC AI Analyzes → Groq Summarizes → Show Result
```

**After:**
```
Upload Image → ASC AI Analyzes → Gather Market Data → Gather User Params → 
Groq Compares & Generates Signal → Show Actionable Result
```

## Key Features

### 1. Real-Time Market Comparison
- Compares chart analysis with current live prices
- Identifies if chart is outdated or still valid
- Shows price movement since chart was captured

### 2. Actionable Trading Signals
- Clear LONG/SHORT/NEUTRAL bias
- Specific entry levels or trigger conditions
- Defined stop loss levels
- Target profit zones
- Confidence percentage

### 3. Risk-Aware Recommendations
- Considers current account balance
- Factors in existing exposure
- Respects risk management rules
- Warns about conflicting positions

### 4. Persona-Based Analysis
Uses the selected analyst persona (Macro, SMC, Liquidity, Algo, etc.) to:
- Prioritize different aspects of the analysis
- Use persona-specific terminology
- Focus on relevant indicators

## Data Sources

### Primary Price Data
1. **MarketDataStore** - Pepperstone cTrader (FOREX, commodities, indices, crypto)
2. **BinanceDataStore** - Binance (USDT pairs only)

### AI Analysis
1. **ASC AI Backend** - Proprietary vision/analysis engine for chart image
2. **Groq API** - Llama 3.3 70B for natural language explanation and signal generation

## Usage

### 1. Upload Chart
- Take a screenshot from MT5 or TradingView
- Upload via the Chart Analysis Node page
- Chart should show the asset symbol clearly

### 2. Run Analysis
- Click "RUN ASC VISION ANALYSIS"
- ASC AI processes the image
- Groq compares with current market
- Generates actionable trading signal

### 3. Review Signal
- Check the SIGNAL (LONG/SHORT/NEUTRAL)
- Review CHART ANALYSIS vs CURRENT MARKET
- Evaluate the TRADE SETUP (entry, SL, TP)
- Read the REASONING
- Note any RISK WARNINGS

### 4. Execute (Optional)
- Use the signal as guidance
- Adjust based on your risk tolerance
- Monitor the suggested levels
- Follow your trading plan

## Error Handling

### "GROQ_API_KEY not configured"
**Solution**: Add Groq API key to `local.properties`:
```
GROQ_API_KEY=gsk_your_key_here
```
Then rebuild the app.

### "No current market data available"
**Solution**: 
- Ensure cTrader bridge is running: `.\start_ctrader_bridge.ps1`
- Check Binance WebSocket connection
- Verify the asset symbol in the chart is supported

### "ASC AI failed to analyze chart"
**Solution**:
- Check ASC AI backend is running
- Verify network connectivity
- Ensure chart image is clear and readable

### "ForexViewModel not available"
**Solution**: This is a code issue - ForexViewModel should be passed to the analysis function

## Technical Details

### Files Modified
1. **AscAiTextExplainer.kt**
   - Enhanced `explainChartAnalysis()` with market data and user parameters
   - Updated prompt to generate actionable trading signals
   - Added structured output format

2. **ChartAnalysisViewModel.kt**
   - Added `buildCurrentMarketData()` function
   - Added `buildUserParameters()` function
   - Updated `analyzeChart()` to accept ForexViewModel
   - Integrated market data and user parameters into analysis

3. **ChartAnalysisScreen.kt**
   - Updated button click to pass ForexViewModel
   - No UI changes (output format handled by Groq)

### Dependencies
- **Groq API**: Llama 3.3 70B Versatile model
- **ASC AI Backend**: Chart image analysis
- **MarketDataStore**: Live price data
- **BinanceDataStore**: USDT pair data
- **ForexViewModel**: Account and trade data

## Testing Checklist

- [ ] Upload a chart screenshot
- [ ] Verify ASC AI processes the image
- [ ] Check that current market data is included
- [ ] Verify user parameters are gathered
- [ ] Confirm Groq generates a structured signal
- [ ] Review signal format (SIGNAL, CHART, MARKET, SETUP, REASONING, RISK)
- [ ] Test with different personas (Macro, SMC, Liquidity, etc.)
- [ ] Verify error handling for missing data

## Example Output

```
🎯 SIGNAL: LONG

📊 CHART ANALYSIS:
The uploaded chart shows BTCUSD forming a bullish flag pattern on H1 timeframe. 
Price consolidated after a strong upward move, with higher lows indicating 
accumulation. Key support at 73,500 held multiple tests.

📈 CURRENT MARKET:
Current price: 74,019.43 (+0.85% from chart)
Market has broken above the flag resistance at 73,800, confirming the bullish 
continuation pattern. Volume is increasing on the breakout.

💡 TRADE SETUP:
• Entry: 74,000 - 74,200 (current zone) or pullback to 73,800
• Stop Loss: 73,400 (below flag support)
• Take Profit: TP1: 74,800 | TP2: 75,500 | TP3: 76,200
• Confidence: 78%

🔍 REASONING:
• Bullish flag breakout confirmed with volume
• Higher timeframe trend is bullish (H4/D1 aligned)
• Pre-move AI score: 82% with high ignition probability
• Risk/reward ratio: 1:3 to first target

⚠️ RISK NOTE:
Watch for rejection at 74,500 (previous resistance). If price closes below 
73,800, the pattern is invalidated. Consider scaling out at each target.
```

## Future Enhancements

1. **Multi-Timeframe Analysis**: Compare chart timeframe with higher/lower TFs
2. **Historical Pattern Matching**: Find similar setups in historical data
3. **Correlation Analysis**: Check correlated assets for confirmation
4. **News Integration**: Factor in upcoming economic events
5. **Backtesting**: Show historical performance of similar setups
6. **Auto-Trade Integration**: Option to execute signal automatically

## Notes

- The analysis is as good as the chart image quality
- Current market data is essential for accurate signals
- Always verify the signal against your own analysis
- This is a tool to assist, not replace, your trading decisions
- Risk management is paramount - never risk more than you can afford to lose
