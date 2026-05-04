package com.trading.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val geometricShapeTools = listOf(
    DrawingToolUiItem(id = "brush", name = "Brush", icon = DrawingIcons.Brush),
    DrawingToolUiItem(id = "highlighter", name = "Highlighter", icon = DrawingIcons.Highlighter),
    DrawingToolUiItem(id = "arrow_marker", name = "Arrow Marker", icon = DrawingIcons.ArrowMarker),
    DrawingToolUiItem(id = "arrow", name = "Arrow", icon = DrawingIcons.Arrow),
    DrawingToolUiItem(id = "arrow_marker_up", name = "Arrow Marker Up", icon = DrawingIcons.ArrowMarkerUp),
    DrawingToolUiItem(id = "arrow_marker_down", name = "Arrow Marker Down", icon = DrawingIcons.ArrowMarkerDown),
    DrawingToolUiItem(id = "rectangle", name = "Rectangle", icon = DrawingIcons.Rectangle),
    DrawingToolUiItem(id = "rotated_rectangle", name = "Rotated Rectangle", icon = DrawingIcons.RotatedRectangle),
    DrawingToolUiItem(id = "path", name = "Path", icon = DrawingIcons.Path),
    DrawingToolUiItem(id = "circle", name = "Circle", icon = DrawingIcons.Circle),
    DrawingToolUiItem(id = "ellipse", name = "Ellipse", icon = DrawingIcons.Ellipse),
    DrawingToolUiItem(id = "polyline", name = "Polyline", icon = DrawingIcons.Polyline),
    DrawingToolUiItem(id = "triangle", name = "Triangle", icon = DrawingIcons.Triangle),
    DrawingToolUiItem(id = "arc", name = "Arc", icon = DrawingIcons.Arc),
    DrawingToolUiItem(id = "curve", name = "Curve", icon = DrawingIcons.Curve),
    DrawingToolUiItem(id = "double_curve", name = "Double Curve", icon = DrawingIcons.DoubleCurve)
)

@Composable
fun GeometricShapesDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = geometricShapeTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
