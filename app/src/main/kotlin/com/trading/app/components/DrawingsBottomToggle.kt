package com.trading.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawingsBottomToggle(
    showFavoritesOnChart: Boolean,
    onShowFavoritesOnChartChange: (Boolean) -> Unit
) {
    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = Color(0xFFD1D4DC),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Show favorites on Chart",
                color = Color(0xFFD1D4DC),
                fontSize = 14.sp
            )
        }
        Switch(
            checked = showFavoritesOnChart,
            onCheckedChange = onShowFavoritesOnChartChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2962FF),
                uncheckedThumbColor = Color(0xFF787B86),
                uncheckedTrackColor = Color(0xFF2A2E39)
            )
        )
    }
}
