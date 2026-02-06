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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun EducationTab() {
    var openSection by remember { mutableStateOf<String?>(null) }

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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "This framework explains why the system identifies conditions, not how to execute trades.",
                            color = SlateText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily
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
                    // Rule-based Engines
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("RULE-BASED ENGINES", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("The core identifies market structure, liquidity sweeps, and supply/demand zones through strict mathematical rules, removing all subjective interpretation.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    // Safety Gate
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SAFETY GATE", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("An automated news engine that blocks all signals during high-impact structural disruptions to prevent exposure to extreme volatility.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    // Confidence Score Composition
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CONFIDENCE SCORE COMPOSITION", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Scores are derived strictly from a 60% Technical Confluence and 40% Fundamental Safety weighting.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("60%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("TECHNICAL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("40%", color = EmeraldSuccess, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SAFETY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
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
                     DetailedConcept(
                         "Market Structure",
                         what = "The mapping of directional flow using swing points such as higher highs and higher lows.",
                         institutionalContext = "Institutions use structure to identify the path of least resistance for large-scale buy and sell programs.",
                         systemDetection = "The structural engine monitors fractal highs and lows over 50-candle lookback windows to define bias."
                     )

                     DetailedConcept(
                         "Liquidity & Stop Hunts",
                         what = "Concentrations of pending orders placed above or below equal highs and lows.",
                         institutionalContext = "Large players require deep liquidity to fill massive orders; they often push price into 'stop' zones to generate necessary counter-liquidity.",
                         systemDetection = "The liquidity engine scans for equal highs and lows and detects wick-based 'sweeps' in real-time."
                     )

                     DetailedConcept(
                         "Supply & Demand",
                         what = "Price areas characterized by aggressive displacement, leaving unfilled institutional orders.",
                         institutionalContext = "These zones act as magnets and decision points for future institutional re-accumulation phases.",
                         systemDetection = "The zone engine identifies 'ERC' (Extended Range Candles) and maps the base of displacement as a zone of interest."
                     )

                     DetailedConcept(
                         "Volatility Expansion",
                         what = "The shift from low-volatility compression (accumulation) to high-volatility expansion (distribution).",
                         institutionalContext = "Expansion typically indicates the participation of major capital players entering the market.",
                         systemDetection = "Calculates the Efficiency Ratio (ER) of price moves compared to the total range covered."
                     )
                 }
             }
        }

        item {
            InfoBox {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("OPPORTUNITY STATE GUIDE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(16.dp))

                    // WAIT card
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                Box(modifier = Modifier.background(RoseError.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("WAIT", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text("No valid high-quality structural alignment detected. Professional discipline is currently in focus.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OBSERVE card
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                Box(modifier = Modifier.background(Color(0xFFF59E0B).copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("OBSERVE", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text("Pre-conditions are forming. The system is monitoring for a final liquidity sweep or displacement.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // FOCUS card
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                Box(modifier = Modifier.background(EmeraldSuccess.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("FOCUS", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text("A high-confluence window is open. Mathematical probability is at its highest state for the current session.", color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BIAS ≠ DECISION block (black background with icon)
                    InfoBox(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(androidx.compose.material.icons.autoMirrored.outlined.Info, null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("BIAS ≠ DECISION", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("A Bullish or Bearish bias identifies the dominant institutional flow. It is a contextual anchor, not a direction to immediately execute. High-timeframe alignment is required for quality confirmation.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
        
        
        
        // Additional info boxes: Risk Protocol, Quality Metrics, Myth Buster
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoBox {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("RISK PROTOCOL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("NEWS VOLATILITY LOGIC", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("The system blocks automated dispatches during High-Impact news because price action in these windows is often driven by algorithm-rebalancing rather than logical technical structure.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("THE WAITING EDGE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Professional analysts view waiting as a decision of equal importance to executing. If the Safety Gate is active, no technical pattern is valid.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                InfoBox {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.autoMirrored.outlined.ShowChart, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("QUALITY METRICS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("BIAS ACCURACY", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Correlation between bias and price direction.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("WAIT EFFECTIVENESS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Noise avoidance in blocked zones.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("SAFETY SUCCESS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Preservation of logic during news events.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("\"ALIGNMENT INDICATORS MEASURE SYSTEM INTEGRITY, NOT FINANCIAL OUTCOME.\"", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                        }
                    }
                }

                InfoBox {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("MYTH BUSTER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Bullet points
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(6.dp).background(IndigoAccent, RoundedCornerShape(3.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("High confidence (90%+) does not guarantee an outcome; it merely indicates high alignment with historical institutional patterns.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(6.dp).background(IndigoAccent, RoundedCornerShape(3.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A 'Wait' state is not a missed opportunity; it is an active protection of capital from directionless market phases.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(6.dp).background(IndigoAccent, RoundedCornerShape(3.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("The AI advisor provides risk framing and market context, but it does not replace human final decision-making.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                    // Moved: Analytical node risk framing (below Myth Buster)
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
private fun DetailedConcept(title: String, what: String, institutionalContext: String, systemDetection: String) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
        Text(title.uppercase(), color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Text("WHAT IT IS", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(what, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Text("INSTITUTIONAL CONTEXT", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(institutionalContext, color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Text("SYSTEM DETECTION", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(systemDetection, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 18.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp))
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