package com.asc.markets.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AppView
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.SlateText

/**
 * Institutional Notched Bottom Navigation
 * Replicates the Bloomberg design tokens:
 * - Pure Black Background (#000000)
 * - Custom Notched Geometry for floating 56dp FAB
 * - Uppercase clinical labels matching Bloomberg font style
 * - White/Grey color palette for icons and labels
 * - Mechanical haptic feedback on every interaction
 */
@Composable
fun NotchedBottomNav(
    currentView: AppView,
    onNavigate: (AppView) -> Unit,
    onHomeSelected: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. The Notched Background Bar (65dp height)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clip(InstitutionalNotchShape),
            color = PureBlack,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Slots: HOME, MARKETS, (Spacer), MARKET VIEW, MENU
                NavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isActive = currentView == AppView.DASHBOARD
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onHomeSelected != null) {
                        onHomeSelected()
                    } else {
                        onNavigate(AppView.DASHBOARD)
                    }
                }

                NavItem(
                    icon = Icons.Default.BarChart,
                    label = "Markets",
                    isActive = currentView == AppView.MARKETS
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(AppView.MARKETS)
                }

                // Central Spacer for FAB Notch Cradle
                Spacer(modifier = Modifier.width(60.dp))

                NavItem(
                    icon = Icons.Default.Visibility,
                    label = "Market View",
                    isActive = currentView == AppView.MARKET_VIEW
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(AppView.MARKET_VIEW)
                }

                NavItem(
                    icon = Icons.Default.Menu,
                    label = "Menu",
                    isActive = currentView == AppView.SIDEBAR_PAGE
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMenuClick()
                }
            }
        }

        // 2. The Central Action Node (Floating in the Notch)
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val fabScale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "fabScale")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp),
            contentAlignment = Alignment.TopCenter
        ) {
                val fabOffset = remember { Animatable(-20f) }
                val fabScope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    fabOffset.animateTo(0f, animationSpec = tween(durationMillis = 420))
                }

                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigate(AppView.ANALYSIS_RESULTS)
                    },
                    containerColor = Color(0xFF2B2B2B),
                    contentColor = Color.White,
                    shape = CircleShape,
                    interactionSource = interactionSource,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .offset(y = fabOffset.value.dp)
                        .size(56.dp)
                        .scale(fabScale)
                ) {
                    // Sparkles Icon Parity
                    Text("✦", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Bloomberg-style colors: White for active, Grey for inactive
    val targetColor = if (isActive) Color.White else Color(0xFF919191)
    val contentColor by animateColorAsState(targetColor)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Match Bloomberg's mechanical feel (no soft ripple)
                onClick = onClick
            )
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.02.em
        )
    }
}

/**
 * Institutional Notch Geometry
 * Smooth bezier curve creating a circular cradle for the central action button.
 */
val InstitutionalNotchShape = GenericShape { size, _ ->
    // px values tuned for typical densities; Shape path uses pixels directly
    val notchRadius = 42f
    val centerX = size.width / 2f
    val cornerRadius = 20f

    moveTo(0f, cornerRadius)
    
    // Top Left Corner
    quadraticBezierTo(0f, 0f, cornerRadius, 0f)
    
    // Straight line to Notch Start
    lineTo(centerX - notchRadius - 10f, 0f)

    // Smooth Inward Curve
    quadraticBezierTo(
        centerX - notchRadius, 0f,
        centerX - notchRadius, 10f
    )
    
    // Circular Arc Cradle
    arcTo(
        rect = Rect(
            left = centerX - notchRadius,
            top = -10f,
            right = centerX + notchRadius,
            bottom = notchRadius * 1.5f
        ),
        startAngleDegrees = 180f,
        sweepAngleDegrees = -180f,
        forceMoveTo = false
    )

    // Smooth Outward Curve
    quadraticBezierTo(
        centerX + notchRadius, 0f,
        centerX + notchRadius + 10f, 0f
    )

    // Line to Top Right Corner
    lineTo(size.width - cornerRadius, 0f)
    
    // Top Right Corner
    quadraticBezierTo(size.width, 0f, size.width, cornerRadius)
    
    // Close the bar path
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}
