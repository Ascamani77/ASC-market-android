package com.researchcenter.util

import com.researchcenter.data.models.NewsCategory

object Constants {
    const val BASE_URL = "https://ais-dev-4tncuadz4nd7q6igo5rb7z-473892111102.europe-west2.run.app"

    // NOTE: Sync this with /sources.json for cross-platform parity
    val CATEGORIES = listOf(
        NewsCategory("all", "All Macro", "🌍"),
        NewsCategory("central-banks", "Central Banks", "🏦"),
        NewsCategory("energy", "Energy & Commodities", "🛢️"),
        NewsCategory("macro", "Macro Data", "📉"),
        NewsCategory("calendar", "Macro Calendar", "📅"),
        NewsCategory("gov-reg", "Gov & Reg", "⚖️")
    )

    val TRENDING_TOPICS = listOf(
        "WTO Global Trade Barometer",
        "South Korea Export Growth (Canary)",
        "IMF World Economic Outlook (WEO)",
        "BCB COPOM Meeting Schedule",
        "RBI MPC Meeting Schedule",
        "ISM US Manufacturing PMI",
        "PBoC LPR Decision Schedule",
        "ECB Governing Council Schedule"
    )

    val RSS_FEEDS = mapOf(
        "calendar" to listOf(
            "https://www.ismworld.org/events/conferences-and-events/upcoming-events/",
            "https://www.forexlive.com/feed",
            "https://www.myfxbook.com/rss/forex-economic-calendar-events",
            "https://www.abs.gov.au/rss/abs_future_releases.xml"
        ),
        "central-banks" to listOf(
            "https://www.bok.or.kr/eng/main/rss.do?menuNo=400244",
            "https://www.bcb.gov.br/api/feed/sitebcb/sitefeeds/notasImprensa?ano=2025",
            "https://www.bcb.gov.br/api/feed/sitebcb/sitefeeds/comunicadoscopom",
            "https://rbi.org.in/pressreleases_rss.xml",
            "https://rbi.org.in/notifications_rss.xml",
            "https://rbi.org.in/speeches_rss.xml",
            "https://www.pbc.gov.cn/goutongjiaoliu/113456/2986536/index.html",
            "https://www.federalreserve.gov/feeds/press_all.xml",
            "https://www.federalreserve.gov/feeds/speeches.xml",
            "https://www.federalreserve.gov/feeds/press_monetary.xml",
            "https://www.ecb.europa.eu/rss/press.html",
            "https://www.ecb.europa.eu/rss/blog.html",
            "https://www.bankofengland.co.uk/rss/news",
            "https://www.bankofengland.co.uk/rss/speeches",
            "https://www.snb.ch/public/en/rss/pressrel",
            "https://www.boj.or.jp/en/rss/whatsnew.xml",
            "https://www.rba.gov.au/rss/rss-cb-media-releases.xml",
            "https://www.bis.org/speeches/index.rss",
            "https://www.bis.org/press/index.rss",
            "https://www.bankofcanada.ca/feed/"
        ),
        "gov-reg" to listOf(
            "https://www.cftc.gov/RSS/RSSGP/rssgp.xml",
            "https://treasurydirect.gov/rss/mspd.xml",
            "https://treasurydirect.gov/TA_WS/securities/announced/rss",
            "https://treasurydirect.gov/TA_WS/securities/auctioned/rss",
            "https://www.sec.gov/news/pressreleases.rss",
            "https://www.fca.org.uk/news/rss.xml",
            "https://www.esma.europa.eu/rss.xml"
        ),
        "macro" to listOf(
            "https://www.reutersagency.com/feed/?best-topics=business&format=xml",
            "https://www.fxstreet.com/rss/news",
            "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=15839069",
            "https://www.wto.org/english/news_e/news_e.rss",
            "https://www.oecd.org/newsroom/rss.xml",
            "https://www.worldbank.org/en/news/all?feed=rss",
            "https://www.imf.org/en/News/RSS",
            "https://www.stlouisfed.org/rss/news_releases.xml",
            "https://ec.europa.eu/eurostat/en/search?p_p_id=estatsearchportlet_WAR_estatsearchportlet&p_p_lifecycle=2&p_p_state=maximized&p_p_mode=view&p_p_resource_id=atom&_estatsearchportlet_WAR_estatsearchportlet_collection=CAT_PREREL",
            "https://ec.europa.eu/eurostat/en/search?p_p_id=estatsearchportlet_WAR_estatsearchportlet&p_p_lifecycle=2&p_p_state=maximized&p_p_mode=view&p_p_resource_id=atom&_estatsearchportlet_WAR_estatsearchportlet_collection=CAT_EURNEW",
            "https://www.abs.gov.au/rss/abs_news_and_events.xml",
            "https://www.stat.go.jp/english/info/news/index.xml",
            "https://www.stat.go.jp/english/info/news/news.xml",
            "https://www.rd.usda.gov/rss.xml",
            "https://www.fao.org/newsroom/rss/en/"
        ),
        "energy" to listOf(
            "https://www.eia.gov/about/new/whatsnew_rss.xml",
            "https://www.eia.gov/petroleum/supply/weekly/rss/weekly_petroleum_status_report.xml",
            "https://www.eia.gov/outlooks/steo/rss/steo.xml",
            "https://www.energy.gov/rss/news.xml",
            "https://www.iea.org/newsroom/news?format=rss",
            "https://www.opec.org/opec_web/en/21.xml",
            "https://www.usda.gov/rss/home.xml"
        )
    )
}
