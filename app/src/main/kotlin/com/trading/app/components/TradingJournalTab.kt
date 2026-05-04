package com.trading.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.JournalEntry

@Composable
fun TradingJournalTab(
    entries: List<JournalEntry> = sampleJournalEntries,
    labelColor: Color = Color(0xFF787B86)
) {
    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No journal entries available", color = labelColor, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(entries) { entry ->
                JournalEntryRow(entry, labelColor)
                Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun JournalEntryRow(entry: JournalEntry, labelColor: Color) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Time",
                color = labelColor,
                fontSize = 14.sp,
                modifier = Modifier.width(50.dp)
            )
            Text(
                text = entry.time,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            // Small indicator/icon on the top right as seen in image
            if (entry == sampleJournalEntries.firstOrNull()) {
                Text(
                    text = "000",
                    color = labelColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Text",
                color = labelColor,
                fontSize = 14.sp,
                modifier = Modifier.width(50.dp)
            )
            Text(
                text = entry.text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

val sampleJournalEntries = listOf(
    JournalEntry(
        time = "2026-04-02 19:52:29",
        text = "Order 2934774519 for symbol\nPEPPERSTONE:XAUUSD has\nbeen executed at price\n4665.06 for 1 units"
    ),
    JournalEntry(
        time = "2026-04-02 19:52:29",
        text = "Order 2934774519\nsuccessfully placed"
    ),
    JournalEntry(
        time = "2026-04-02 19:52:29",
        text = "Call to place limit order to\nbuy 1 units of symbol\nPEPPERSTONE:XAUUSD at\nprice 5000.00"
    ),
    JournalEntry(
        time = "2026-04-02 19:47:58",
        text = "Order 2934760578 for\nsymbol\nPEPPERSTONE:XAUUSD has\nbeen executed at price\n4666.96 for 1 units"
    ),
    JournalEntry(
        time = "2026-04-02 19:47:58",
        text = "Order 2934760578\nsuccessfully placed"
    )
)
