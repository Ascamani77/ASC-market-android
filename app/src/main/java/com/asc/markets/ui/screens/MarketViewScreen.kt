package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.asc.markets.ui.components.ChartWebView
import com.asc.markets.ui.theme.PureBlack

@Composable
fun MarketViewScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        // Full screen chart view with no footer text
        ChartWebView(
            modifier = Modifier.fillMaxSize(),
            url = "https://appassets.androidplatform.net/assets/www/index.html"
        )
    }
}
