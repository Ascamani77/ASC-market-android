package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.terminal.theme.*
import com.asc.markets.ui.terminal.viewmodels.ChartViewModel
import com.asc.markets.ui.terminal.models.DrawingItem

@Composable
fun RightPanelContent(
    activeUtility: String?,
    viewModel: ChartViewModel,
    onClose: () -> Unit
) {
    if (activeUtility == null) return
    
    val drawings by viewModel.drawings.collectAsState()
    val indicators by viewModel.indicators.collectAsState()

    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeUtility.uppercase().replace("_", " "),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        HorizontalDivider(color = BorderColor)
        
        if (activeUtility == "object_tree") {
            ObjectTree(
                drawings = drawings,
                indicators = indicators,
                onRemoveDrawing = { viewModel.removeDrawing(it) },
                onRemoveIndicator = { viewModel.toggleIndicator(it) }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Details for ${activeUtility.replace("_", " ")}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
}

@Composable
fun ObjectTree(
    drawings: List<DrawingItem>,
    indicators: List<String>,
    onRemoveDrawing: (String) -> Unit,
    onRemoveIndicator: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (indicators.isNotEmpty()) {
            item { SectionHeader("Indicators") }
            items(indicators) { indicator ->
                ObjectItem(
                    name = indicator.uppercase(),
                    type = "Indicator",
                    icon = Icons.Default.Timeline,
                    onRemove = { onRemoveIndicator(indicator) }
                )
            }
        }
        
        if (drawings.isNotEmpty()) {
            item { SectionHeader("Drawings") }
            items(drawings) { drawing ->
                ObjectItem(
                    name = drawing.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                    type = "Drawing",
                    icon = Icons.Default.Category,
                    onRemove = { onRemoveDrawing(drawing.id) }
                )
            }
        }
        
        if (indicators.isEmpty() && drawings.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No objects on chart", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ObjectItem(
    name: String,
    type: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, color = Color.White, fontSize = 12.sp)
                Text(type, color = TextSecondary, fontSize = 9.sp)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
}
