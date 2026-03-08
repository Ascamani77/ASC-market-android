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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Branded Logo and Name matching the reference image
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth()
            ) {
                // The Logo: A clean geometric chevron pointing up
                Text(
                    "Λ", 
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = buildAnnotatedString {
                        // ASC: Large, Bold, White
                        withStyle(style = SpanStyle(
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Black, 
                            color = Color.White
                        )) {
                            append("ASC ")
                        }
                        // MARKET: Smaller, medium weight, bluish-slate color
                        withStyle(style = SpanStyle(
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Medium, 
                            color = Color(0xFF94A3B8)
                        )) {
                            append("MARKET")
                        }
                    },
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = title.replace("_", " "),
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
            }
        }
    }
}