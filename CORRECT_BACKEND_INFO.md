# Your Backend System - Clarification

## Why NEW_ASC Directory?

**`C:\Users\HP\Documents\NEW_ASC`** is your **complete AI trading system**, not just a simple backend!

### What NEW_ASC Contains:

```
NEW_ASC/
├── ai_api.py                    ← Main API server (port 8003)
├── START.ps1                    ← ONE SCRIPT to start everything
├── ASC_UNIFIED_AI/              ← Your ASC EA and engine
│   ├── ASC_EA.mq5              ← Your MT5 Expert Advisor
│   └── include/
│       └── LiveDataStreamer.mqh ← NEW: Live data streaming
├── AI_SYSTEM/                   ← 8 AI Feeders
│   ├── FEEDERS/
│   │   ├── REGIME_AI/          ← Market regime detection
│   │   ├── VOLATILITY_AI/      ← Volatility analysis
│   │   ├── LIQUIDITY_SMC_AI/   ← Smart Money Concepts
│   │   ├── TIME_AI/            ← Session intelligence
│   │   └── ... (4 more)
│   └── PORTFOLIO_DECISION_AI/   ← Final decision layer
└── Redis                        ← Database for AI surfaces
```

## The Complete System Architecture

```
┌─────────────────────────────────────────────────────────┐
│  MT5 Terminal                                           │
│  ├─ ASC_EA.mq5 (Your Expert Advisor)                   │
│  └─ LiveDataStreamer (streams to app)                  │
└────────────┬────────────────────────────────────────────┘
             ↓
┌─────────────────────────────────────────────────────────┐
│  NEW_ASC System (Your Desktop)                          │
│                                                          │
│  Port 8003: API Server (ai_api.py)                     │
│  ├─ GET /latest-ai           → AI scores               │
│  ├─ GET /live-market-data    → EA live data (NEW!)     │
│  └─ GET /latest-deployments  → Full AI decisions       │
│                                                          │
│  Port 8004: MT5 Data Receiver                          │
│  └─ POST /publish_tick       → Receives MT5 ticks      │
│                                                          │
│  Port 6379: Redis Database                             │
│  └─ Stores AI surfaces (L1-L13)                        │
│                                                          │
│  Background: 8 AI Feeders (run every 2 min)           │
│  ├─ Regime AI    → Market state detection              │
│  ├─ Volatility AI → Expansion/compression              │
│  ├─ Liquidity AI  → SMC analysis                       │
│  └─ ... (5 more layers)                                │
└────────────┬────────────────────────────────────────────┘
             ↓
┌─────────────────────────────────────────────────────────┐
│  MyRealApp (Android)                                    │
│  ├─ Polls port 8003 every 10 seconds                   │
│  ├─ Gets AI scores from /latest-ai                     │
│  └─ Gets live data from /live-market-data (NEW!)       │
└─────────────────────────────────────────────────────────┘
```

## Why Port 8003 (Not 8000)?

Your **NEW_ASC** system uses port **8003** because it's a production-grade system with:

- ✅ **Port 8003**: Main API (AI scores + live data)
- ✅ **Port 8004**: MT5 data receiver
- ✅ **Port 6379**: Redis database

Port 8000 is typically used for simple test servers. Your system is much more sophisticated!

## How to Start Your System

### ✅ The CORRECT Way (ONE Command):

```powershell
cd C:\Users\HP\Documents\NEW_ASC
.\START.ps1
```

This single script:
1. Starts Redis
2. Starts API server (port 8003)
3. Starts MT5 data receiver (port 8004)
4. Starts all 8 AI feeders
5. Keeps everything running

### ❌ WRONG Way:

```powershell
python ai_api.py  # ❌ This won't start Redis or feeders!
```

## Your Other ASC Folders

You have multiple ASC folders - here's what they are:

| Folder | Purpose | Active? |
|--------|---------|---------|
| **NEW_ASC** | ✅ **Main system** - Use this! | **YES** |
| ASC_AI_BOT | Old reference code | No |
| ASC_TRADING_BOT | Old trading bot | No |

## Verification Commands

### Check if NEW_ASC system is running:

```powershell
# Test API server (port 8003)
curl http://localhost:8003/health

# Test live market data endpoint
curl http://localhost:8003/live-market-data

# Test AI scores endpoint
curl http://localhost:8003/latest-ai

# Check Redis
podman ps | findstr redis
```

### Expected Output:

```json
// http://localhost:8003/health
{
  "status": "healthy",
  "redis": "connected",
  "ai_feeders": "running"
}

// http://localhost:8003/live-market-data
{
  "success": true,
  "data_source": "MT5_EA_LIVE",
  "asset_count": 52,
  "assets": [...]
}
```

## App Configuration

Your app is now configured to connect to:

```kotlin
// EALiveDataStore.kt
private const val EA_DATA_URL = "http://10.164.138.133:8003/live-market-data"
                                                      ↑
                                                   Port 8003
```

## Summary

✅ **Use `NEW_ASC` directory** - It's your complete AI system  
✅ **Run `.\START.ps1`** - One command to start everything  
✅ **Port 8003** - Main API server port  
✅ **Port 8004** - MT5 data receiver  
✅ **Port 6379** - Redis database  

Your system is **production-grade** with:
- 8-layer AI pipeline
- Real-time data streaming
- Redis caching
- Complete decision engine

Much more powerful than a simple `python ai_api.py` server! 🚀
