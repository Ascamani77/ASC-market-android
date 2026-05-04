package com.trading.app.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun QuickActionsButton(
    onClick: () -> Unit,
    offset: IntOffset,
    onOffsetChange: (IntOffset) -> Unit,
    isLocked: Boolean = false,
    isModalOpen: Boolean = false,
    autoHideDelay: Long = 3000L,
    modifier: Modifier = Modifier
) {
    var isFaded by remember { mutableStateOf(false) }
    var interactionTrigger by remember { mutableIntStateOf(0) }

    val alpha by animateFloatAsState(
        targetValue = if (isFaded && !isModalOpen) 0.3f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "ButtonFadeAlpha"
    )

    LaunchedEffect(interactionTrigger, isModalOpen) {
        if (isModalOpen) {
            isFaded = false
            return@LaunchedEffect
        }
        isFaded = false
        delay(autoHideDelay)
        isFaded = true
    }

    val currentOffset by rememberUpdatedState(offset)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)

    Box(
        modifier = modifier
            .offset { currentOffset }
            .size(63.dp)
            .graphicsLayer(alpha = alpha)
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures(
                        onDragStart = {
                            isFaded = false
                            interactionTrigger++
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnOffsetChange(
                                IntOffset(
                                    (currentOffset.x + dragAmount.x).roundToInt(),
                                    (currentOffset.y + dragAmount.y).roundToInt()
                                )
                            )
                            interactionTrigger++
                        }
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isFaded) {
                        isFaded = false
                        interactionTrigger++
                    } else {
                        onClick()
                        interactionTrigger++
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF121212))
                .border(2.dp, Color(0xFF363A45), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(25.dp)) {
                val strokeWidth = 2.25.dp.toPx()
                val iconColor = Color(0xFF787B86)
                
                // Vertical stack of vertical (tall) diamonds
                val topPath = Path().apply {
                    moveTo(size.width / 2, size.height * 0.1f)
                    lineTo(size.width * 0.75f, size.height * 0.35f)
                    lineTo(size.width / 2, size.height * 0.6f)
                    lineTo(size.width * 0.25f, size.height * 0.35f)
                    close()
                }

                val bottomPath = Path().apply {
                    moveTo(size.width / 2, size.height * 0.4f)
                    lineTo(size.width * 0.75f, size.height * 0.65f)
                    lineTo(size.width / 2, size.height * 0.9f)
                    lineTo(size.width * 0.25f, size.height * 0.65f)
                    close()
                }

                drawPath(
                    path = bottomPath,
                    color = iconColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                drawPath(
                    path = topPath,
                    color = iconColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        if (isLocked) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-3.6).dp, y = 3.6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2962FF))
                    .padding(3.6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White,
                    modifier = Modifier.size(10.8.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionsModal(
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    isHeaderVisible: Boolean,
    onHeaderToggle: () -> Unit,
    isBottomMenuVisible: Boolean,
    onBottomMenuToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onDrawingsClick: () -> Unit,
    onChartTypeClick: () -> Unit,
    isTimezoneVisible: Boolean,
    onTimezoneToggle: () -> Unit,
    isCrosshairActive: Boolean,
    onCrosshairToggle: () -> Unit,
    onAlertClick: () -> Unit,
    onReplayClick: () -> Unit,
    isReplayActive: Boolean,
    isLocked: Boolean = false,
    onLockToggle: () -> Unit = {},
    onClose: () -> Unit,
    offset: IntOffset,
    onOffsetChange: (IntOffset) -> Unit
) {
    val currentOffset by rememberUpdatedState(offset)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    val scrollState = rememberScrollState()
    
    var isFaded by remember { mutableStateOf(false) }
    var interactionTrigger by remember { mutableIntStateOf(0) }

    val alpha by animateFloatAsState(
        targetValue = if (isFaded) 0.3f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "ModalFadeAlpha"
    )

    LaunchedEffect(interactionTrigger) {
        isFaded = false
        delay(5000L) // 5 seconds idle timeout
        isFaded = true
    }

    Box(
        modifier = Modifier
            .offset { currentOffset }
            .graphicsLayer(alpha = alpha)
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures { change, dragAmount ->
                        interactionTrigger++
                        change.consume()
                        currentOnOffsetChange(
                            IntOffset(
                                (currentOffset.x + dragAmount.x).roundToInt(),
                                (currentOffset.y + dragAmount.y).roundToInt()
                            )
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        interactionTrigger++
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { /* Consume taps inside modal */ }
            }
            .width(260.dp)
            .heightIn(max = 525.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212).copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionButtonItem(
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            label = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            onClick = { onFullscreenToggle(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButtonItem(
                            icon = if (isHeaderVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            label = if (isHeaderVisible) "Hide Header" else "Show Header",
                            onClick = { onHeaderToggle(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionButtonItem(
                            icon = Icons.Default.Menu,
                            label = if (isBottomMenuVisible) "Hide Bottom Menu" else "Show Bottom Menu",
                            onClick = { onBottomMenuToggle(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButtonItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = { onSettingsClick(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionButtonItem(
                            icon = Icons.Default.Edit,
                            label = "Drawings Indicators",
                            onClick = { onDrawingsClick(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButtonItem(
                            icon = Icons.Default.BarChart,
                            label = "Chart Type",
                            onClick = { onChartTypeClick(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionButtonItem(
                            icon = Icons.Default.FilterCenterFocus,
                            label = "Crosshair",
                            onClick = { onCrosshairToggle(); interactionTrigger++ },
                            isActive = isCrosshairActive,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButtonItem(
                            icon = Icons.Outlined.NotificationsNone,
                            label = "Alert",
                            onClick = { onAlertClick(); interactionTrigger++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionButtonItem(
                            icon = Icons.Default.Replay,
                            label = "Replay",
                            onClick = { onReplayClick(); interactionTrigger++ },
                            isActive = isReplayActive,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            
            QuickActionToggleRow(
                label = "Lock Button Position",
                isActive = isLocked,
                onToggle = { onLockToggle(); interactionTrigger++ }
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            QuickActionToggleRow(
                label = if (isTimezoneVisible) "Hide Timezone Pane" else "Show Timezone Pane",
                isActive = isTimezoneVisible,
                onToggle = { onTimezoneToggle(); interactionTrigger++ }
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = Color(0xFF434651),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isLocked) "Button is pinned" else "Drag to move",
                    color = Color(0xFF434651),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QuickActionToggleRow(
    label: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .size(36.dp, 20.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFF2962FF) else Color(0xFF434651))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(if (isActive) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun QuickActionButtonItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp, 
                if (isActive) Color(0xFF2962FF) else Color.White.copy(alpha = 0.1f), 
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color(0xFF2962FF) else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) Color(0xFF2962FF) else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp
        )
    }
}
