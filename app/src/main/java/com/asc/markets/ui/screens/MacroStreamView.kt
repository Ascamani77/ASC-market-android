package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.MacroEventStatus
import com.asc.markets.data.ImpactPriority
import com.asc.markets.data.sampleMacroEvents
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MacroStreamView(events: List<MacroEvent> = sampleMacroEvents()) {
    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Header: visual vitals
        Surface(color = Color.Black, modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(8.dp)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Macro Intelligence Stream", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Surveillance Node — clinical view", color = SlateText, fontSize = 10.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Regime Weighting: 67%", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("Pre-Event Awareness: 90%", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // Main area: 90% Upcoming Intel Node previews
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Left: Upcoming (90%)
            Column(modifier = Modifier.weight(0.9f).fillMaxHeight().padding(end = 8.dp)) {
                InfoBox(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text("Upcoming Intel Nodes", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            val upcoming = events.filter { it.status == MacroEventStatus.UPCOMING }
                            items(upcoming) { ev ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(ev.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                        Text(ev.source, color = SlateText, fontSize = 10.sp)
                                    }
                                    // countdown
                                    val dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ev.datetimeUtc), ZoneOffset.UTC)
                                    Text(dt.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm'Z'")), color = IndigoAccent, fontSize = 11.sp)
                                }
                                Divider(color = Color.White.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }

            // Right: Confirmed History (10%)
            Column(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                InfoBox(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        Text("Confirmed History", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        val confirmed = events.filter { it.status == MacroEventStatus.CONFIRMED }
                        confirmed.take(6).forEach { c ->
                            Text(c.title, color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

// sampleMacroEvents now provided by com.asc.markets.data.SampleData
