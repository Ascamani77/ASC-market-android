package com.trading.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

class BoxWithConstraintsScope internal constructor(
    constraints: Constraints,
    density: androidx.compose.ui.unit.Density,
    boxScope: BoxScope
) : BoxScope by boxScope {
    val constraints: Constraints = constraints
    val maxWidth: Dp = with(density) { constraints.maxWidth.toDp() }
    val maxHeight: Dp = with(density) { constraints.maxHeight.toDp() }
    val minWidth: Dp = with(density) { constraints.minWidth.toDp() }
    val minHeight: Dp = with(density) { constraints.minHeight.toDp() }
}

@Composable
fun BoxWithConstraints(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit
) {
    val density = LocalDensity.current
    SubcomposeLayout(modifier = modifier) { subcomposeConstraints ->
        val measurable = subcompose(0) {
            Box {
                val scope = BoxWithConstraintsScope(
                    subcomposeConstraints,
                    density,
                    this@Box
                )
                scope.content()
            }
        }
        val placeables = measurable.map { it.measure(subcomposeConstraints) }
        layout(subcomposeConstraints.maxWidth, subcomposeConstraints.maxHeight) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}
