package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.*
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.AppBottomNavHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsModal(
    event: IntelligenceEvent,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363A45))
            )
        },
        windowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxHeight(0.93f)
            .padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "EVENT INTELLIGENCE — ${event.asset_class.name.uppercase()}",
                        color = SlateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI NARRATIVE Section
            DetailSectionHeader("AI NARRATIVE")
            Surface(
                color = PureBlack,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = event.narrative_summary,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // STRATEGY CONTEXT Section
            event.strategy_context?.let { strategy ->
                DetailSectionHeader("STRATEGY CONTEXT")
                Surface(
                    color = PureBlack,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoItem("BIAS", strategy.bias.uppercase(), modifier = Modifier.weight(1f))
                            InfoItem("POSTURE", strategy.risk_posture.uppercase(), modifier = Modifier.weight(1f))
                        }
                        
                        Divider(color = HairlineBorder)
                        
                        Column {
                            Text("RATIONALE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strategy.rationale,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // TRANSITION TRIGGERS Section
            event.transition_triggers?.let { triggers ->
                if (triggers.isNotEmpty()) {
                    DetailSectionHeader("TRANSITION TRIGGERS")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        triggers.forEach { trigger ->
                            Surface(
                                color = PureBlack,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(trigger.label, color = Color.White, fontSize = 13.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (trigger.status == "met") Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = trigger.status.uppercase(),
                                            color = if (trigger.status == "met") Color(0xFF4CAF50) else Color(0xFFFFB300),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ALLOWED TACTICS Section
            event.allowed_tactics?.let { tactics ->
                if (tactics.isNotEmpty()) {
                    DetailSectionHeader("ALLOWED TACTICS")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tactics.forEach { tactic ->
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF2C2C35), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tactic,
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // EBC Section
            event.ebc?.let { ebc ->
                DetailSectionHeader("EXECUTION BOUNDARY CONTRACT")
                Surface(
                    color = PureBlack,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("STATUS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(
                                text = ebc.status.name,
                                color = when (ebc.status) {
                                    IntelligenceEBCStatus.PERMITTED -> Color(0xFF4CAF50)
                                    IntelligenceEBCStatus.DEGRADED -> Color(0xFFFFB300)
                                    IntelligenceEBCStatus.BLOCKED -> Color(0xFFE53935)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (ebc.violations.isNotEmpty()) {
                            Divider(color = HairlineBorder)
                            Column {
                                Text("VIOLATIONS", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                ebc.violations.forEach { violation ->
                                    Text(
                                        text = "• $violation",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer Info
            Surface(
                color = IndigoAccent.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "This intelligence is generated by ASC AI based on real-time data feeds and proprietary analysis models.",
                        color = SlateText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF787B86),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
