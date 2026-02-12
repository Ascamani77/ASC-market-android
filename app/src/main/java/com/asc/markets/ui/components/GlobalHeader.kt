package com.asc.markets.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.asc.markets.data.AppView
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.theme.*

@Composable
fun GlobalHeader(
    currentView: AppView,
    selectedPair: ForexPair,
    onOpenDrawer: () -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    Log.d("ASC", "GlobalHeader: menu icon clicked")
                    onOpenDrawer()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SlateText)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Dashboard writeup and top LIVE indicator removed per spec.
            // Leave center area empty to avoid duplicate dashboard title; LIVE shows in sidebar footer.
            Box(modifier = Modifier.weight(1f)) { }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = SlateText)
                }

                // Use dedicated HomeAlertIcon component; clicking opens the Home Alerts page.
                HomeAlertIcon(count = 3, onClick = onNotifications)
            }
        }
    }
}

@Composable
fun HeaderWaveText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    Row(horizontalArrangement = Arrangement.Center) {
        text.forEachIndexed { index, char ->
            val animatedOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing, delayMillis = index * 80),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset"
            )
            val animatedColor by infiniteTransition.animateColor(
                initialValue = Color(0xFF4B5563),
                targetValue = Color.White,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing, delayMillis = index * 80),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "color"
            )
            Text(
                text = char.toString(),
                color = animatedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.offset(y = animatedOffset.dp)
            )
        }
    }
}