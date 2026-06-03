package com.researchcenter.util

import com.researchcenter.data.models.NewsCategory

object Constants {
    const val BASE_URL = "https://ais-dev-4tncuadz4nd7q6igo5rb7z-473892111102.europe-west2.run.app"

    // NOTE: Sync this with /sources.json for cross-platform parity
    val CATEGORIES = listOf(
        NewsCategory("all", "All Trading", "🌍"),
        NewsCategory("forex", "Forex", "💱"),
        NewsCategory("commodities", "Commodities", "🛢️"),
        NewsCategory("crypto", "Crypto", "₿"),
        NewsCategory("central-banks", "Central Banks", "🏦"),
        NewsCategory("macro-trading", "Macro Data", "📉")
    )

    val TRENDING_TOPICS = listOf(
        "Fed Interest Rate Decision",
        "ECB Monetary Policy",
        "NFP Employment Report",
        "US CPI Inflation Data",
        "Crude Oil Inventory",
        "Gold Price Outlook",
        "Bitcoin Volatility",
        "USD Index Movement",
        "OPEC Production Cuts",
        "Central Bank Rate Hikes"
    )

    val RSS_FEEDS = mapOf(
        "forex" to listOf(
            "https://www.forexlive.com/feed",
            "https://www.myfxbook.com/rss/forex-economic-calendar-events",
            "https://www.fxstreet.com/rss/news"
        ),
        "central-banks" to listOf(
            // Only monetary policy & interest rate decisions (forex-relevant)
            "https://www.federalreserve.gov/feeds/press_monetary.xml",
            "https://www.ecb.europa.eu/rss/press.html",
            "https://www.bankofengland.co.uk/rss/news",
            "https://www.snb.ch/public/en/rss/pressrel",
            "https://www.boj.or.jp/en/rss/whatsnew.xml",
            "https://www.rba.gov.au/rss/rss-cb-media-releases.xml",
            "https://www.bok.or.kr/eng/main/rss.do?menuNo=400244"
        ),
        "commodities" to listOf(
            // Energy & commodities only
            "https://www.eia.gov/petroleum/supply/weekly/rss/weekly_petroleum_status_report.xml",
            "https://www.iea.org/newsroom/news?format=rss",
            "https://www.opec.org/opec_web/en/21.xml",
            "https://www.cftc.gov/RSS/RSSGP/rssgp.xml"
        ),
        "crypto" to listOf(
            // Placeholder - add crypto-specific RSS feeds if available
            // "https://cointelegraph.com/rss",
            // "https://www.coindesk.com/arc/outboundfeeds/rss/"
        ),
        "macro-trading" to listOf(
            // Only macro data that directly impacts forex/commodities
            "https://www.stlouisfed.org/rss/news_releases.xml",
            "https://www.imf.org/en/News/RSS"
        )
    )
    
    // Keywords to filter relevant trading news
    val TRADING_KEYWORDS = listOf(
        // Forex pairs
        "EUR", "USD", "GBP", "JPY", "CHF", "AUD", "NZD", "CAD",
        "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD", "NZDUSD",
        "forex", "currency", "exchange rate", "fx",
        
        // Commodities
        "gold", "silver", "oil", "crude", "WTI", "Brent", "copper",
        "natural gas", "platinum", "palladium", "commodity", "commodities",
        
        // Crypto
        "bitcoin", "BTC", "ethereum", "ETH", "crypto", "cryptocurrency",
        "blockchain", "altcoin", "DeFi", "NFT",
        
        // Trading-relevant macro
        "interest rate", "inflation", "CPI", "NFP", "GDP", "PMI",
        "employment", "unemployment", "retail sales", "trade balance",
        "central bank", "monetary policy", "Fed", "ECB", "BOE", "BOJ",
        "rate hike", "rate cut", "QE", "quantitative",
        
        // Market terms
        "volatility", "liquidity", "trading", "market", "price",
        "rally", "selloff", "bullish", "bearish", "trend"
    )
    
    // Exclude non-trading topics
    val EXCLUDE_KEYWORDS = listOf(
        "climate", "sustainability", "ESG", "diversity", "inclusion",
        "conference", "seminar", "workshop", "webinar", "event",
        "appointment", "resignation", "hiring", "staff",
        "regulation" // unless it's about trading regulation
    )
}
