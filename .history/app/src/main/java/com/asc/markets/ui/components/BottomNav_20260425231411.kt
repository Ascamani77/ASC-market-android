package com.asc.markets.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AppView
import com.asc.markets.ui.theme.PureBlack
import androidx.compose.foundation.Canvas

/**
 * Institutional Bottom Navigation
 * Replicates the provided design image exactly:
 * - Flat 65dp Pure Black Bar (#000000)
 * - Navigation Items: HOME, MARKETS (Bars), WATCHLIST (Eye), STREAM (Chart), MENU (Hamburger)
 * - All labels in Uppercase
 */
@Composable
fun NotchedBottomNav(
    currentView: AppView,
    onNavigate: (AppView) -> Unit,
    onHomeSelected: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp),
        color = PureBlack,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. HOME (Institutional Home Icon)
            NavItemContent(
                label = "Home",
                isActive = currentView == AppView.DASHBOARD,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onHomeSelected != null) onHomeSelected() else onNavigate(AppView.DASHBOARD)
                }
            ) { color ->
                Icon(Icons.Default.Home, null, tint = color, modifier = Modifier.size(24.dp))
            }

            // 2. MARKETS (Institutional Bar Chart)
            NavItemContent(
                label = "Markets",
                isActive = currentView == AppView.MARKETS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(AppView.MARKETS)
                }
            ) { color ->
                Icon(Icons.Default.BarChart, null, tint = color, modifier = Modifier.size(24.dp))
            }

            // 3. WATCHLIST (Custom Eye Icon - Arc + Circle centered)
            NavItemContent(
                label = "Watchlist",
                isActive = currentView == AppView.WATCHLIST,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(AppView.WATCHLIST)
                }
            ) { color ->
                Canvas(modifier = Modifier.size(28.dp)) {
                    val strokeWidth = 2.8.dp.toPx()
                    // Re-centered to match standard 24dp icon alignment (bottom padding = 2dp)
                    val eyeCenterY = center.y + 2.dp.toPx()
                    
                    // Pupil (Centered Circle)
                    drawCircle(
                        color = color,
                        radius = 4.2.dp.toPx(),
                        center = Offset(center.x, eyeCenterY)
                    )
                    // Upper Lid (Arc centered horizontally over the pupil)
                    drawArc(
                        color = color,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - 12.dp.toPx(), eyeCenterY - 10.dp.toPx()),
                        size = Size(24.dp.toPx(), 20.dp.toPx())
                    )
                }
            }

            // 4. STREAM (Bottom menu stream destination)
            NavItemContent(
                label = "Stream",
                isActive = currentView == AppView.MACRO_STREAM,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(AppView.MACRO_STREAM)
                }
            ) { color ->
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 5. MENU (Hamburger Menu)
            NavItemContent(
                label = "Menu",
                isActive = currentView == AppView.SIDEBAR_PAGE,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenuClick()
                }
            ) { color ->
                Icon(Icons.Default.Menu, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun NavItemContent(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    iconContent: @Composable (Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val targetColor = if (isActive) Color.White else Color(0xFF919191)
    val contentColor by animateColorAsState(targetColor, label = "navColor")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Match institutional mechanical feel
                onClick = onClick
            )
            .padding(horizontal = 4.dp)
    ) {
        // Fixed-height box ensures all icons are vertically centered on the same line
        Box(
            modifier = Modifier.height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            iconContent(contentColor)
        }
        
        // Consistent space between icon and label (matches standard icon alignment)
        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label.uppercase(),
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.02.em
        )
    }
}
