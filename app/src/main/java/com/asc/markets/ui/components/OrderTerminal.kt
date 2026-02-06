package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun OrderTerminal(symbol: String) {
    var size by remember { mutableStateOf("0.10") }
    var selectedAlgo by remember { mutableStateOf("MARKET") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("MACRO INTELLIGENCE DESK", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("INSTITUTIONAL PIPELINE v4.2", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            
            Box(modifier = Modifier.background(EmeraldSuccess.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).border(1.dp, EmeraldSuccess.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("PROTOCOL_SECURE", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Institutional Algo Selector
        Row(modifier = Modifier.fillMaxWidth().background(GhostWhite, RoundedCornerShape(12.dp)).padding(4.dp)) {
            listOf("MARKET", "VWAP", "TWAP").forEach { algo ->
                val active = selectedAlgo == algo
                Surface(
                    color = if (active) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).clickable { selectedAlgo = algo }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(algo, color = if (active) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExecutionButton("BUY", EmeraldSuccess, Modifier.weight(1f))
            ExecutionButton("SELL", RoseError, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = size,
            onValueChange = { size = it },
            label = { Text("POSITION SIZE (LOTS)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            )
        )
    }
}

@Composable
fun ExecutionButton(label: String, color: Color, modifier: Modifier) {
    Button(
        onClick = { },
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    }
}