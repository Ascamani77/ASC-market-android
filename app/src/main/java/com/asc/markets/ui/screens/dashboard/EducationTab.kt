package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun EducationTab() {
    var openSection by remember { mutableStateOf<String?>("arch") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            InfoBox(minHeight = 160.dp) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Icon(
                        androidx.compose.material.icons.autoMirrored.outlined.School, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.03f),
                        modifier = Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(androidx.compose.material.icons.autoMirrored.outlined.AutoGraph, null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("INTELLIGENCE FRAMEWORK", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Understanding the institutional logic and rule-based engines driving situational awareness.",
                            color = SlateText, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }

        item {
            EducationCollapseItem(
                id = "arch",
                title = "System Architecture & Logic",
                icon = androidx.compose.material.icons.autoMirrored.outlined.Layers,
                isOpen = openSection == "arch",
                onClick = { openSection = if (openSection == "arch") null else "arch" }
            ) {
                 Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                     Text("The core identifies market structure, liquidity sweeps, and supply/demand zones through strict mathematical rules, removing subjective interpretation.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                         Box(modifier = Modifier.weight(1f).background(GhostWhite, RoundedCornerShape(8.dp)).padding(12.dp)) {
                             Column {
                                 Text("TECHNICAL", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                 Text("60% Weight", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                             }
                         }
                         Box(modifier = Modifier.weight(1f).background(GhostWhite, RoundedCornerShape(8.dp)).padding(12.dp)) {
                             Column {
                                 Text("SAFETY", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                 Text("40% Weight", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                             }
                         }
                     }
                 }
             }
        }

        item {
            EducationCollapseItem(
                id = "concepts",
                title = "Market Concepts Library",
                icon = androidx.compose.material.icons.autoMirrored.outlined.MenuBook,
                isOpen = openSection == "concepts",
                onClick = { openSection = if (openSection == "concepts") null else "concepts" }
            ) {
                 Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                     ConceptItem("Market Structure", "Mapping directional flow via HH/HL/LH/LL swing points.")
                     ConceptItem("Liquidity Sweeps", "Institutional interaction with retail stop-loss clusters.")
                     ConceptItem("Supply & Demand", "Areas of high-volume displacement and unfilled orders.")
                 }
             }
        }

        item {
            InfoBox {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("OPPORTUNITY STATE GUIDE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(16.dp))
                    InterpretationRow("WAIT", "No high-quality structural alignment detected.", RoseError)
                    InterpretationRow("OBSERVE", "Pre-conditions are forming; monitoring sweep.", Color(0xFFF59E0B))
                    InterpretationRow("FOCUS", "High-confluence institutional window open.", EmeraldSuccess)
                }
            }
        }
        
        item {
            Surface(
                color = GhostWhite,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(androidx.compose.material.icons.autoMirrored.outlined.Info, null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "The analytical node provides risk framing based on historical alignment. Final decision authority resides with the operator.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun EducationCollapseItem(id: String, title: String, icon: ImageVector, isOpen: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(title.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                // Show FILLED icon for the active/open state, OUTLINED for the inactive state
                Icon(if (isOpen) androidx.compose.material.icons.autoMirrored.filled.ExpandLess else androidx.compose.material.icons.autoMirrored.outlined.ExpandMore, null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = isOpen, enter = expandVertically(), exit = shrinkVertically()) {
                Box(modifier = Modifier.border(androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(0.dp))) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ConceptItem(title: String, desc: String) {
    Column {
        Text(title, color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(desc, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun InterpretationRow(label: String, desc: String, color: Color) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(desc, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}