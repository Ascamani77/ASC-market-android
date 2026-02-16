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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun HomeAlertIcon(count: Int = 3, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(modifier = modifier.size(44.dp), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        if (count > 0) {
            Surface(
                color = Color(0xFFE11D48),
                shape = CircleShape,
                modifier = Modifier.size(16.dp).align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp),
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = count.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}
