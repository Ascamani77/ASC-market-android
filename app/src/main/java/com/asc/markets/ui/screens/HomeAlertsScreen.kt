package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*

@Composable
fun HomeAlertsScreen(viewModel: ForexViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Home Alerts (blank)", color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Placeholder screen for home alert icon debugging.", color = SlateText, fontSize = 12.sp)
        }
    }
}
