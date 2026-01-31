package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.ConfidenceGauge
import com.asc.markets.ui.theme.*

@Composable
fun AnalysisResultsScreen() {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("DEEP AUDIT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text("LOCAL NODE DIRECTIVE L14-UK", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f).height(180.dp),
                color = PureBlack,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ConfidenceGauge(88)
                }
            }
            
            Column(modifier = Modifier.weight(1f).height(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfluenceItem("STRUCTURE", true)
                ConfluenceItem("LIQUIDITY", true)
                ConfluenceItem("NEWS_GUARD", true)
                ConfluenceItem("VOLUME_DELTA", false)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = PureBlack,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("DETERMINISTIC LOGIC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                    Icon(Icons.Default.Info, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Text(
                        "Internal engine: Price action has successfully mitigated the M15 discount zone. Institutional buy program detected via local volume profile. Structural alignment confirmed. Safety Gate: CLEAR.",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = PureBlack,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("SCORING CONFLUENCE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(20.dp))
                FactorRow("TECHNICAL FACT ENGINE", 88)
                Spacer(modifier = Modifier.height(16.dp))
                FactorRow("LOCAL SAFETY GATE", 100)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = PureBlack,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Message, null, tint = SlateText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("NODE CONTEXT AUDIT:", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.weight(1f))
                Text("STRONG BULLISH", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(GhostWhite, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun FactorRow(label: String, percentage: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text("$percentage%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp))) {
            Box(modifier = Modifier.fillMaxWidth(percentage/100f).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
        }
    }
}

@Composable
private fun ConfluenceItem(label: String, checked: Boolean) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(if (checked) EmeraldSuccess else Color.DarkGray, androidx.compose.foundation.shape.CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, color = if (checked) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
    }
}