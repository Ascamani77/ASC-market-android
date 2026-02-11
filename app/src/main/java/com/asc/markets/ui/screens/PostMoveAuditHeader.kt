package com.asc.markets.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PostMoveAuditHeader(
    showMainHeader: MutableState<Boolean>,
    filterState: MutableState<String>,
    viewModel: ForexViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val headerTopPad = (configuration.screenHeightDp * 0.02f).dp

    var mainHeaderHeightPx by remember { mutableStateOf(0) }
    var submenuHeightPx by remember { mutableStateOf(0) }

    val targetOffset = if (showMainHeader.value) 0 else -mainHeaderHeightPx
    val animatedOffset by animateIntAsState(targetValue = targetOffset, animationSpec = tween(180))

    val animatedHeaderTopPad by animateDpAsState(targetValue = if (showMainHeader.value) headerTopPad else 6.dp, animationSpec = tween(180))

    Column(modifier = Modifier.offset { IntOffset(0, animatedOffset) }) {
        // Main local header (measured)
        Surface(color = PureBlack, modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { mainHeaderHeightPx = it.size.height }) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = animatedHeaderTopPad), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("POST-MOVE AUDIT", color = Color.White, style = TerminalTypography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily))
                }
                Row(modifier = Modifier.wrapContentWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { /* toggle search modal */ Toast.makeText(context, "Search", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) }
                    IconButton(onClick = {
                        try {
                            val uri = Uri.parse("asc://settings?section=post_move_audit")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                `package` = context.packageName
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White) }
                    var moreOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { moreOpen = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White) }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(text = { Text("Clear Ledger") }, onClick = { viewModel.clearAuditLedger(); moreOpen = false })
                            DropdownMenuItem(text = { Text("Mark All Audited") }, onClick = { viewModel.markAllAuditRecordsAudited(); moreOpen = false })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sticky Sub-Menu (moves together with header for smooth transition)
        val pills = listOf("ALL", "SIMPLE ALERTS", "SMART ALERTS", "NEWS", "STRATEGY", "SYSTEM", "ACCOUNT")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pills) { p ->
                val active = p == filterState.value
                Surface(
                    color = if (active) Color(0xFF2d2d2d) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = if (!active) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                    modifier = Modifier
                        .clickable { filterState.value = p }
                ) {
                    Text(
                        p,
                        color = if (active) Color.White else Color(0xFF94a3b8),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}
