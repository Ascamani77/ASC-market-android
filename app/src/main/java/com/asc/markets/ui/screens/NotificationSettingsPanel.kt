package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.UserSettings
import com.asc.markets.ui.theme.PureBlack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsPanel(userSettings: UserSettings, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Notifications") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }, colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = PureBlack))

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Push Notifications", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Enabled: ")
                    Text(if (userSettings.notificationsEnabled) "Yes" else "No")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Sleep Mode: ${userSettings.sleepStartHour}:00 - ${userSettings.sleepEndHour}:00")
                Spacer(modifier = Modifier.height(12.dp))
                Text("News Impact Filters: ${if (userSettings.newsImpactFilter) "On" else "Off"}")
            }
        }
    }
}