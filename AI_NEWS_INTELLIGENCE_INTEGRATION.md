# AI News Intelligence Integration

## Overview
The Android app now uses **YOUR AI's news intelligence** instead of the external Gemini backend for impact scores and news analysis.

## What Changed

### 1. News Intelligence Export (`export_news_for_app.py`)
- Reads news intelligence from `NEWS_INTELLIGENCE_AI` output
- Exports to JSON format for Android app consumption
- Copies to multiple locations:
  - `C:\Users\HP\AndroidStudioProjects\MyRealApp\ai_news_intelligence.json`
  - `app/src/main/assets/ai_news_intelligence.json` (bundled with app)
  - `app/src/main/res/raw/ai_news_intelligence.json` (alternative location)

### 2. Android App Updates

#### NewsService.kt
- **PRIORITY 1**: Loads news from YOUR AI's `ai_news_intelligence.json`
- **PRIORITY 2**: Falls back to external Gemini API if AI file not found
- Checks multiple locations:
  1. Assets folder (bundled with app)
  2. External storage locations
- Logs which source is being used (AI vs external API)

#### AiService.kt
- **PRIORITY 1**: Loads AI-sorted news from YOUR AI's intelligence file
- **PRIORITY 2**: Falls back to external Gemini API
- Sorts by impact score (highest first)
- Same multi-location checking as NewsService

#### NewsViewModel.kt
- Updated to accept Context parameter
- Added `NewsViewModelFactory` for proper Context injection
- Services now have access to app's assets folder

#### UI Screens
- `AnalysisOpinionScreen.kt`: Uses factory to inject Context
- `MainScreen.kt`: Uses factory to inject Context

### 3. Production Script (`start_production_live.ps1`)
- Automatically exports news intelligence after full pipeline runs
- Runs on startup and every 15 minutes
- Ensures app always has latest AI intelligence

## How It Works

### Data Flow
```
NEWS_INTELLIGENCE_AI (Python)
    ↓
D2_V1_news_intelligence_enriched.parquet
    ↓
export_news_for_app.py
    ↓
ai_news_intelligence.json (multiple locations)
    ↓
Android App (NewsService/AiService)
    ↓
Analysis & Opinion Page (with YOUR AI's impact scores)
```

### Impact Score Source
- **Before**: External Gemini backend API
- **After**: YOUR AI's NEWS_INTELLIGENCE_AI feeder

### Impact Score Calculation (in NEWS_INTELLIGENCE_AI)
- Event type (40%): CENTRAL_BANK_DECISION (0.95) > MAJOR_DATA_RELEASE (0.85) > etc.
- Source authority (20%): Fed/ECB/BoE (1.0) > Reuters/Bloomberg (0.85) > etc.
- Asset relevance (20%): Direct mention vs indirect
- Timing (10%): Recent (1.0) to old (0.1)
- Surprise detection (10%): Keywords like "surprise", "shock", "beat"

## Current Data
- **515 unique news articles** analyzed by YOUR AI
- **2,855 asset-specific records** (each article analyzed for multiple assets)
- **Impact distribution**:
  - High impact (≥0.7): 0 articles (0%)
  - Medium impact (0.4-0.7): 272 articles (52.8%)
  - Low impact (<0.4): 243 articles (47.2%)

## Usage

### Manual Export
```powershell
cd c:\Users\HP\Documents\NEW_ASC
python export_news_for_app.py
```

### Automatic Export (Production)
```powershell
cd c:\Users\HP\Documents\NEW_ASC
.\start_production_live.ps1
```
The production script will:
1. Run full AI pipeline on startup
2. Export news intelligence immediately
3. Re-export every 15 minutes after full pipeline runs

### Verify in App
Check Android logcat for these messages:
- ✅ `NewsService: Loaded AI news intelligence from assets folder`
- ✅ `AiService: Using X AI-sorted articles from YOUR AI`
- ⚠️ `NewsService: Using X articles from external API (fallback)` (if AI file not found)

## Benefits
1. **Your AI's Intelligence**: Impact scores come from YOUR trading-specific AI logic
2. **No External Dependency**: Works offline if AI file is bundled
3. **Automatic Updates**: Production script keeps data fresh
4. **Graceful Fallback**: Uses external API if AI file unavailable
5. **Asset-Specific Analysis**: Each news item analyzed for all 28 assets

## Next Steps
1. Build and deploy the Android app with the updated code
2. Run `.\start_production_live.ps1` to generate fresh AI intelligence
3. Verify in app that impact scores are from YOUR AI (check logcat)
4. Monitor that news intelligence updates every 15 minutes

## Files Modified
- `c:\Users\HP\Documents\NEW_ASC\export_news_for_app.py`
- `c:\Users\HP\Documents\NEW_ASC\start_production_live.ps1`
- `app\src\main\java\com\researchcenter\services\NewsService.kt`
- `app\src\main\java\com\researchcenter\services\AiService.kt`
- `app\src\main\java\com\researchcenter\ui\viewmodel\NewsViewModel.kt`
- `app\src\main\java\com\researchcenter\ui\screens\AnalysisOpinionScreen.kt`
- `app\src\main\java\com\researchcenter\ui\screens\MainScreen.kt`

## Files Created
- `c:\Users\HP\AndroidStudioProjects\MyRealApp\ai_news_intelligence.json`
- `app\src\main\assets\ai_news_intelligence.json`
- `app\src\main\res\raw\ai_news_intelligence.json`
