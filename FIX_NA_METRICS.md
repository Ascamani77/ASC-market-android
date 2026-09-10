# Fix N/A Metrics in Scalping Page

## The Problem
The metrics (Volatility, Momentum, Liquidity, Risk Level) show "N/A" because the API server has old code cached in memory.

## The Root Cause
We fixed a bug where `confluence_score` was being set twice - once with the real value, then overwritten with `None`. The API server needs to be restarted to load the fixed code.

## Solution: Restart the Backend

### Step 1: Stop the API Server
Find the terminal/console running `ai_api.py` and press `Ctrl+C` to stop it.

### Step 2: Stop the Scalping Pipeline  
Find the terminal running the scalping pipeline and press `Ctrl+C` to stop it.

### Step 3: Start the API Server
```bash
cd c:\Users\HP\Documents\NEW_ASC
python ai_api.py
```

Wait for it to show:
```
INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
```

### Step 4: Start the Scalping Pipeline
In a **separate terminal**:
```bash
cd c:\Users\HP\Documents\NEW_ASC
python AI_SYSTEM/SCALPING_ASC_AI/realtime_scalping_pipeline.py --continuous
```

Watch for logs like:
```
[SCALPING] ETHUSDT SELL @ 1633.70 | Conf: 0.95 | Vol: 0.0001 | 
Immediate: -0.04% | Short: -0.04% | Medium: -0.04% | Trend: -0.000 | 
Metrics - VolScore: 0.11, IndScore: 1.00, LiqScore: 0.50, RiskScore: 0.05
```

### Step 5: Test the API
```bash
python test_scalping_api.py
```

You should now see:
```
Volatility Score: 0.1063084285463022
Indicator Score: 1.0
Liquidity Score: 0.49975654010369824
Risk Score: 0.050000000000000044
```

NOT:
```
Volatility Score: None
Indicator Score: None
Liquidity Score: None
Risk Score: None
```

### Step 6: Refresh the Android App
Pull to refresh on the Scalping page. You should now see:
- ✅ **Volatility**: 11% (not N/A)
- ✅ **Momentum**: 100% (not N/A)
- ✅ **Liquidity**: 50% (not N/A)
- ✅ **Risk Level**: 5% (not N/A)

## If Still Showing N/A

1. **Check API logs** - Look for `[SCALPING API]` lines showing the scores
2. **Check scalping pipeline logs** - Look for `Metrics -` in the output
3. **Verify the parquet file** has the fields:
   ```bash
   python -c "import pandas as pd; df = pd.read_parquet('AI_SYSTEM/SCALPING_ASC_AI/surfaces/scalping_signals.parquet'); print(list(df.columns))"
   ```
   Should include: `feeder_volatility_score`, `feeder_indicator_score`, `feeder_liquidity_score`, `feeder_risk_score`

4. **Check Android app is fetching from correct URL**: Should be `http://YOUR_PC_IP:8000/scalping-signals`

## What Was Fixed

### Bug in ai_api.py (Line 1258)
```python
# BEFORE (WRONG):
"confluence_score": None,  # ← This was overwriting the real value!

# AFTER (FIXED):
# confluence_score is already set above - don't override
```

The real values were being set on lines 1198-1223, but then `confluence_score` was being overwritten with `None` on line 1258. This is now fixed.
