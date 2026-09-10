package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Shared UI Components and Utilities for ASC Markets App
 * 
 * This file contains reusable composables and helper functions
 * that are used across multiple screens in the app.
 */

// ============================================================================
// COMPOSABLE COMPONENTS
// ============================================================================

/**
 * Display a detail item with label and value
 */
@Composable
fun DetailItem(
    label: String, 
    value: String, 
    valueColor: Color = Color.White
) {
    Column {
        Text(
            text = label,
            color = SlateText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
    }
}

/**
 * "NEW" badge indicator
 */
@Composable
fun NewBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LuminousBlue
    ) {
        Text(
            text = "NEW",
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Action button with icon (circular)
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.1f),
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Action button with text and icon (rectangular)
 */
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = LuminousBlue.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = LuminousBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = LuminousBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Parse time string to minutes for sorting
 * Example: "5m" -> 5, "2h" -> 120
 */
fun parseTimeToEventMinutes(time: String): Int {
    if (time.isBlank() || time == "N/A") return Int.MAX_VALUE
    
    val number = time.filter { it.isDigit() }.toIntOrNull() ?: return Int.MAX_VALUE
    
    return when {
        time.contains("s", ignoreCase = true) -> number / 60 // seconds to minutes
        time.contains("m", ignoreCase = true) -> number // already in minutes
        time.contains("h", ignoreCase = true) -> number * 60 // hours to minutes
        time.contains("d", ignoreCase = true) -> number * 1440 // days to minutes
        else -> number // assume minutes
    }
}

/**
 * Format timestamp to "X ago" format
 */
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

/**
 * Format ISO timestamp to readable format
 * Example: "2024-01-15T10:30:45.123Z" -> "Jan 15, 10:30:45"
 * 
 * Note: Use with remember {} in composables to avoid locale observation warnings:
 * val formatted = remember(timestamp) { formatTimestamp(timestamp) }
 */
fun formatTimestamp(isoTimestamp: String): String {
    return try {
        // Use US locale to avoid non-observable locale access in composables
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = inputFormat.parse(isoTimestamp.substringBefore('.'))
        
        val outputFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        // Fallback to original string if parsing fails
        isoTimestamp
    }
}

/**
 * Format timestamp with custom pattern
 * 
 * Note: Use with remember {} in composables to avoid locale observation warnings:
 * val formatted = remember(timestamp) { formatTimestamp(timestamp, pattern) }
 */
fun formatTimestamp(isoTimestamp: String, pattern: String): String {
    return try {
        // Use US locale to avoid non-observable locale access in composables
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = inputFormat.parse(isoTimestamp.substringBefore('.'))
        
        val outputFormat = SimpleDateFormat(pattern, Locale.US)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        isoTimestamp
    }
}

/**
 * Get color based on confidence level
 */
fun getConfidenceColor(confidence: Double): Color {
    return when {
        confidence >= 80.0 -> GreenProfit
        confidence >= 60.0 -> Color(0xFFF59E0B) // Amber
        confidence >= 40.0 -> Color(0xFFEAB308) // Yellow
        else -> RoseError
    }
}

/**
 * Get color based on score (0-100)
 */
fun getScoreColor(score: Double): Color {
    return when {
        score >= 75.0 -> GreenProfit
        score >= 50.0 -> LuminousBlue
        score >= 25.0 -> Color(0xFFF59E0B) // Amber
        else -> RoseError
    }
}

/**
 * Format percentage with sign
 */
fun formatPercentage(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return "$sign%.2f%%".format(value)
}

/**
 * Format pips with sign
 */
fun formatPips(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return "$sign%.1f".format(value)
}
