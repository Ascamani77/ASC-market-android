package com.trading.app.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object DrawingIcons {
    private fun PathBuilder.drawCircle(x: Float, y: Float, radius: Float) {
        moveTo(x + radius, y)
        arcTo(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, x - radius, y)
        arcTo(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, x + radius, y)
    }

    private fun PathBuilder.addOval(x: Float, y: Float, w: Float, h: Float) {
        moveTo(x + w, y + h / 2)
        arcTo(w / 2, h / 2, 0f, isMoreThanHalf = true, isPositiveArc = true, x, y + h / 2)
        arcTo(w / 2, h / 2, 0f, isMoreThanHalf = true, isPositiveArc = true, x + w, y + h / 2)
    }

    val TrendLine = ImageVector.Builder(
        name = "TrendLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 19f)
            lineTo(19f, 5f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 19f, 2.2f)
            drawCircle(19f, 5f, 2.2f)
        }
    }.build()

    val Ray = ImageVector.Builder(
        name = "Ray",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 19f)
            lineTo(22f, 2f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 19f, 2.2f)
            drawCircle(14f, 10f, 2.2f)
        }
    }.build()

    val InfoLine = ImageVector.Builder(
        name = "InfoLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 19f)
            lineTo(19f, 5f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 19f, 2.2f)
            drawCircle(19f, 5f, 2.2f)
            moveTo(15f, 15f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(15f)
            close()
        }
    }.build()

    val ExtendedLine = ImageVector.Builder(
        name = "ExtendedLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2f, 22f)
            lineTo(22f, 2f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(8f, 16f, 2.2f)
            drawCircle(16f, 8f, 2.2f)
        }
    }.build()

    val TrendAngle = ImageVector.Builder(
        name = "TrendAngle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 19f)
            lineTo(20f, 19f)
            moveTo(5f, 19f)
            lineTo(18f, 6f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 19f, 2.2f)
            drawCircle(15f, 9f, 2.2f)
        }
    }.build()

    val HorizontalLine = ImageVector.Builder(
        name = "HorizontalLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2f, 12f)
            lineTo(22f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 2.2f)
        }
    }.build()

    val HorizontalRay = ImageVector.Builder(
        name = "HorizontalRay",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 12f)
            lineTo(22f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 12f, 2.2f)
        }
    }.build()

    val VerticalLine = ImageVector.Builder(
        name = "VerticalLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 2f)
            lineTo(12f, 22f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 2.2f)
        }
    }.build()

    val CrossLine = ImageVector.Builder(
        name = "CrossLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 2f)
            lineTo(12f, 22f)
            moveTo(2f, 12f)
            lineTo(22f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 2.2f)
        }
    }.build()

    val ParallelChannel = ImageVector.Builder(
        name = "ParallelChannel",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 15f)
            lineTo(19f, 5f)
            moveTo(5f, 19f)
            lineTo(19f, 9f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 15f, 1.8f)
            drawCircle(19f, 5f, 1.8f)
            drawCircle(5f, 19f, 1.8f)
        }
    }.build()

    val RegressionTrend = ImageVector.Builder(
        name = "RegressionTrend",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(20f, 6f)
            moveTo(4f, 14f)
            lineTo(20f, 2f)
            moveTo(4f, 22f)
            lineTo(20f, 10f)
        }
    }.build()

    val FlatTopBottom = ImageVector.Builder(
        name = "FlatTopBottom",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 6f)
            lineTo(20f, 12f)
            moveTo(4f, 18f)
            lineTo(20f, 12f)
        }
    }.build()

    val Pitchfork = ImageVector.Builder(
        name = "Pitchfork",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(3f, 12f)
            lineTo(10f, 12f)
            lineTo(21f, 5f)
            moveTo(10f, 12f)
            lineTo(21f, 12f)
            moveTo(10f, 12f)
            lineTo(21f, 19f)
            moveTo(21f, 5f)
            lineTo(21f, 19f)
        }
    }.build()

    // Geometric Shapes
    val Brush = ImageVector.Builder(
        name = "Brush",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 21f)
            curveTo(7f, 21f, 10f, 18f, 11f, 17f)
            curveTo(12f, 16f, 13f, 15f, 15f, 13f)
            curveTo(17f, 11f, 19f, 9f, 20f, 8f)
            curveTo(21f, 7f, 21f, 6f, 20f, 5f)
            curveTo(19f, 4f, 18f, 4f, 17f, 5f)
            curveTo(16f, 6f, 14f, 8f, 12f, 10f)
            curveTo(10f, 12f, 8f, 14f, 7f, 15f)
            curveTo(6f, 16f, 3f, 19f, 3f, 19f)
            lineTo(7f, 21f)
            close()
        }
    }.build()

    val Highlighter = ImageVector.Builder(
        name = "Highlighter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(15.24f, 3.76f)
            lineTo(18.36f, 6.88f)
            lineTo(10.59f, 14.65f)
            lineTo(5.64f, 9.71f)
            lineTo(15.24f, 3.76f)
            close()
            moveTo(5f, 10f)
            lineTo(10f, 15f)
            lineTo(8f, 17f)
            horizontalLineTo(4f)
            verticalLineTo(13f)
            lineTo(5f, 10f)
            close()
        }
    }.build()

    val ArrowMarker = ImageVector.Builder(
        name = "ArrowMarker",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 20f)
            lineTo(20f, 4f)
            moveTo(20f, 4f)
            horizontalLineTo(14f)
            moveTo(20f, 4f)
            verticalLineTo(10f)
        }
    }.build()

    val Arrow = ImageVector.Builder(
        name = "Arrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 19f)
            lineTo(19f, 5f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 19f, 2.2f)
            moveTo(19f, 5f)
            lineTo(14f, 5f)
            lineTo(19f, 10f)
            close()
        }
    }.build()

    val ArrowMarkerUp = ImageVector.Builder(
        name = "ArrowMarkerUp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 20f)
            verticalLineTo(4f)
            moveTo(12f, 4f)
            lineTo(6f, 10f)
            moveTo(12f, 4f)
            lineTo(18f, 10f)
        }
    }.build()

    val ArrowMarkerDown = ImageVector.Builder(
        name = "ArrowMarkerDown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            verticalLineTo(20f)
            moveTo(12f, 20f)
            lineTo(6f, 14f)
            moveTo(12f, 20f)
            lineTo(18f, 14f)
        }
    }.build()

    val Rectangle = ImageVector.Builder(
        name = "Rectangle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(5f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(5f, 5f, 1.8f)
            drawCircle(19f, 5f, 1.8f)
            drawCircle(19f, 19f, 1.8f)
            drawCircle(5f, 19f, 1.8f)
        }
    }.build()

    val RotatedRectangle = ImageVector.Builder(
        name = "RotatedRectangle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(8f, 4f)
            lineTo(20f, 10f)
            lineTo(16f, 20f)
            lineTo(4f, 14f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(8f, 4f, 1.8f)
            drawCircle(20f, 10f, 1.8f)
            drawCircle(16f, 20f, 1.8f)
        }
    }.build()

    val Path = ImageVector.Builder(
        name = "Path",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(12f, 8f)
            lineTo(20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 18f, 1.8f)
            drawCircle(12f, 8f, 1.8f)
            moveTo(20f, 12f)
            lineTo(16f, 10f)
            lineTo(18f, 16f)
            close()
        }
    }.build()

    val Circle = ImageVector.Builder(
        name = "Circle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            drawCircle(12f, 12f, 8f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 1.8f)
            drawCircle(20f, 12f, 1.8f)
        }
    }.build()

    val Ellipse = ImageVector.Builder(
        name = "Ellipse",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            addOval(4f, 8f, 16f, 8f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 1.8f)
            drawCircle(20f, 12f, 1.8f)
            drawCircle(12f, 8f, 1.8f)
            drawCircle(12f, 16f, 1.8f)
        }
    }.build()

    val Polyline = ImageVector.Builder(
        name = "Polyline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 16f)
            lineTo(8f, 8f)
            lineTo(16f, 16f)
            lineTo(20f, 8f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 16f, 1.8f)
            drawCircle(8f, 8f, 1.8f)
            drawCircle(16f, 16f, 1.8f)
            drawCircle(20f, 8f, 1.8f)
        }
    }.build()

    val Triangle = ImageVector.Builder(
        name = "Triangle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 5f)
            lineTo(19f, 18f)
            lineTo(5f, 18f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 5f, 1.8f)
            drawCircle(19f, 18f, 1.8f)
            drawCircle(5f, 18f, 1.8f)
        }
    }.build()

    val Arc = ImageVector.Builder(
        name = "Arc",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 18f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(6f, 18f, 1.8f)
            drawCircle(18f, 18f, 1.8f)
            drawCircle(12f, 8f, 1.8f)
        }
    }.build()

    val Curve = ImageVector.Builder(
        name = "Curve",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            quadTo(12f, 2f, 20f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 18f, 1.8f)
            drawCircle(12f, 10f, 1.8f)
            drawCircle(20f, 18f, 1.8f)
        }
    }.build()

    val DoubleCurve = ImageVector.Builder(
        name = "DoubleCurve",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            curveTo(8f, 4f, 16f, 20f, 20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 1.8f)
            drawCircle(10f, 9f, 1.8f)
            drawCircle(14f, 15f, 1.8f)
            drawCircle(20f, 12f, 1.8f)
        }
    }.build()

    // Gann and Fibonacci
    val FibRetracement = ImageVector.Builder(
        name = "FibRetracement",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            lineTo(18f, 6f)
            moveTo(6f, 10f)
            lineTo(18f, 10f)
            moveTo(6f, 14f)
            lineTo(18f, 14f)
            moveTo(6f, 18f)
            lineTo(18f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 6f, 1.5f)
            drawCircle(12f, 18f, 1.5f)
        }
    }.build()

    val TrendBasedFibExtension = ImageVector.Builder(
        name = "TrendBasedFibExtension",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(10f, 10f)
            lineTo(16f, 18f)
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            moveTo(4f, 10f)
            lineTo(20f, 10f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 18f, 1.5f)
            drawCircle(10f, 10f, 1.5f)
            drawCircle(16f, 18f, 1.5f)
        }
    }.build()

    val FibChannel = ImageVector.Builder(
        name = "FibChannel",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            lineTo(16f, 4f)
            moveTo(8f, 16f)
            lineTo(20f, 8f)
            moveTo(12f, 20f)
            lineTo(22f, 14f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 1.5f)
            drawCircle(16f, 4f, 1.5f)
            drawCircle(8f, 16f, 1.5f)
        }
    }.build()

    val FibTimeZone = ImageVector.Builder(
        name = "FibTimeZone",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 4f)
            lineTo(6f, 20f)
            moveTo(10f, 4f)
            lineTo(10f, 20f)
            moveTo(14f, 4f)
            lineTo(14f, 20f)
            moveTo(18f, 4f)
            lineTo(18f, 20f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(6f, 12f, 1.5f)
            drawCircle(10f, 12f, 1.5f)
        }
    }.build()

    val FibSpeedResistanceFan = ImageVector.Builder(
        name = "FibSpeedResistanceFan",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 20f)
            lineTo(20f, 4f)
            moveTo(4f, 20f)
            lineTo(20f, 12f)
            moveTo(4f, 20f)
            lineTo(12f, 4f)
            moveTo(20f, 4f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 20f, 1.5f)
            drawCircle(20f, 4f, 1.5f)
        }
    }.build()

    val TrendBasedFibTime = ImageVector.Builder(
        name = "TrendBasedFibTime",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(10f, 10f)
            lineTo(16f, 18f)
            moveTo(10f, 4f)
            lineTo(10f, 20f)
            moveTo(16f, 4f)
            lineTo(16f, 20f)
            moveTo(20f, 4f)
            lineTo(20f, 20f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 18f, 1.5f)
            drawCircle(10f, 10f, 1.5f)
            drawCircle(16f, 18f, 1.5f)
        }
    }.build()

    val FibCircles = ImageVector.Builder(
        name = "FibCircles",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            drawCircle(12f, 12f, 3f)
            drawCircle(12f, 12f, 6f)
            drawCircle(12f, 12f, 9f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 1.5f)
            drawCircle(21f, 12f, 1.5f)
        }
    }.build()

    val FibSpiral = ImageVector.Builder(
        name = "FibSpiral",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 12f)
            arcTo(2f, 2f, 0f, false, true, 14f, 14f)
            arcTo(4f, 4f, 0f, false, true, 10f, 18f)
            arcTo(8f, 8f, 0f, false, true, 18f, 10f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 1.5f)
            drawCircle(18f, 10f, 1.5f)
        }
    }.build()

    val FibSpeedResistanceArcs = ImageVector.Builder(
        name = "FibSpeedResistanceArcs",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            arcTo(8f, 8f, 0f, false, true, 20f, 12f)
            moveTo(8f, 12f)
            arcTo(4f, 4f, 0f, false, true, 16f, 12f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 1.5f)
            drawCircle(12f, 4f, 1.5f)
        }
    }.build()

    val FibWedge = ImageVector.Builder(
        name = "FibWedge",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            arcTo(14f, 14f, 0f, false, true, 18f, 4f)
            moveTo(4f, 18f)
            lineTo(18f, 4f)
            moveTo(8f, 18f)
            arcTo(10f, 10f, 0f, false, true, 18f, 8f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 18f, 1.5f)
            drawCircle(18f, 4f, 1.5f)
            drawCircle(11f, 11f, 1.5f)
        }
    }.build()

    val GannBox = ImageVector.Builder(
        name = "GannBox",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 4f)
            lineTo(20f, 20f)
            moveTo(4f, 20f)
            lineTo(20f, 4f)
            moveTo(12f, 4f)
            lineTo(12f, 20f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 4f, 1.5f)
            drawCircle(20f, 20f, 1.5f)
        }
    }.build()

    val GannSquareFixed = ImageVector.Builder(
        name = "GannSquareFixed",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            horizontalLineTo(18f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            close()
            moveTo(6f, 10f)
            horizontalLineTo(18f)
            moveTo(6f, 14f)
            horizontalLineTo(18f)
            moveTo(10f, 6f)
            lineTo(10f, 18f)
            moveTo(14f, 6f)
            lineTo(14f, 18f)
        }
    }.build()

    val GannSquare = ImageVector.Builder(
        name = "GannSquare",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 4f)
            lineTo(20f, 20f)
            moveTo(12f, 4f)
            lineTo(12f, 20f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 4f, 1.5f)
            drawCircle(20f, 20f, 1.5f)
        }
    }.build()

    val GannFan = ImageVector.Builder(
        name = "GannFan",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 20f)
            lineTo(12f, 4f)
            moveTo(4f, 20f)
            lineTo(18f, 8f)
            moveTo(4f, 20f)
            lineTo(20f, 14f)
            moveTo(4f, 20f)
            lineTo(20f, 20f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 20f, 1.5f)
            drawCircle(12f, 4f, 1.5f)
        }
    }.build()

    // Patterns
    val XABCDPattern = ImageVector.Builder(
        name = "XABCDPattern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(8f, 6f)
            lineTo(12f, 14f)
            lineTo(16f, 6f)
            lineTo(20f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 18f, 1.5f)
            drawCircle(8f, 6f, 1.5f)
            drawCircle(12f, 14f, 1.5f)
            drawCircle(16f, 6f, 1.5f)
            drawCircle(20f, 18f, 1.5f)
        }
    }.build()

    val CypherPattern = ImageVector.Builder(
        name = "CypherPattern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 14f)
            lineTo(8f, 6f)
            lineTo(12f, 18f)
            lineTo(16f, 10f)
            lineTo(20f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 14f, 1.5f)
            drawCircle(8f, 6f, 1.5f)
            drawCircle(12f, 18f, 1.5f)
            drawCircle(16f, 10f, 1.5f)
            drawCircle(20f, 18f, 1.5f)
        }
    }.build()

    val HeadAndShoulders = ImageVector.Builder(
        name = "HeadAndShoulders",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 14f)
            lineTo(7f, 8f)
            lineTo(10f, 14f)
            lineTo(13f, 4f)
            lineTo(16f, 14f)
            lineTo(19f, 8f)
            lineTo(22f, 14f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(7f, 8f, 1.5f)
            drawCircle(13f, 4f, 1.5f)
            drawCircle(19f, 8f, 1.5f)
        }
    }.build()

    val ABCDPattern = ImageVector.Builder(
        name = "ABCDPattern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 18f)
            lineTo(10f, 6f)
            lineTo(14f, 18f)
            lineTo(18f, 6f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(6f, 18f, 1.5f)
            drawCircle(10f, 6f, 1.5f)
            drawCircle(14f, 18f, 1.5f)
            drawCircle(18f, 6f, 1.5f)
        }
    }.build()

    val TrianglePattern = ImageVector.Builder(
        name = "TrianglePattern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 6f)
            lineTo(20f, 12f)
            lineTo(4f, 18f)
            close()
            moveTo(4f, 12f)
            lineTo(12f, 9f)
            moveTo(12f, 15f)
            lineTo(4f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 6f, 1.5f)
            drawCircle(20f, 12f, 1.5f)
            drawCircle(4f, 18f, 1.5f)
        }
    }.build()

    val ThreeDrivesPattern = ImageVector.Builder(
        name = "ThreeDrivesPattern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(8f, 10f)
            lineTo(10f, 16f)
            lineTo(14f, 8f)
            lineTo(16f, 14f)
            lineTo(20f, 6f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(8f, 10f, 1.5f)
            drawCircle(14f, 8f, 1.5f)
            drawCircle(20f, 6f, 1.5f)
        }
    }.build()

    val ElliottImpulseWave = ImageVector.Builder(
        name = "ElliottImpulseWave",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(7f, 12f)
            lineTo(10f, 16f)
            lineTo(13f, 6f)
            lineTo(16f, 10f)
            lineTo(19f, 4f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(7f, 12f, 1.5f)
            drawCircle(13f, 6f, 1.5f)
            drawCircle(19f, 4f, 1.5f)
        }
    }.build()

    val ElliottCorrectionWave = ImageVector.Builder(
        name = "ElliottCorrectionWave",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            lineTo(12f, 18f)
            lineTo(18f, 6f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(6f, 6f, 1.5f)
            drawCircle(12f, 18f, 1.5f)
            drawCircle(18f, 6f, 1.5f)
        }
    }.build()

    val ElliottTriangleWave = ImageVector.Builder(
        name = "ElliottTriangleWave",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 8f)
            lineTo(20f, 12f)
            lineTo(6f, 16f)
            lineTo(16f, 10f)
            lineTo(8f, 14f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 8f, 1.5f)
            drawCircle(20f, 12f, 1.5f)
        }
    }.build()

    val ElliottDoubleComboWave = ImageVector.Builder(
        name = "ElliottDoubleComboWave",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 10f)
            lineTo(8f, 18f)
            lineTo(12f, 12f)
            lineTo(16f, 20f)
            lineTo(20f, 14f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(8f, 18f, 1.5f)
            drawCircle(16f, 20f, 1.5f)
        }
    }.build()

    val ElliottTripleComboWave = ImageVector.Builder(
        name = "ElliottTripleComboWave",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 10f)
            lineTo(7f, 18f)
            lineTo(10f, 14f)
            lineTo(13f, 20f)
            lineTo(16f, 16f)
            lineTo(19f, 22f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(7f, 18f, 1.5f)
            drawCircle(13f, 20f, 1.5f)
            drawCircle(19f, 22f, 1.5f)
        }
    }.build()

    val CyclicLines = ImageVector.Builder(
        name = "CyclicLines",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            lineTo(6f, 18f)
            moveTo(12f, 6f)
            lineTo(12f, 18f)
            moveTo(18f, 6f)
            lineTo(18f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(6f, 12f, 1.5f)
            drawCircle(12f, 12f, 1.5f)
            drawCircle(18f, 12f, 1.5f)
        }
    }.build()

    val TimeCycles = ImageVector.Builder(
        name = "TimeCycles",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            arcTo(4f, 4f, 0f, false, true, 12f, 12f)
            arcTo(4f, 4f, 0f, false, true, 20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 1.5f)
            drawCircle(12f, 12f, 1.5f)
            drawCircle(20f, 12f, 1.5f)
        }
    }.build()

    val SineLine = ImageVector.Builder(
        name = "SineLine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            quadTo(8f, 4f, 12f, 12f)
            quadTo(16f, 20f, 20f, 12f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 1.5f)
            drawCircle(12f, 12f, 1.5f)
            drawCircle(20f, 12f, 1.5f)
        }
    }.build()

    // Tools Page Icons
    val Measure = ImageVector.Builder(
        name = "Measure",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 21f)
            lineTo(21f, 7f)
            moveTo(8f, 18f)
            lineTo(10f, 20f)
            moveTo(11f, 15f)
            lineTo(13f, 17f)
            moveTo(14f, 12f)
            lineTo(16f, 14f)
            moveTo(17f, 9f)
            lineTo(19f, 11f)
        }
    }.build()

    val Eraser = ImageVector.Builder(
        name = "Eraser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16.24f, 7.76f)
            lineTo(18.36f, 9.88f)
            lineTo(10.59f, 17.65f)
            lineTo(5.64f, 12.71f)
            lineTo(16.24f, 7.76f)
            close()
            moveTo(7.05f, 14.12f)
            lineTo(14.12f, 14.12f)
        }
    }.build()

    val KeepDrawing = ImageVector.Builder(
        name = "KeepDrawing",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(3f, 17.25f)
            verticalLineTo(21f)
            horizontalLineTo(6.75f)
            lineTo(17.81f, 9.94f)
            lineTo(14.06f, 6.19f)
            lineTo(3f, 17.25f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 15f)
            verticalLineTo(17f)
            horizontalLineTo(17f)
            verticalLineTo(21f)
            horizontalLineTo(22f)
            verticalLineTo(17f)
            horizontalLineTo(21f)
            verticalLineTo(15f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 15f)
            close()
            moveTo(18.5f, 15f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20.5f, 15f)
            verticalLineTo(17f)
            horizontalLineTo(18.5f)
            verticalLineTo(15f)
            close()
        }
    }.build()

    val HideDrawings = ImageVector.Builder(
        name = "HideDrawings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 5f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 19f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 5f)
            close()
            moveTo(3f, 3f)
            lineTo(21f, 21f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(12f, 12f, 3f)
        }
    }.build()

    val LockAllDrawings = ImageVector.Builder(
        name = "LockAllDrawings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 10f)
            verticalLineTo(20f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 22f)
            horizontalLineTo(18f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20f, 20f)
            verticalLineTo(10f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 8f)
            close()
            moveTo(9f, 6f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(9f)
            verticalLineTo(6f)
            close()
        }
    }.build()

    val Magnet = ImageVector.Builder(
        name = "Magnet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 3f)
            verticalLineTo(13f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17f, 13f)
            verticalLineTo(3f)
            moveTo(7f, 3f)
            horizontalLineTo(11f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            close()
            moveTo(13f, 3f)
            horizontalLineTo(17f)
            verticalLineTo(7f)
            horizontalLineTo(13f)
            close()
        }
    }.build()

    val RemoveAll = ImageVector.Builder(
        name = "RemoveAll",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(3f, 6f)
            horizontalLineTo(21f)
            moveTo(19f, 6f)
            verticalLineTo(20f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 22f)
            horizontalLineTo(7f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 20f)
            verticalLineTo(6f)
            moveTo(8f, 6f)
            verticalLineTo(4f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10f, 2f)
            horizontalLineTo(14f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 4f)
            verticalLineTo(6f)
        }
    }.build()

    val Pitchfan = ImageVector.Builder(
        name = "Pitchfan",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            lineTo(20f, 6f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 12f)
            lineTo(20f, 18f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 1.5f)
            drawCircle(14f, 8.25f, 1.5f)
            drawCircle(14f, 15.75f, 1.5f)
        }
    }.build()

    val ZoomIn = ImageVector.Builder(
        name = "ZoomIn",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            drawCircle(10f, 10f, 7f)
            moveTo(21f, 21f)
            lineTo(15f, 15f)
            moveTo(10f, 7f)
            verticalLineTo(13f)
            moveTo(7f, 10f)
            horizontalLineTo(13f)
        }
    }.build()

    val ZoomOut = ImageVector.Builder(
        name = "ZoomOut",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            drawCircle(10f, 10f, 7f)
            moveTo(21f, 21f)
            lineTo(15f, 15f)
            moveTo(7f, 10f)
            horizontalLineTo(13f)
        }
    }.build()

    val LongPosition = ImageVector.Builder(
        name = "LongPosition",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            verticalLineTo(12f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 12f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(5f)
            close()
            moveTo(12f, 5f)
            verticalLineTo(19f)
        }
    }.build()

    val ShortPosition = ImageVector.Builder(
        name = "ShortPosition",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            verticalLineTo(12f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 12f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(5f)
            close()
            moveTo(12f, 5f)
            verticalLineTo(19f)
        }
    }.build()

    val Forecast = ImageVector.Builder(
        name = "Forecast",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 18f)
            lineTo(10f, 12f)
            lineTo(14f, 15f)
            lineTo(20f, 7f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(20f, 7f, 1.8f)
        }
    }.build()

    val BarsPattern = ImageVector.Builder(
        name = "BarsPattern",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 18f)
            verticalLineTo(10f)
            moveTo(10f, 18f)
            verticalLineTo(6f)
            moveTo(14f, 18f)
            verticalLineTo(12f)
            moveTo(18f, 18f)
            verticalLineTo(8f)
        }
    }.build()

    val GhostFeed = ImageVector.Builder(
        name = "GhostFeed",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 18f)
            verticalLineTo(14f)
            moveTo(10f, 18f)
            verticalLineTo(12f)
            moveTo(14f, 18f)
            verticalLineTo(15f)
            moveTo(18f, 18f)
            verticalLineTo(13f)
        }
    }.build()

    val Projection = ImageVector.Builder(
        name = "Projection",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 16f)
            lineTo(12f, 16f)
            lineTo(20f, 8f)
            moveTo(20f, 8f)
            horizontalLineTo(15f)
            moveTo(20f, 8f)
            verticalLineTo(13f)
        }
    }.build()

    val AnchoredVWAP = ImageVector.Builder(
        name = "AnchoredVWAP",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            curveTo(8f, 12f, 12f, 18f, 20f, 6f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(4f, 12f, 2f)
        }
    }.build()

    val FixedRangeVolume = ImageVector.Builder(
        name = "FixedRangeVolume",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            lineTo(6f, 18f)
            moveTo(18f, 6f)
            lineTo(18f, 18f)
            moveTo(6f, 8f)
            horizontalLineTo(14f)
            moveTo(6f, 11f)
            horizontalLineTo(16f)
            moveTo(6f, 14f)
            horizontalLineTo(12f)
            moveTo(6f, 17f)
            horizontalLineTo(15f)
        }
    }.build()

    val AnchoredVolume = ImageVector.Builder(
        name = "AnchoredVolume",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            lineTo(6f, 18f)
            moveTo(6f, 8f)
            horizontalLineTo(14f)
            moveTo(6f, 11f)
            horizontalLineTo(16f)
            moveTo(6f, 14f)
            horizontalLineTo(12f)
            moveTo(6f, 17f)
            horizontalLineTo(15f)
        }
        path(fill = SolidColor(Color.White)) {
            drawCircle(6f, 6f, 1.5f)
        }
    }.build()

    val PriceRange = ImageVector.Builder(
        name = "PriceRange",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 4f)
            verticalLineTo(20f)
            moveTo(9f, 4f)
            horizontalLineTo(15f)
            moveTo(9f, 20f)
            horizontalLineTo(15f)
            moveTo(12f, 4f)
            lineTo(10f, 7f)
            moveTo(12f, 4f)
            lineTo(14f, 7f)
            moveTo(12f, 20f)
            lineTo(10f, 17f)
            moveTo(12f, 20f)
            lineTo(14f, 17f)
        }
    }.build()

    val DateRange = ImageVector.Builder(
        name = "DateRange",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            horizontalLineTo(20f)
            moveTo(4f, 9f)
            verticalLineTo(15f)
            moveTo(20f, 9f)
            verticalLineTo(15f)
            moveTo(4f, 12f)
            lineTo(7f, 10f)
            moveTo(4f, 12f)
            lineTo(7f, 14f)
            moveTo(20f, 12f)
            lineTo(17f, 10f)
            moveTo(20f, 12f)
            lineTo(17f, 14f)
        }
    }.build()

    val DateAndPriceRange = ImageVector.Builder(
        name = "DateAndPriceRange",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.32f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(6f, 6f)
            horizontalLineTo(18f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            close()
            moveTo(12f, 8f)
            verticalLineTo(16f)
            moveTo(8f, 12f)
            horizontalLineTo(16f)
        }
    }.build()

    val Emojis = ImageVector.Builder(
        name = "Emojis",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            drawCircle(12f, 12f, 9f)
            moveTo(9f, 9f)
            drawCircle(9f, 9f, 0.5f)
            moveTo(15f, 9f)
            drawCircle(15f, 9f, 0.5f)
            moveTo(8f, 14f)
            quadTo(12f, 18f, 16f, 14f)
        }
    }.build()

    val Stickers = ImageVector.Builder(
        name = "Stickers",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            verticalLineTo(14f)
            lineTo(14f, 19f)
            horizontalLineTo(5f)
            close()
            moveTo(14f, 14f)
            verticalLineTo(19f)
            lineTo(19f, 14f)
            close()
        }
    }.build()

    val IconsVisuals = ImageVector.Builder(
        name = "IconsVisuals",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.65f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(7f, 7f)
            horizontalLineTo(10f)
            verticalLineTo(10f)
            horizontalLineTo(7f)
            close()
            moveTo(14f, 7f)
            horizontalLineTo(17f)
            verticalLineTo(10f)
            horizontalLineTo(14f)
            close()
            moveTo(7f, 14f)
            horizontalLineTo(10f)
            verticalLineTo(17f)
            horizontalLineTo(7f)
            close()
            moveTo(14f, 14f)
            horizontalLineTo(17f)
            verticalLineTo(17f)
            horizontalLineTo(14f)
            close()
        }
    }.build()
}

