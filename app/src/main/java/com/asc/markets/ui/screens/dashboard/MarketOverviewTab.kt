package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MiniChart
import com.asc.markets.ui.components.ForexIcon
import com.asc.markets.ui.theme.*
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import com.asc.markets.BuildConfig

// --- Data model ---
data class NewsItem(
    val headline: String,
    val source: String,
    val timestamp: String,
    val assetType: String,
    val imageUrl: String = ""
)

// --- Networking: fetch from OpenAI (uses BuildConfig.OPENAI_API_KEY) ---
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun fetchNewsFromGemini(): List<NewsItem> {
    return try {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) {
            android.util.Log.e("AscNews", "OPENAI API key missing in BuildConfig")
            return emptyList()
        }

        val prompt = """
            Generate 10 recent financial market news headlines for trading markets.
            Include a mix of asset types: forex, crypto, stocks, indices.
            For each headline include: headline, source, timestamp (e.g. "2h ago"), assetType (one of: "forex","crypto","stocks","indices"), imageUrl (https or empty).
            Return ONLY a valid JSON array (no extra text) like:
            [{"headline":"...","source":"...","timestamp":"...","assetType":"...","imageUrl":"..."}, ...]
        """.trimIndent()

        val bodyJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.7)
            put("max_tokens", 1200)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val url = URL("https://api.openai.com/v1/chat/completions")
        val responseText = withContext(Dispatchers.IO) {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            conn.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

            val code = conn.responseCode
            android.util.Log.d("AscNews", "OpenAI response code: $code")
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                android.util.Log.e("AscNews", "OpenAI HTTP error: $code $err")
                ""
            }
            conn.disconnect()
            text
        }

        if (responseText.isBlank()) {
            android.util.Log.d("AscNews", "Empty responseText from OpenAI")
            return emptyList()
        }

        // Extract assistant content: choices[0].message.content
        val topJson = JSONObject(responseText)
        val choices = topJson.optJSONArray("choices")
        val contentStr = if (choices != null && choices.length() > 0) {
            val msgObj = choices.getJSONObject(0).optJSONObject("message")
            msgObj?.optString("content") ?: ""
        } else {
            ""
        }

        if (contentStr.isBlank()) {
            android.util.Log.d("AscNews", "No content in choices, falling back")
            return emptyList()
        }

        // Try parsing content as a JSON array directly; handle surrounding text if present
        val jsonArray = try {
            JSONArray(contentStr)
        } catch (e: Exception) {
            val start = contentStr.indexOf('[')
            val end = contentStr.lastIndexOf(']')
            if (start != -1 && end != -1 && end > start) {
                JSONArray(contentStr.substring(start, end + 1))
            } else {
                throw e
            }
        }

        val items = mutableListOf<NewsItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val headline = obj.optString("headline", "").trim()
            if (headline.isEmpty()) continue
            val source = obj.optString("source", "Market")
            val timestamp = obj.optString("timestamp", "Just now")
            val assetType = obj.optString("assetType", "forex")
            val imageUrl = obj.optString("imageUrl", "")
            items.add(NewsItem(headline, source, timestamp, assetType, imageUrl))
        }

        android.util.Log.d("AscNews", "Parsed ${items.size} news items from OpenAI")
        if (items.isNotEmpty()) items else emptyList()
    } catch (e: Exception) {
        android.util.Log.e("AscNews", "Exception in fetchNewsFromGemini: ${e.message}", e)
        emptyList()
    }
}

// --- Fallback/mock data ---
fun getMockAscNews(): List<NewsItem> = listOf(
    NewsItem("Federal Reserve maintains interest rates amid inflation concerns", "Reuters", "2h ago", "forex"),
    NewsItem("Bitcoin surges 5% as institutional investors return to market", "Bloomberg", "3h ago", "crypto"),
    NewsItem("Tech stocks rally following better-than-expected earnings reports", "CNBC", "4h ago", "stocks"),
    NewsItem("S&P 500 reaches all-time high on strong Q4 performance", "MarketWatch", "1h ago", "indices"),
    NewsItem("Euro strengthens against dollar on ECB hawkish signals", "FT", "2h ago", "forex"),
    NewsItem("Ethereum breaks $3000 as layer-2 scaling solutions gain adoption", "CoinDesk", "1h ago", "crypto"),
    NewsItem("Apple announces record revenue driven by iPhone 16 sales", "WSJ", "3h ago", "stocks"),
    NewsItem("FTSE 100 climbs as energy stocks benefit from oil price surge", "LSE News", "5h ago", "indices"),
    NewsItem("Gold prices stabilize above $2100 per ounce on geopolitical tensions", "Reuters", "2h ago", "forex"),
    NewsItem("US unemployment rate drops to 3.5% signaling robust economic health", "Bureau of Labor", "4h ago", "indices")
)

