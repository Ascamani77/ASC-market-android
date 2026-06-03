# Quick Start: AI News Intelligence

## TL;DR
Your app now uses **ONLY YOUR AI's impact scores** (no Gemini fallback). Just run production mode and rebuild the app.

## One Command to Rule Them All
```powershell
cd c:\Users\HP\Documents\NEW_ASC
.\start_production_live.ps1
```
This will:
- ✅ Run your AI system
- ✅ Export news intelligence for app
- ✅ Update every 15 minutes automatically

## Then Build Your App
In Android Studio:
1. Build > Rebuild Project
2. Run > Run 'app'

## Verify It's Working
Check Android logcat for:
```
✅ NewsService: Loaded AI news intelligence from assets folder
✅ AiService: Using 200 AI-sorted articles from YOUR AI
```

## If You See This (No AI Data)
```
⚠️ NewsService: No AI news intelligence found. Run export_news_for_app.py
```
**Fix**: 
1. Run `python export_news_for_app.py` in NEW_ASC folder
2. Rebuild the app to include the updated assets folder

## Manual Export (If Needed)
```powershell
cd c:\Users\HP\Documents\NEW_ASC
python export_news_for_app.py
```

## Check Current Data
```powershell
cd c:\Users\HP\AndroidStudioProjects\MyRealApp
python verify_ai_news.py
```

## That's It!
Your Analysis & Opinion page now shows impact scores from **ONLY YOUR AI** (no Gemini fallback).

---

**Questions?** Check `TASK_COMPLETE_AI_NEWS_INTEGRATION.md` for full details.
