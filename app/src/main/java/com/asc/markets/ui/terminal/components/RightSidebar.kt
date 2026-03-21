package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.asc.markets.ui.terminal.theme.*

@Composable
fun RightSidebar(
    activeUtility: String? = "watchlist",
    onUtilityClick: (String) -> Unit = {},
    onToggleExpand: () -> Unit = {}
) {
    val utilities = listOf(
        UtilityItem("watchlist", Icons.Default.Timeline, "Watchlist"),
        UtilityItem("alerts", Icons.Default.Notifications, "Alerts"),
        UtilityItem("data", Icons.Default.Storage, "Data Window"),
        UtilityItem("hotlist", Icons.Default.AutoAwesome, "Hotlists"),
        UtilityItem("calendar", Icons.Default.CalendarToday, "Calendar"),
        UtilityItem("ideas", Icons.Default.Lightbulb, "My Ideas"),
        UtilityItem("chats_public", Icons.Default.Forum, "Public Chats"),
        UtilityItem("chats_private", Icons.Default.ChatBubble, "Private Chats"),
        UtilityItem("idea_stream", Icons.Default.Newspaper, "Idea Stream"),
        UtilityItem("notifications", Icons.Default.NotificationsActive, "Notifications"),
        UtilityItem("order_panel", Icons.Default.GridView, "Order Panel"),
        UtilityItem("dom", Icons.Default.Layers, "DOM"),
        UtilityItem("object_tree", Icons.Default.AccountTree, "Object Tree")
    )

    Row(modifier = Modifier.fillMaxHeight()) {
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
        
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            utilities.forEach { util ->
                UtilityButton(
                    icon = util.icon,
                    label = util.label,
                    active = activeUtility == util.id,
                    onClick = { onUtilityClick(util.id) }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            UtilityButton(Icons.Default.Fullscreen, "Expand", onClick = onToggleExpand)
            UtilityButton(Icons.Default.HelpOutline, "Help")
        }
    }
}

data class UtilityItem(val id: String, val icon: ImageVector, val label: String)

@Composable
private fun UtilityButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .background(
                if (active) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color.White else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
