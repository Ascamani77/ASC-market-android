package com.trading.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge support for accurate safe area awareness
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            TradingApp()
        }
    }
}
