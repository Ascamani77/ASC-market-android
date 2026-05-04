package com.trading.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val forecastingAndMeasurementTools = listOf(
    DrawingToolUiItem(id = "long_position", name = "Long Position", icon = DrawingIcons.LongPosition),
    DrawingToolUiItem(id = "short_position", name = "Short Position", icon = DrawingIcons.ShortPosition),
    DrawingToolUiItem(id = "forecast", name = "Forecast", icon = DrawingIcons.Forecast),
    DrawingToolUiItem(id = "bars_pattern", name = "Bars Pattern", icon = DrawingIcons.BarsPattern),
    DrawingToolUiItem(id = "ghost_feed", name = "Ghost Feed", icon = DrawingIcons.GhostFeed),
    DrawingToolUiItem(id = "projection", name = "Projection", icon = DrawingIcons.Projection),
    DrawingToolUiItem(id = "anchored_vwap", name = "Anchored VWAP", icon = DrawingIcons.AnchoredVWAP),
    DrawingToolUiItem(id = "fixed_range_volume", name = "Fixed Range Volume...", icon = DrawingIcons.FixedRangeVolume),
    DrawingToolUiItem(id = "anchored_volume", name = "Anchored Volume ...", icon = DrawingIcons.AnchoredVolume),
    DrawingToolUiItem(id = "price_range", name = "Price Range", icon = DrawingIcons.PriceRange),
    DrawingToolUiItem(id = "date_range", name = "Date Range", icon = DrawingIcons.DateRange),
    DrawingToolUiItem(id = "date_and_price_range", name = "Date and Price Ran...", icon = DrawingIcons.DateAndPriceRange)
)

@Composable
fun ForecastingAndMeasurementDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = forecastingAndMeasurementTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
