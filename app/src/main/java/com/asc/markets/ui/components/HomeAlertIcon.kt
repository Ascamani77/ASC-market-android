package com.asc.markets.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import com.asc.markets.ui.theme.PureBlack

@Composable
fun HomeAlertIcon(count: Int = 3, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
        }
        if (count > 0) {
            Surface(
                color = Color(0xFFE11D48),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp),
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                    Text(count.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
