package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun EducationScreen() {
    val concepts = listOf(
        "Market Structure" to "Mapping directional flow via swing points (HH/HL/LH/LL).",
        "Liquidity Sweeps" to "Institutional manipulation of retail stop-loss zones.",
        "Supply & Demand" to "Areas of high-volume displacement and unfilled orders."
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("FRAMEWORK", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("INSTITUTIONAL CONCEPTS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(concepts) { (title, desc) ->
                ConceptCard(title, desc)
            }
        }
    }
}

@Composable
fun ConceptCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack, RoundedCornerShape(12.dp))
            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(title, color = IndigoAccent, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
    }
}