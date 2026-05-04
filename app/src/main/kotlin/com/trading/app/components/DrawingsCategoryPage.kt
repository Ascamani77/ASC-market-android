package com.trading.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DrawingsCategoryPage(
    category: DrawingCategory,
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (category) {
        DrawingCategory.FAVORITES -> FavoritesDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.TOOLS -> ToolsDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.TREND_LINES -> TrendLinesDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.GANN_AND_FIBONACCI -> GannAndFibonacciDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.PATTERNS -> PatternsDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.FORECASTING_AND_MEASUREMENT -> ForecastingAndMeasurementDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.GEOMETRIC_SHAPES -> GeometricShapesDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.ANNOTATION -> AnnotationDrawingsPage(searchQuery, onToolSelect, modifier)
        DrawingCategory.VISUALS -> VisualsDrawingsPage(searchQuery, onToolSelect, modifier)
    }
}
