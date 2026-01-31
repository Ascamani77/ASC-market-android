package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun AlertsScreen(viewModel: ForexViewModel) {
    var isSmartMode by remember { mutableStateOf(true) }
    val selectedConfirmations = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()
    
    val logicScore = remember(selectedConfirmations.size) {
        40 + (selectedConfirmations.size * 15).coerceAtMost(60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("GUARD SURVEILLANCE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("CONTINUOUS NODE SCANNING", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Mode Toggle Parity
        Row(modifier = Modifier.fillMaxWidth().background(PureBlack, RoundedCornerShape(12.dp)).padding(4.dp)) {
            listOf(true to "SMART ALERT", false to "SIMPLE").forEach { (mode, label) ->
                val active = isSmartMode == mode
                Surface(
                    color = if (active) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp).clickable { isSmartMode = mode }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (active) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSmartMode) {
            SmartCalibration(logicScore, selectedConfirmations)
        } else {
            SimpleCalibration()
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Parity: Rejected Patterns Log
        Text("REJECTED PATTERNS LOG", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RejectedPatternCard("GBP/USD", "RESISTANCE_BREAK", "Price touched level but closed back inside.")
            RejectedPatternCard("XAU/USD", "LIQUIDITY_SWEEP", "Insufficient volume expansion on fractal break.")
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun SmartCalibration(score: Int, selections: MutableList<String>) {
    val items = listOf("CANDLE_CLOSE_BEYOND", "HTF_BIAS_ALIGNMENT", "VOLATILITY_EXPANSION", "RSI_EXIT_DIVERGENCE")
    
    InfoBox {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("PROBABILITY LOGIC", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("$score%", color = if (score > 75) EmeraldSuccess else Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Box(modifier = Modifier.background(if (score > 75) EmeraldSuccess.copy(alpha = 0.1f) else GhostWhite, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(if (score > 75) "STRONG ALERT" else "EARLY STRUCTURE", color = if (score > 75) EmeraldSuccess else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            items.forEach { item ->
                val active = selections.contains(item)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(48.dp).clickable { if (active) selections.remove(item) else selections.add(item) },
                    color = if (active) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (active) HairlineHighlight else HairlineBorder)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(16.dp).background(if (active) IndigoAccent else Color.Transparent, RoundedCornerShape(4.dp)).border(1.dp, if (active) IndigoAccent else Color.DarkGray, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(item, color = if (active) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("DEPLOY SURVEILLANCE NODE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
fun SimpleCalibration() {
    InfoBox(height = 120.dp) {
        Text("THRESHOLD_EVALUATOR_L14_ACTIVE", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
fun RejectedPatternCard(pair: String, pattern: String, reason: String) {
    Surface(
        color = RoseError.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pair, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text(pattern, color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(reason, color = SlateText, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = InterFontFamily)
        }
    }
}