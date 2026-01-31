package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.PureBlack

@Composable
fun NavHeader(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = title.replace("_", " "),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 14.4.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            IconButton(onClick = onSearch, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
            }
        }
    }
}