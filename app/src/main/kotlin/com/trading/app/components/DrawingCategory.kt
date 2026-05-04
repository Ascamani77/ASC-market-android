package com.trading.app.components

enum class DrawingCategory(val title: String) {
    FAVORITES("Favorites"),
    TOOLS("Tools"),
    TREND_LINES("Trend lines"),
    GANN_AND_FIBONACCI("Gann and Fibonacci"),
    PATTERNS("Patterns"),
    FORECASTING_AND_MEASUREMENT("Forecasting and measurement"),
    GEOMETRIC_SHAPES("Geometric shapes"),
    ANNOTATION("Annotation"),
    VISUALS("Visuals");

    companion object {
        val ordered: List<DrawingCategory> = values().toList()
    }
}