// --- UI ---
@Composable
fun MarketOverviewTab(selectedPair: ForexPair, onAssetClick: (ForexPair) -> Unit = {}) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val categories = listOf("Overview", "Stocks", "Crypto", "Futures", "Forex", "Bonds")
    var selectedCat by remember { mutableStateOf("Overview") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        contentPadding = PaddingValues(bottom = 158.dp)
    ) {
        // Top header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Institutional Explore", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // Category chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCat == cat
                    Surface(
                        color = if (isSelected) Color(0xFF2d2d2d) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                        modifier = Modifier.clickable {
                            vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                            selectedCat = cat
                        }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF94a3b8),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Market overview summary (kept concise)
        item {
            InfoBox(minHeight = 180.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Market Overview", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Global Sentiment", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Bullish", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volatility Index", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("18.5 (Low)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trading Volume", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("High", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top Gainer", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("BTC +8.2%", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top Loser", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("EUR/USD -1.1%", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Market Cap", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("2.56 Trillion", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    // Explanatory points
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "• Global Sentiment reflects aggregate market bias: Bullish indicates net long positioning across major indices and crypto.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Volatility Index (VIX-equivalent) measures expected price swing range: Low = calm markets, High = institutional hedging active.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Trading Volume surge indicates breakout potential: High volume on bullish days confirms trend strength and liquidity.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Top Gainer/Loser tracks momentum leaders: Use as sentiment gauge for sector rotation and relative strength analysis.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Market Cap concentration shows where institutional capital flows: Crypto > $2.5T signals strong institutional adoption.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Real-time updates: All metrics refresh every 60 seconds to capture micro-trends before they cascade across asset classes.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // INSTITUTIONAL NODES SECTION
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // INSTITUTIONAL NODES SECTION
        
        // 1. ADAPTIVE SESSION PROGRESS ENGINE (Clock Node)
        item {
            InfoBox(minHeight = 140.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🕐", fontSize = 18.sp)
                            Text("Session Progress", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("LONDON OPEN", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    
                    // Circular gauge representation
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Time (UTC)", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("08:42:15", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Text("64%", color = IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Text("London 08:00–16:00 UTC | New York 13:00–21:00 UTC | Tokyo 00:00–08:00 UTC", color = SlateText, fontSize = 10.sp, lineHeight = 12.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 2. OPERATIONAL VITALS GRID (KPI)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Operational Vitals", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Spread
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Spread", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("0.18 Pips", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("Below avg", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Volatility
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Volatility", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("12.3 Pips/Hr", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("20-day avg", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Safety Gate
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🛡️", fontSize = 14.sp)
                                Text("Safety Gate", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("ARMED", color = EmeraldSuccess, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("+47 min safe", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Node Latency
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Node Latency", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("8.2 ms", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("Optimal ping", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 3. NET EXPOSURE HUB
        item {
            InfoBox(minHeight = 120.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💰", fontSize = 18.sp)
                            Text("Net Exposure", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(color = IndigoAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text("BALANCED", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    
                    Text("Net USD Direction", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(0.4f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.6f).background(RoseError, RoundedCornerShape(4.dp)))
                        }
                        Box(modifier = Modifier.weight(0.6f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                        }
                    }
                    Text("Short 2.3M USD | Long 2.1M USD | Net: 0.2M Short", color = Color.White, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 4. MACRO REGIME CLASSIFIER
        item {
            InfoBox(minHeight = 120.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌍", fontSize = 18.sp)
                        Text("Macro Regime", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("VIX Index:", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Text("16.8", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("RISK_ON", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("DXY 24h:", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Text("+0.34%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("USD_DOMINANT", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Text("Environment: Equities & Growth Assets Favored | USD Strength Persists", color = Color.White, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 5. INSTITUTIONAL TAPE (Event Log)
        item {
            InfoBox(minHeight = 150.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📊", fontSize = 18.sp)
                        Text("Institutional Tape", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Event 1
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("08:41", color = SlateText, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("RSI enters Overbought (>70) on M5", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        // Event 2
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("08:38", color = SlateText, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("Volume spike 2.3x 10-period MA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        // Event 3
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("08:35", color = SlateText, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("Price touches R1 level 1.0892", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        // Event 4
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("08:32", color = SlateText, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("Higher High / Higher Low confirmed", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 6. STRUCTURAL CONTEXT CARD (Fractal Logic)
        item {
            InfoBox(minHeight = 110.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 18.sp)
                        Text("Structural Context", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    Text("Last 4 Swings: Higher High & Higher Low sequence detected (Bullish Structure). Price closed above swing high on M15 → BOS (Break of Structure) confirmed.", color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 7. INSTITUTIONAL LEVELS GRID (S/R, Daily High/Low)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Institutional Levels", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Support", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0845", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("SL: 1.0835", color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Resistance", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0915", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("TP: 1.0925", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daily High", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0928", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("+0.8% from open", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daily Low", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0812", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("-0.65% from open", color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Forex Majors title
        item {
            Text(
                "Forex Majors",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 3. EXPLORE GRID (2-Column Matrix)
        val gridItems = FOREX_PAIRS.take(6).chunked(2)
        items(gridItems) { row ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { pair ->
                    ExploreMiniCard(
                        pair = pair,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                            onAssetClick(pair)
                        }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        // 4. NEWS FLOW HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("News Flow >", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }

        // 5. NEWS FLOW ITEMS
        val newsItems = listOf(
            Triple("Reuters • 10:06 pm", "Two things OPEC+ can't control: Trump and China imports", "B"),
            Triple("Dow Jones • 10:00 pm", "Week Ahead for FX, Bonds: U.S. Jobs Data, Central Bank Decisions in Focus", "W"),
            Triple("Reuters • 09:45 pm", "Island Pharmaceuticals Seeks Trading Halt", "I"),
            Triple("Bloomberg • 09:30 pm", "Bitcoin's Price Sinks Further: High Volatility Expected", "B")
        )

        items(newsItems) { (meta, title, iconChar) ->
            NewsFlowRow(meta, title, iconChar)
        }

        // MARKET FLOW (NEWS STREAM) WITH VISUALIZATION
        item {
            InfoBox(minHeight = 160.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header with title and status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Newspaper, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Text("Market Flow", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Impact headline metric
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("High-Impact Headlines", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("4 critical news events in last 2 hours", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    
                    // Source distribution progress bars
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Reuters
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reuters", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                            Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.65f).background(RoseError, RoundedCornerShape(2.dp)))
                            }
                            Text("65%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Bloomberg
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bloomberg", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                            Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(IndigoAccent, RoundedCornerShape(2.dp)))
                            }
                            Text("35%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // LP ROUTING FLOW WITH VENUE LATENCY
        item {
            InfoBox(minHeight = 180.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header with title and icon
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                        Text("LP Routing Flow", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    // Venue liquidity distribution
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // JPM-NODE
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("JPM-NODE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.78f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.01MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // LMAX-UK
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LMAX-UK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.56f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.02MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                            
                        // CITADEL
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("CITADEL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.45f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.01MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // BARC-L7
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("BARC-L7", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.32f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.04MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // VOLATILITY PULSE
        item {
            InfoBox(minHeight = 140.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header with title and status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("↑", color = IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Volatility Pulse", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "STABLE",
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Standard Deviation metric
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Standard Deviation", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.683f).background(Color(0xFFFFA500), RoundedCornerShape(4.dp)))
                            }
                            Text("68.3%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    // Market context insight
                    Text(
                        "Market compression detected. Expansion phase expected within the next 45 minutes of NY session.",
                        color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium
                    )
                    
                    // Large metric display
                    Text(
                        "1.4",
                        color = IndigoAccent,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // ASC NEWS SECTION
        item {
            AscNewsSection()
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 6. MARKET DEPTH LADDER
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 16.dp)) {
                MarketDepthLadder(selectedPair = selectedPair)
            }
        }

        // NEW SECTIONS: Crypto, Stocks, Indices Cards
        item { CryptoCardsSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { StockCardsSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { IndicesCardsSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { MajorIndicesSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        // Crypto market cap removed per request
    }
}

@Composable
private fun ExploreMiniCard(pair: ForexPair, modifier: Modifier, onClick: () -> Unit) {
    val isUp = pair.change >= 0
    val color = if (isUp) EmeraldSuccess else RoseError

    InfoBox(
        modifier = modifier.clickable { onClick() },
        height = 130.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Content Row
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Show flags for forex pairs, badge for others
                    if (pair.symbol.contains("/")) {
                        ForexIcon(pair.symbol, size = 20)
                    } else {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
                            Text(pair.symbol.take(1), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Column {
                        Text(pair.symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(pair.name.take(8).uppercase() + "...", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("D —", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    text = String.format(Locale.US, "%.2f", pair.price) + if (pair.symbol.contains("/")) "" else " INR",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f", pair.changePercent)}% today",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Edge-to-Edge Sparkline
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))) {
                MiniChart(
                    values = List(15) { pair.price + kotlin.random.Random.nextDouble(-pair.price*0.01, pair.price*0.01) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NewsFlowRow(meta: String, title: String, iconChar: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                Text(iconChar, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Text(meta, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            fontFamily = InterFontFamily
        )
    }
}

// ============= NEW SECTIONS: Crypto, Stocks, Indices Cards =============

@Composable
fun CryptoCardsSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Crypto Gainers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(0.dp)) {
            items(3) { idx ->
                val cryptos = listOf(
                    Triple("Bitcoin", "BTC", "76,762"),
                    Triple("Ethereum", "ETH", "2,300.9"),
                    Triple("zkSync", "ZK", "0.028705")
                )
                val (name, symbol, price) = cryptos[idx % cryptos.size]
                CryptoCard(name, symbol, price) {
                    val p = price.replace(",", "").toDoubleOrNull() ?: 0.0
                    onAssetClick(ForexPair(symbol, name, p, 0.0, 0.0))
                }
            }
        }
    }
}

@Composable
private fun CryptoCard(name: String, symbol: String, price: String, onClick: () -> Unit = {}) {
    InfoBox(modifier = Modifier.width(160.dp).clickable { onClick() }, height = 200.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Crypto badge with emoji icon
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                    Text("₿", color = Color(0xFFF7931A), fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Column {
                    Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(symbol, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(price + " USD", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("+20.59% today", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(6.dp))) {
                MiniChart(
                    values = List(20) { 100.0 + kotlin.random.Random.nextDouble(-5.0, 5.0) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun StockCardsSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Stocks Gainers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val stocks = listOf(
            Pair("United Foods", "217.44 INR"),
            Pair("Creative Dynamics", "615.65 INR"),
            Pair("Gas Holdings", "1.8308 USD")
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(0.dp)) {
            items(stocks.size) { idx ->
                val (name, price) = stocks[idx]
                StockCard(name, price) {
                    val p = price.replace(" INR", "").replace(" USD", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    onAssetClick(ForexPair(name, name, p, 0.0, 0.0))
                }
            }
        }
    }
}

@Composable
private fun StockCard(name: String, price: String, onClick: () -> Unit = {}) {
    InfoBox(modifier = Modifier.width(160.dp).clickable { onClick() }, height = 200.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Stock badge
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                    Text("📈", fontSize = 16.sp)
                }
                Text(name.take(6), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(price, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("+20.0% today", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(6.dp))) {
                MiniChart(
                    values = List(20) { 217.0 + kotlin.random.Random.nextDouble(-10.0, 10.0) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun IndicesCardsSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Major Indices", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val indices = listOf(
            Triple("S&P 500", "SPX", "6,939.02"),
            Triple("Nasdaq 100", "NDX", "25,552.39"),
            Triple("DAX", "DAX", "24,538.81")
        )
        
        indices.forEach { (name, symbol, price) ->
            IndexCard(name, symbol, price) {
                val p = price.replace(",", "").replace(" USD", "").replace(" JPY", "").toDoubleOrNull() ?: 0.0
                onAssetClick(ForexPair(symbol, name, p, 0.0, 0.0))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun IndexCard(name: String, symbol: String, price: String, onClick: () -> Unit = {}) {
    InfoBox(modifier = Modifier.fillMaxWidth().clickable { onClick() }, height = 150.dp) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Index badge
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                        Text(symbol.take(3), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text(price, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("-0.43%", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
                        if (symbol == "SPX" || name.contains("S&P")) {
                                AndroidView(
                                        factory = { ctx ->
                                                WebView(ctx).apply {
                                                        settings.javaScriptEnabled = true
                                                        settings.domStorageEnabled = true
                                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                        val html = """
                                                                <!doctype html>
                                                                <html>
                                                                <head>
                                                                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
                                                                    <script type=\"text/javascript\" src=\"https://s3.tradingview.com/tv.js\"></script>
                                                                </head>
                                                                <body style=\"margin:0;background-color:#0b0b0b;\">
                                                                    <div id=\"tv_chart_container\"></div>
                                                                    <script type=\"text/javascript\">(function() {
                                                                        new TradingView.widget({
                                                                            \"autosize\": true,
                                                                            \"symbol\": \"INDEX:SPX\",
                                                                            \"interval\": \"D\",
                                                                            \"timezone\": \"Etc/UTC\",
                                                                            \"theme\": \"Dark\",
                                                                            \"style\": \"1\",
                                                                            \"locale\": \"en\",
                                                                            \"container_id\": \"tv_chart_container\",
                                                                            \"hide_legend\": true,
                                                                            \"allow_symbol_change\": false,
                                                                            \"hide_side_toolbar\": true,
                                                                            \"withdateranges\": false,
                                                                            \"details\": false,
                                                                            \"toolbar_bg\": \"#0b0b0b\",
                                                                            \"width\": \"100%\",
                                                                            \"height\": 120
                                                                        });
                                                                    })();</script>
                                                                </body>
                                                                </html>
                                                        """.trimIndent()
                                                        loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                                                }
                                        },
                                        modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(8.dp))
                                )
                        } else {
                                Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(8.dp))) {
                                        MiniChart(
                                                values = List(25) { 6900.0 + kotlin.random.Random.nextDouble(-50.0, 50.0) },
                                                modifier = Modifier.fillMaxSize()
                                        )
                                }
                        }
        }
    }
}

@Composable
fun MajorIndicesSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("All Major Markets", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        
        val majorMarkets = listOf(
            // STOCK INDICES
            MajorIndex("S&P 500", "SPX", "6,939.02 USD", "+0.87%", EmeraldSuccess),
            MajorIndex("Nasdaq 100", "NDX", "25,552.39 USD", "+1.42%", EmeraldSuccess),
            MajorIndex("DAX", "DAX", "24,538.81 EUR", "+0.94%", EmeraldSuccess),
            MajorIndex("FTSE 100", "UKX", "10,223.54 GBP", "+0.51%", EmeraldSuccess),
            
            // FOREX PAIRS
            MajorIndex("EUR/USD", "EURUSD", "1.0852", "+0.23%", EmeraldSuccess),
            MajorIndex("GBP/USD", "GBPUSD", "1.2734", "-0.15%", RoseError),
            MajorIndex("USD/JPY", "USDJPY", "149.85", "+0.42%", EmeraldSuccess),
            
            // ASIAN INDICES
            MajorIndex("Japan 225", "NI225", "53,322.80 JPY", "+2.10%", EmeraldSuccess),
            MajorIndex("SSE Composite", "000001", "4,117.9476 CNY", "-0.96%", RoseError),
            
            // COMMODITIES
            MajorIndex("Gold (Spot)", "XAUUSD", "2,087.50 USD", "+1.85%", EmeraldSuccess),
            MajorIndex("Crude Oil WTI", "WTICRUDEOJ", "76.45 USD", "-1.32%", RoseError),
            MajorIndex("Natural Gas", "NGAS", "2.856 USD", "+3.21%", EmeraldSuccess)
        )
        
        majorMarkets.forEach { market ->
            MajorIndexRow(market) {
                val p = market.value.replace(",", "").replace(" USD", "").replace(" JPY", "").replace(" GBP", "").toDoubleOrNull() ?: 0.0
                onAssetClick(ForexPair(market.code, market.name, p, 0.0, 0.0))
            }
        }
        
        Text(
            "See all markets >",
            color = IndigoAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

data class MajorIndex(val name: String, val code: String, val value: String, val change: String, val changeColor: Color)

@Composable
private fun MajorIndexRow(index: MajorIndex, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            // Show flags for forex pairs, badges for others
            if (index.code.contains("/")) {
                ForexIcon(index.code, size = 40)
            } else {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(IndigoAccent), contentAlignment = Alignment.Center) {
                    Text(index.code.take(3), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            Column {
                Text(index.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(index.code, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(index.value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(index.change, color = index.changeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Crypto market cap section removed per user request

@Composable
fun AscNewsSection() {
    var newsList by remember { mutableStateOf<List<NewsItem>>(getMockAscNews()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("AscNews", "Starting to fetch news from OpenAI...")
            val fetchedNews = fetchNewsFromGemini()
            android.util.Log.d("AscNews", "Fetched ${fetchedNews.size} news items")
            if (fetchedNews.isNotEmpty()) {
                newsList = fetchedNews
                android.util.Log.d("AscNews", "Updated newsList with ${fetchedNews.size} items")
            }
        } catch (e: Exception) {
            android.util.Log.e("AscNews", "Error fetching news: ${e.message}", e)
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Header
        Text("ASC News >", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }

    // Display news items
    newsList.take(10).forEach { newsItem ->
        AscNewsItemRow(newsItem)
    }
}

@Composable
fun AscNewsItemRow(newsItem: NewsItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Asset type icon
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                Text(
                    when (newsItem.assetType) {
                        "forex" -> "🔄"
                        "crypto" -> "₿"
                        "stocks" -> "📈"
                        "indices" -> "📊"
                        else -> "📰"
                    },
                    fontSize = 11.sp
                )
            }
            Text("${newsItem.source} • ${newsItem.timestamp}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = newsItem.headline,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            fontFamily = InterFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
