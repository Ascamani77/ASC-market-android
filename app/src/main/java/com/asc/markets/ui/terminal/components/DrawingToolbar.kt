package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DrawingToolbar(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        color = Color(0xFF1E222D),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF363A45))
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { /* Settings */ }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
            
            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF363A45)))
            
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}
