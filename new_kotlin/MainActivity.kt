package com.intelligence.dashboard

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.intelligence.dashboard.ui.IntelligenceDashboardScreen
import com.intelligence.dashboard.ui.theme.*
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AscApp()
        }
    }
}

@Composable
fun AscApp() {
    var viewMode by remember { mutableStateOf("native") } // "native" or "webview"
    val context = LocalContext.current
    
    // Institutional Colors (Matching IntelligenceScreen.kt)
    val TvBlack = ColorBlack
    val TvBorder = ColorZinc800
    val TvAccent = ColorIndigo600
    val TvZinc700 = ColorZinc700
    val TvZinc200 = ColorZinc200

    // Javascript Interface for WebView to Native communication
    val webAppInterface = remember {
        object {
            @JavascriptInterface
            fun sendDataToNative(json: String) {
                try {
                    val data = JSONObject(json)
                    val status = data.optString("status", "NOMINAL")
                    println("Received from JS: $status")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            @JavascriptInterface
            fun showToast(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TvBlack) {
        Column {
            // Top Navigation / Mode Switcher
            Surface(
                color = TvBlack,
                modifier = Modifier.fillMaxWidth().border(1.dp, TvBorder).padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ASC INTELLIGENCE v3.2",
                        color = TvZinc200,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeButton(
                            label = "NATIVE",
                            isSelected = viewMode == "native",
                            onClick = { viewMode = "native" }
                        )
                        ModeButton(
                            label = "WEBVIEW",
                            isSelected = viewMode == "webview",
                            onClick = { viewMode = "webview" }
                        )
                    }
                }
            }

            if (viewMode == "native") {
                IntelligenceDashboardScreen()
            } else {
                WebViewScreen(
                    url = "https://ais-dev-tkwrivsdwrm6fjfbr2g3ta-466295561767.europe-west2.run.app",
                    webAppInterface = webAppInterface
                )
            }
        }
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) ColorIndigo600 else ColorBlack,
        border = BorderStroke(1.dp, if (isSelected) ColorIndigo600 else ColorZinc800),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            color = if (isSelected) Color.White else ColorZinc700,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun WebViewScreen(url: String, webAppInterface: Any) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                
                webViewClient = WebViewClient()
                
                // Add the Javascript Interface
                addJavascriptInterface(webAppInterface, "Android")
                
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
