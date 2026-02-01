package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asc.markets.data.EconomicEvent

data class MacroItem(
    val title: String,
    val value: String,
    val sub: String = ""
)

@Composable
fun MacroBasketGrid(
    items: List<MacroItem>? = null,
    modifier: Modifier = Modifier
) {
    val sample = remember {
        items ?: listOf(
            MacroItem("GDP (QoQ)", "+1.2%", "USD"),
            MacroItem("Unemployment", "3.8%", "UK"),
            MacroItem("CPI (YoY)", "2.6%", "EUR"),
            MacroItem("Retail Sales", "+0.4%", "AUD")
        )
    }

    Surface(modifier = modifier.fillMaxWidth()) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            // render two items per row
            items(sample.chunked(2)) { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (cell in row) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cell.title, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(cell.value, color = Color(0xFF60A5FA), fontWeight = FontWeight.ExtraBold)
                                if (cell.sub.isNotEmpty()) Text(cell.sub, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    if (row.size == 1) { // fill the empty cell
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
