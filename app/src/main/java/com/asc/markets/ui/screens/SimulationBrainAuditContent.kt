package com.asc.markets.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimulationBrainAuditContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Surface(
                color = Color(0xFFEAB308).copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "AI Brain Audit",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deep analysis of AI decision patterns and confidence calibration.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        // Cards Section
        BrainAuditCard(
            label = "DECISION CONSISTENCY",
            value = "85%",
            description = "Demo Mode: AI shows high strategy consistency.",
            valueColor = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BrainAuditCard(
            label = "CONFIDENCE BIAS",
            value = "+2.4%",
            description = "Demo Mode: Calibration is within optimal range.",
            valueColor = Color(0xFFEF4444) // Red
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BrainAuditCard(
            label = "OPTIMIZATION EFFICIENCY",
            value = "92%",
            description = "Demo Mode: SL/TP optimization active.",
            valueColor = Color(0xFF10B981) // Green
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Recent Decision Logs Header
        Text(
            text = "RECENT DECISION LOGS",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Placeholder for Decision Logs
        DecisionLogPlaceholder()
    }
}

@Composable
fun BrainAuditCard(label: String, value: String, description: String, valueColor: Color) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DecisionLogPlaceholder() {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "No decision logs recorded in this session.",
                color = Color.DarkGray,
                fontSize = 12.sp
            )
        }
    }
}
