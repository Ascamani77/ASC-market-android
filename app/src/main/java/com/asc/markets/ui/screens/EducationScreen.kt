package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.asc.markets.ai.AIContextService

@Composable
fun EducationScreen() {
    val aiContext by AIContextService.contextState.collectAsState()
    val platformContext = aiContext.platformContext

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).padding(16.dp)) {
        Text("AI CONTEXT & FRAMEWORK", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("CENTRALIZED KNOWLEDGE REPOSITORY", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("ACCESS PERMISSIONS", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(platformContext.accessPermissions.toList()) { (page, allowed) ->
                PermissionCard(page, allowed)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("DATA SOURCING RULES", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(platformContext.dataSources.toList()) { (assetClass, source) ->
                DataSourceCard(assetClass, source)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("OPERATIONAL DIRECTIVES", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(platformContext.operationalRules) { rule ->
                RuleCard(rule)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("INSTITUTIONAL CONCEPTS", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
            }
            val concepts = listOf(
                "Market Structure" to "Mapping directional flow via swing points (HH/HL/LH/LL).",
                "Liquidity Sweeps" to "Institutional manipulation of retail stop-loss zones.",
                "Supply & Demand" to "Areas of high-volume displacement and unfilled orders."
            )
            items(concepts) { (title, desc) ->
                ConceptCard(title, desc)
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PermissionCard(page: String, allowed: Boolean) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(page, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = if (allowed) EmeraldSuccess.copy(alpha = 0.1f) else RoseError.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    if (allowed) "UNRESTRICTED" else "RESTRICTED",
                    color = if (allowed) EmeraldSuccess else RoseError,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DataSourceCard(assetClass: String, source: String) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(assetClass, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(source, color = IndigoAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun RuleCard(rule: String) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text("•", color = IndigoAccent, modifier = Modifier.padding(end = 8.dp))
            Text(rule, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
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
