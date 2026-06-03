# Macro Stream Trading Focus Update

## Changes Made

### 1. **Reduced RSS Feeds (40+ → 14 feeds)**
Removed non-trading sources and kept only forex, crypto, and commodities-relevant feeds:

#### **Forex** (3 feeds):
- ForexLive
- MyFxBook Economic Calendar
- FX Street

#### **Central Banks** (7 feeds):
- Fed Monetary Policy only (removed speeches & general press)
- ECB Press Releases
- Bank of England
- Swiss National Bank
- Bank of Japan
- Reserve Bank of Australia
- Bank of Korea

#### **Commodities** (4 feeds):
- EIA Weekly Petroleum Status
- IEA News
- OPEC
- CFTC (Commitments of Traders)

#### **Crypto** (0 feeds currently):
- Placeholder added for crypto RSS feeds
- Can add: CoinTelegraph, CoinDesk, etc.

#### **Macro Trading** (2 feeds):
- St. Louis Fed (economic data)
- IMF (global macro)

### 2. **Smart Content Filtering**
Added keyword-based filtering to ensure only trading-relevant news appears:

#### **Include Keywords** (50+ terms):
- **Forex**: EUR, USD, GBP, JPY, CHF, AUD, NZD, CAD, forex, currency, exchange rate, fx
- **Commodities**: gold, silver, oil, crude, WTI, Brent, copper, natural gas, platinum
- **Crypto**: bitcoin, BTC, ethereum, ETH, crypto, cryptocurrency, blockchain
- **Macro**: interest rate, inflation, CPI, NFP, GDP, PMI, employment, retail sales
- **Central Banks**: Fed, ECB, BOE, BOJ, rate hike, rate cut, QE, monetary policy
- **Market Terms**: volatility, liquidity, trading, rally, selloff, bullish, bearish

#### **Exclude Keywords**:
- climate, sustainability, ESG, diversity, inclusion
- conference, seminar, workshop, webinar, event
- appointment, resignation, hiring, staff
- regulation (unless paired with trading keywords)

### 3. **Updated Categories**
Changed from generic macro to trading-specific:
- ~~All Macro~~ → **All Trading**
- ~~Energy & Commodities~~ → **Commodities**
- Added: **Forex** 💱
- Added: **Crypto** ₿
- ~~Macro Calendar~~ → Removed
- ~~Gov & Reg~~ → Removed

### 4. **Updated Trending Topics**
Changed from institutional/academic to trader-focused:
- Fed Interest Rate Decision
- ECB Monetary Policy
- NFP Employment Report
- US CPI Inflation Data
- Crude Oil Inventory
- Gold Price Outlook
- Bitcoin Volatility
- USD Index Movement
- OPEC Production Cuts
- Central Bank Rate Hikes

## How It Works

1. **NewsService.fetchAllNews()** now:
   - Loads AI-analyzed news (priority)
   - Fetches from 14 focused RSS feeds
   - Filters ALL articles through `isTradingRelevant()` function
   - Logs filtering stats: "Filtered X articles down to Y trading-relevant articles"

2. **Filtering Logic**:
   - Articles with AI asset tags → Always included
   - Articles with trading keywords → Included
   - Articles with exclude keywords → Rejected (unless strong trading keyword present)

3. **Result**: Only forex, crypto, and commodities news reaches the Macro Stream

## Testing

After rebuilding the app, you should see:
- Fewer total articles in Macro Stream
- All articles related to currencies, commodities, or crypto
- No climate, ESG, conference, or HR news
- Log messages showing filter effectiveness

## Future Enhancements

1. **Add Crypto RSS Feeds**:
   ```kotlin
   "crypto" to listOf(
       "https://cointelegraph.com/rss",
       "https://www.coindesk.com/arc/outboundfeeds/rss/",
       "https://cryptonews.com/news/feed/"
   )
   ```

2. **Adjust Keyword Sensitivity**:
   - Add more specific forex pairs (EURGBP, USDJPY, etc.)
   - Add commodity-specific terms (Brent crude, WTI, spot gold)
   - Add crypto altcoins (SOL, ADA, XRP, etc.)

3. **AI Intelligence Priority**:
   - Ensure your `ai_news_intelligence.json` focuses on trading assets
   - AI-tagged articles bypass keyword filtering

## Files Modified

1. `app/src/main/java/com/researchcenter/util/Constants.kt`
   - Reduced RSS_FEEDS from 40+ to 14 sources
   - Added TRADING_KEYWORDS list
   - Added EXCLUDE_KEYWORDS list
   - Updated CATEGORIES
   - Updated TRENDING_TOPICS

2. `app/src/main/java/com/researchcenter/services/NewsService.kt`
   - Added `isTradingRelevant()` filtering function
   - Applied filter to all fetched articles
   - Added logging for filter effectiveness
