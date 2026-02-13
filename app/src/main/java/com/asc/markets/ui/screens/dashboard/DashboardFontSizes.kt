package com.asc.markets.ui.screens.dashboard

import androidx.compose.ui.unit.sp

/**
 * Centralized dashboard font size definitions
 * Change values here to update all dashboard components globally
 */
object DashboardFontSizes {
    
    // === SECTION HEADERS ===
    // Large headers for major sections (e.g., "Overview", "Session Progress")
    val sectionHeaderLarge = 16.sp      // Was 14.sp, now +2.sp
    val sectionHeaderMedium = 15.sp     // Was 13.sp, now +2.sp
    val sectionHeaderSmall = 14.sp      // Was 12.sp, now +2.sp
    
    // === SUB-HEADERS & LABELS ===
    // Labels for content groups and field names
    val labelLarge = 13.sp              // Was 11.sp, now +2.sp
    val labelMedium = 12.sp             // Was 10.sp, now +2.sp
    val labelSmall = 11.sp              // Was 9.sp, now +2.sp
    
    // === VALUES & DATA DISPLAY ===
    // Primary values and important metrics
    val valueLarge = 20.sp              // Was 18.sp, now +2.sp
    val valueMediumLarge = 16.sp        // Was 14.sp, now +2.sp
    val valueMedium = 14.sp             // Was 12.sp, now +2.sp
    val valueSmall = 13.sp              // Was 11.sp, now +2.sp
    
    // === BODY TEXT ===
    // Regular content and descriptions
    val bodyMedium = 13.sp              // Was 11.sp, now +2.sp
    val bodySmall = 12.sp               // Was 10.sp, now +2.sp
    val bodyTiny = 11.sp                // Was 9.sp, now +2.sp
    
    // === SPECIAL CASES ===
    val dashboardActiveSession = 22.sp  // Large dashboard header (unchanged from DashboardOverviewTab)
    val sessionLabel = 12.sp            // Session info labels
    val vitalsKpiLabel = 11.sp          // KPI card labels
    val vitalsKpiValue = 20.sp          // KPI card values
    
    // === GRID & COMPACT DISPLAY ===
    val gridHeaderSmall = 12.sp         // For metric labels in grids
    val gridValueSmall = 14.sp          // For metric values in grids
    val gridLabelTiny = 11.sp           // Tiny labels in grids
    val gridValueTiny = 12.sp           // Tiny values in grids
    
    // === SPECIAL TEXT ===
    val emojiIcon = 16.sp               // For emoji icons
    val tinyCaption = 11.sp             // For captions and footnotes
    val aiScopeNote = 11.sp             // For AI scope note at bottom
    
    // === MICRO TEXT ===
    val microCaption = 8.sp             // For very small captions and tags
    
    // === LARGE METRIC DISPLAYS ===
    val sentimentScore = 56.sp          // For large sentiment/psychology score display
    val qualityScore = 42.sp            // For quality/audit metrics large display
    val verificationCheckmark = 26.sp   // For large checkmark/verification icons
    val signalZoneEmoji = 72.sp         // For large emoji in signal zones
    val watermarkText = 96.sp           // For watermark background text (ASC, etc.)
}
