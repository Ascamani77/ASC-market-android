package com.trading.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val patternTools = listOf(
    DrawingToolUiItem(id = "xabcd_pattern", name = "XABCD Pattern", icon = DrawingIcons.XABCDPattern),
    DrawingToolUiItem(id = "cypher_pattern", name = "Cypher Pattern", icon = DrawingIcons.CypherPattern),
    DrawingToolUiItem(id = "head_and_shoulders", name = "Head and Shoulders", icon = DrawingIcons.HeadAndShoulders),
    DrawingToolUiItem(id = "abcd_pattern", name = "ABCD Pattern", icon = DrawingIcons.ABCDPattern),
    DrawingToolUiItem(id = "triangle_pattern", name = "Triangle Pattern", icon = DrawingIcons.TrianglePattern),
    DrawingToolUiItem(id = "three_drives_pattern", name = "Three Drives Pattern", icon = DrawingIcons.ThreeDrivesPattern),
    DrawingToolUiItem(id = "elliott_impulse_wave", name = "Elliott Impulse Wav...", icon = DrawingIcons.ElliottImpulseWave),
    DrawingToolUiItem(id = "elliott_correction_wave", name = "Elliott Correction W...", icon = DrawingIcons.ElliottCorrectionWave),
    DrawingToolUiItem(id = "elliott_triangle_wave", name = "Elliott Triangle Wav...", icon = DrawingIcons.ElliottTriangleWave),
    DrawingToolUiItem(id = "elliott_double_combo_wave", name = "Elliott Double Com...", icon = DrawingIcons.ElliottDoubleComboWave),
    DrawingToolUiItem(id = "elliott_triple_combo_wave", name = "Elliott Triple Comb...", icon = DrawingIcons.ElliottTripleComboWave),
    DrawingToolUiItem(id = "cyclic_lines", name = "Cyclic Lines", icon = DrawingIcons.CyclicLines),
    DrawingToolUiItem(id = "time_cycles", name = "Time Cycles", icon = DrawingIcons.TimeCycles),
    DrawingToolUiItem(id = "sine_line", name = "Sine Line", icon = DrawingIcons.SineLine)
)

@Composable
fun PatternsDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = patternTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
