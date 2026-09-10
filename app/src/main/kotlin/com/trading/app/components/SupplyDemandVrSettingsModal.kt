package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.indicators.SupplyDemandVrSettings

private val SD_VR_PALETTE = listOf(
    "#2157f3", "#787b86", "#f44336", "#81c784", "#4caf50", "#009688",
    "#64b5f6", "#2962ff", "#9c27b0", "#e91e63", "#ff5d00", "#ff9800", "#ffffff"
)

@Composable
fun SupplyDemandVrSettingsModal(
    settings: SupplyDemandVrSettings,
    onChange: (SupplyDemandVrSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var thresholdText by remember(settings) { mutableStateOf(settings.thresholdPercent.toString()) }
    var resolutionText by remember(settings) { mutableStateOf(settings.resolution.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            color = Color(0xFF1E222D),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Supply & Demand Visible Range", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))

                SdVrSectionTitle("Calculation")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrNumberField(
                        value = thresholdText,
                        onValueChange = {
                            thresholdText = it
                            it.toFloatOrNull()?.let { v -> onChange(settings.copy(thresholdPercent = v.coerceIn(0f, 100f))) }
                        },
                        label = "Threshold %"
                    )
                    Spacer(Modifier.width(12.dp))
                    SdVrNumberField(
                        value = resolutionText,
                        onValueChange = {
                            resolutionText = it
                            it.toIntOrNull()?.let { v -> if (v >= 2) onChange(settings.copy(resolution = v.coerceIn(2, 500))) }
                        },
                        label = "Resolution"
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = settings.alertsEnabled,
                        onCheckedChange = { onChange(settings.copy(alertsEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2962FF))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Zone entry alarms (toast)", color = Color(0xFFD1D4DC), fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))

                SdVrSectionTitle("Supply")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrSwitchRow("Show", settings.showSupply) { onChange(settings.copy(showSupply = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrColorSwatch(settings.supplyColorHex) { hex -> onChange(settings.copy(supplyColorHex = hex)) }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrSwitchRow("Area", settings.supplyArea) { onChange(settings.copy(supplyArea = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrSwitchRow("Average", settings.supplyAvg) { onChange(settings.copy(supplyAvg = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrSwitchRow("Weighted", settings.supplyWavg) { onChange(settings.copy(supplyWavg = it)) }
                }
                Spacer(Modifier.height(12.dp))

                SdVrSectionTitle("Equilibrium")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrSwitchRow("Show", settings.showEquilibrium) { onChange(settings.copy(showEquilibrium = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrColorSwatch(settings.equilibriumColorHex) { hex -> onChange(settings.copy(equilibriumColorHex = hex)) }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrSwitchRow("Average", settings.equilibriumAvg) { onChange(settings.copy(equilibriumAvg = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrSwitchRow("Weighted", settings.equilibriumWavg) { onChange(settings.copy(equilibriumWavg = it)) }
                }
                Spacer(Modifier.height(12.dp))

                SdVrSectionTitle("Demand")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrSwitchRow("Show", settings.showDemand) { onChange(settings.copy(showDemand = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrColorSwatch(settings.demandColorHex) { hex -> onChange(settings.copy(demandColorHex = hex)) }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SdVrSwitchRow("Area", settings.demandArea) { onChange(settings.copy(demandArea = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrSwitchRow("Average", settings.demandAvg) { onChange(settings.copy(demandAvg = it)) }
                    Spacer(Modifier.width(10.dp))
                    SdVrSwitchRow("Weighted", settings.demandWavg) { onChange(settings.copy(demandWavg = it)) }
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onChange(SupplyDemandVrSettings()) }) { Text("Reset", color = Color(0xFFF23645)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                    ) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun SdVrSectionTitle(text: String) {
    Text(text, color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SdVrSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2962FF))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color(0xFFD1D4DC), fontSize = 13.sp)
    }
}

@Composable
private fun SdVrNumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    Column {
        Text(label, color = Color(0xFFD1D4DC), fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(120.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun SdVrColorSwatch(hex: String, onPick: (String) -> Unit) {
    val idx = SD_VR_PALETTE.indexOfFirst { it == hex }.coerceAtLeast(0)
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(Color(android.graphics.Color.parseColor(SD_VR_PALETTE[idx])), CircleShape)
            .border(1.dp, Color(0xFF363A45), CircleShape)
            .clickable {
                onPick(SD_VR_PALETTE[(idx + 1) % SD_VR_PALETTE.size])
            }
    )
}
