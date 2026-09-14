package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.indicators.VolumaticFvgSettings

@Composable
fun VolumaticFvgSettingsModal(
    settings: VolumaticFvgSettings,
    onSettingsChange: (VolumaticFvgSettings) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val bullPalette = listOf("#1AC2D8", "#26A69A", "#4A90D9", "#7C4DFF", "#FF7043")
    val bearPalette = listOf("#D8761A", "#EF5350", "#D84315", "#C62828", "#F57C00")
    var mitigation by remember { mutableStateOf(settings.mitigationSrc) }
    var bullGaps by remember { mutableStateOf(settings.bullGaps) }
    var bearGaps by remember { mutableStateOf(settings.bearGaps) }
    var volumeBars by remember { mutableStateOf(settings.volumeBars) }
    var bullColor by remember { mutableStateOf(settings.bullColorHex) }
    var bearColor by remember { mutableStateOf(settings.bearColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131722),
        title = { Text("Volumatic Fair Value Gaps", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("‹ MTVG FVG ›", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Text("Credit: BigBeluga", color = Color(0xFF787B86), fontSize = 12.sp)

                Text("FVGs Mitigation Source", color = Color(0xFFD1D4DC), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("close", "high/low").forEach { opt ->
                        val selected = mitigation == opt
                        Box(
                            modifier = Modifier
                                .background(if (selected) Color(0xFF2962FF) else Color(0xFF1E222D), RoundedCornerShape(6.dp))
                                .border(1.dp, if (selected) Color(0xFF2962FF) else Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                                .clickable { mitigation = opt }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(opt, color = if (selected) Color.White else Color(0xFFB2B5BE), fontSize = 13.sp)
                        }
                    }
                }

                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bullGaps,
                        onCheckedChange = { bullGaps = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2962FF))
                    )
                    Text("Bullish FVG", color = Color(0xFFD1D4DC), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bearGaps,
                        onCheckedChange = { bearGaps = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2962FF))
                    )
                    Text("Bearish FVG", color = Color(0xFFD1D4DC), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = volumeBars,
                        onCheckedChange = { volumeBars = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2962FF))
                    )
                    Text("Volume Bars", color = Color(0xFFD1D4DC), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                }

                Text("Bullish", color = Color(0xFFD1D4DC), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bullPalette.forEach { hex ->
                        val c = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(c, RoundedCornerShape(4.dp))
                                .border(2.dp, if (bullColor == hex) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { bullColor = hex }
                        )
                    }
                }
                Text("Bearish", color = Color(0xFFD1D4DC), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bearPalette.forEach { hex ->
                        val c = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(c, RoundedCornerShape(4.dp))
                                .border(2.dp, if (bearColor == hex) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { bearColor = hex }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF2A2E39), modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "•••",
                            color = Color(0xFF787B86),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onReset() }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reset", color = Color(0xFF787B86), fontSize = 13.sp, modifier = Modifier.clickable { onReset() })
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF2962FF)) }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            onSettingsChange(
                                VolumaticFvgSettings(
                                    mitigationSrc = mitigation,
                                    bullGaps = bullGaps,
                                    bearGaps = bearGaps,
                                    volumeBars = volumeBars,
                                    bullColorHex = bullColor,
                                    bearColorHex = bearColor
                                )
                            )
                            onDismiss()
                        }) { Text("Ok", color = Color(0xFF2962FF)) }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}