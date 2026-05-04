package com.trading.app.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.indicators.RsiIndicator
import com.trading.app.models.OHLCData
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.common.PriceLine
import com.tradingview.lightweightcharts.api.options.models.*
import com.tradingview.lightweightcharts.api.series.enums.*
import com.tradingview.lightweightcharts.api.series.models.*
import com.tradingview.lightweightcharts.view.ChartsView
import java.util.Locale

internal const val RSI_SCALE_KEY = "rsi_pane"
private const val RSI_PANE_BACKGROUND_HEX = "#000000"
private const val RSI_PANE_BORDER_HEX = "#363A45"
private const val RSI_MIN = 0f
private const val RSI_MID = 50f
private const val RSI_OVERBOUGHT = 70f
private const val RSI_MAX = 100f
private const val RSI_OVERSOLD = 30f

private fun Long.toChartTime(): Time = Time.Utc(this)

private fun applyOpacity(color: Int, opacity: Int): Int {
    val alpha = (opacity / 100f * 255).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (alpha shl 24)
}

private fun buildRsiData(
    candles: List<OHLCData>,
    rsiValues: List<Float?>
): List<LineData> {
    return rsiValues.mapIndexedNotNull { index, value ->
        value?.let {
            LineData(
                time = candles[index].time.toChartTime(),
                value = it.coerceIn(RSI_MIN, RSI_MAX)
            )
        }
    }
}

private fun buildFlatLineData(
    candles: List<OHLCData>,
    value: Float
): List<LineData> = candles.map { candle ->
    LineData(
        time = candle.time.toChartTime(),
        value = value
    )
}

private fun buildFlatAreaData(
    candles: List<OHLCData>,
    value: Float,
    lineColor: IntColor,
    topColor: IntColor,
    bottomColor: IntColor
): List<AreaData> = candles.map { candle ->
    AreaData(
        lineColor = lineColor,
        topColor = topColor,
        bottomColor = bottomColor,
        time = candle.time.toChartTime(),
        value = value
    )
}

internal data class RsiChartData(
    val values: List<Float?> = emptyList(),
    val movingAverageValues: List<Float?> = emptyList()
) {
    val latestValue: Float?
        get() = values.lastOrNull { it != null }

    val latestMovingAverageValue: Float?
        get() = movingAverageValues.lastOrNull { it != null }
}

@Stable
internal class RsiPaneRefs {
    var paneBackgroundSeriesApi by mutableStateOf<SeriesApi?>(null)
    var bandFillSeriesApi by mutableStateOf<SeriesApi?>(null)
    var bandMaskSeriesApi by mutableStateOf<SeriesApi?>(null)
    var lowerBoundarySeriesApi by mutableStateOf<SeriesApi?>(null)
    var upperBoundarySeriesApi by mutableStateOf<SeriesApi?>(null)
    var upperGuideSeriesApi by mutableStateOf<SeriesApi?>(null)
    var middleGuideSeriesApi by mutableStateOf<SeriesApi?>(null)
    var lowerGuideSeriesApi by mutableStateOf<SeriesApi?>(null)
    var rsiSeriesApi by mutableStateOf<SeriesApi?>(null)
    var movingAverageSeriesApi by mutableStateOf<SeriesApi?>(null)

    var crosshairRsiValue by mutableStateOf<Float?>(null)
    var crosshairMaValue by mutableStateOf<Float?>(null)

    var rsiPriceLine by mutableStateOf<PriceLine?>(null)
    var maPriceLine by mutableStateOf<PriceLine?>(null)

    fun clear() {
        paneBackgroundSeriesApi = null
        bandFillSeriesApi = null
        bandMaskSeriesApi = null
        lowerBoundarySeriesApi = null
        upperBoundarySeriesApi = null
        upperGuideSeriesApi = null
        middleGuideSeriesApi = null
        lowerGuideSeriesApi = null
        rsiSeriesApi = null
        movingAverageSeriesApi = null
        crosshairRsiValue = null
        crosshairMaValue = null
        rsiPriceLine = null
        maPriceLine = null
    }

    fun clearData() {
        paneBackgroundSeriesApi?.setData(emptyList())
        bandFillSeriesApi?.setData(emptyList())
        bandMaskSeriesApi?.setData(emptyList())
        lowerBoundarySeriesApi?.setData(emptyList())
        upperBoundarySeriesApi?.setData(emptyList())
        upperGuideSeriesApi?.setData(emptyList())
        middleGuideSeriesApi?.setData(emptyList())
        lowerGuideSeriesApi?.setData(emptyList())
        rsiSeriesApi?.setData(emptyList())
        movingAverageSeriesApi?.setData(emptyList())
        crosshairRsiValue = null
        crosshairMaValue = null

        rsiSeriesApi?.let { api ->
            rsiPriceLine?.let { api.removePriceLine(it) }
        }
        movingAverageSeriesApi?.let { api ->
            maPriceLine?.let { api.removePriceLine(it) }
        }
        rsiPriceLine = null
        maPriceLine = null
    }

    fun priceScaleOwner(): SeriesApi? {
        return rsiSeriesApi
            ?: movingAverageSeriesApi
            ?: paneBackgroundSeriesApi
            ?: bandFillSeriesApi
            ?: bandMaskSeriesApi
    }

}

@Composable
internal fun rememberRsiPaneRefs(): RsiPaneRefs = remember { RsiPaneRefs() }

internal fun calculateRsiChartData(
    candles: List<OHLCData>,
    enabled: Boolean,
    period: Int
): RsiChartData {
    if (!enabled || candles.size <= period) {
        return RsiChartData()
    }

    val indicator = RsiIndicator(period = period)
    val values = indicator.calculate(candles)
    val movingAverageValues = indicator.calculateMa(values)
    return RsiChartData(
        values = values,
        movingAverageValues = movingAverageValues
    )
}

internal fun createInlineRsiPaneSeries(
    chartsView: ChartsView,
    refs: RsiPaneRefs,
    scaleMargins: PriceScaleMargins? = null,
    borderColor: IntColor? = null,
    visible: Boolean = true
) {
    refs.clear()

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
            lineWidth = LineWidth.ONE,
            lastValueVisible = false,
            priceLineVisible = false,
            priceScaleId = PriceScaleId(RSI_SCALE_KEY),
            crosshairMarkerVisible = false
        ),
        onSeriesCreated = { refs.lowerBoundarySeriesApi = it }
    )

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
            lineWidth = LineWidth.ONE,
            lastValueVisible = false,
            priceLineVisible = false,
            priceScaleId = PriceScaleId(RSI_SCALE_KEY),
            crosshairMarkerVisible = false
        ),
        onSeriesCreated = { refs.upperBoundarySeriesApi = it }
    )

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(applyOpacity(AndroidColor.parseColor("#7E57C2"), 60)),
            lineWidth = LineWidth.ONE,
            lineStyle = LineStyle.SOLID,
            lastValueVisible = false,
            priceLineVisible = false,
            priceScaleId = PriceScaleId(RSI_SCALE_KEY),
            crosshairMarkerVisible = false
        ),
        onSeriesCreated = { refs.upperGuideSeriesApi = it }
    )

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(applyOpacity(AndroidColor.parseColor("#7E57C2"), 35)),
            lineWidth = LineWidth.ONE,
            lineStyle = LineStyle.SOLID,
            lastValueVisible = false,
            priceLineVisible = false,
            priceScaleId = PriceScaleId(RSI_SCALE_KEY),
            crosshairMarkerVisible = false
        ),
        onSeriesCreated = { refs.middleGuideSeriesApi = it }
    )

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(applyOpacity(AndroidColor.parseColor("#7E57C2"), 60)),
            lineWidth = LineWidth.ONE,
            lineStyle = LineStyle.SOLID,
            lastValueVisible = false,
            priceLineVisible = false,
            priceScaleId = PriceScaleId(RSI_SCALE_KEY),
            crosshairMarkerVisible = false
        ),
        onSeriesCreated = { refs.lowerGuideSeriesApi = it }
    )

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(AndroidColor.parseColor("#7E57C2")),
            lineWidth = LineWidth.TWO,
            lineStyle = LineStyle.SOLID,
            lastValueVisible = false,
            priceLineVisible = false,
            priceFormat = PriceFormat.priceFormatBuiltIn(
                type = PriceFormat.Type.PRICE,
                precision = 2,
                minMove = 0.01f
            ),
            priceScaleId = PriceScaleId(RSI_SCALE_KEY)
        ),
        onSeriesCreated = { series ->
            refs.rsiSeriesApi = series
            if (scaleMargins != null && borderColor != null) {
                series.priceScale().applyOptions(
                    PriceScaleOptions(
                        autoScale = true,
                        scaleMargins = scaleMargins,
                        visible = false,
                        borderVisible = false,
                        borderColor = borderColor,
                        entireTextOnly = true,
                        alignLabels = true,
                        ticksVisible = false
                    )
                )
            }
        }
    )

    chartsView.api.addLineSeries(
        options = LineSeriesOptions(
            color = IntColor(AndroidColor.parseColor("#F2C94C")),
            lineWidth = LineWidth.TWO,
            lineStyle = LineStyle.SOLID,
            lastValueVisible = false,
            priceLineVisible = false,
            priceFormat = PriceFormat.priceFormatBuiltIn(
                type = PriceFormat.Type.PRICE,
                precision = 2,
                minMove = 0.01f
            ),
            priceScaleId = PriceScaleId(RSI_SCALE_KEY)
        ),
        onSeriesCreated = { refs.movingAverageSeriesApi = it }
    )
}


internal fun updateInlineRsiPaneData(
    refs: RsiPaneRefs,
    candles: List<OHLCData>,
    data: RsiChartData,
    enabled: Boolean,
    showLabels: Boolean = true,
    showLines: Boolean = false
) {
    if (!enabled || candles.isEmpty()) {
        refs.clearData()
        return
    }

    refs.lowerBoundarySeriesApi?.setData(buildFlatLineData(candles, RSI_MIN))
    refs.upperBoundarySeriesApi?.setData(buildFlatLineData(candles, RSI_MAX))
    refs.upperGuideSeriesApi?.setData(buildFlatLineData(candles, RSI_OVERBOUGHT))
    refs.middleGuideSeriesApi?.setData(buildFlatLineData(candles, RSI_MID))
    refs.lowerGuideSeriesApi?.setData(buildFlatLineData(candles, RSI_OVERSOLD))
    
    val rsiData = buildRsiData(candles, data.values)
    refs.rsiSeriesApi?.setData(rsiData)

    val maData = data.movingAverageValues.mapIndexedNotNull { index, value ->
        value?.let {
            LineData(
                time = candles[index].time.toChartTime(),
                value = it.coerceIn(RSI_MIN, RSI_MAX)
            )
        }
    }
    refs.movingAverageSeriesApi?.setData(maData)

    // Manage Price Lines
    val rsiApi = refs.rsiSeriesApi
    rsiApi?.let { api ->
        refs.rsiPriceLine?.let { api.removePriceLine(it) }
        refs.rsiPriceLine = null
        if (showLabels || showLines) {
            data.latestValue?.let { lastVal ->
                refs.rsiPriceLine = api.createPriceLine(
                    PriceLineOptions(
                        price = lastVal,
                        color = IntColor(ComposeColor(0xFF7E57C2).toArgb()),
                        lineWidth = LineWidth.ONE,
                        lineStyle = LineStyle.DASHED,
                        lineVisible = showLines,
                        axisLabelVisible = showLabels,
                        title = "RSI | ${String.format(Locale.US, "%.2f", lastVal)}"
                    )
                )
            }
        }
    }

    val maApi = refs.movingAverageSeriesApi
    maApi?.let { api ->
        refs.maPriceLine?.let { api.removePriceLine(it) }
        refs.maPriceLine = null
        if (showLabels || showLines) {
            data.latestMovingAverageValue?.let { lastVal ->
                refs.maPriceLine = api.createPriceLine(
                    PriceLineOptions(
                        price = lastVal,
                        color = IntColor(ComposeColor(0xFF2962FF).toArgb()),
                        lineWidth = LineWidth.ONE,
                        lineStyle = LineStyle.DASHED,
                        lineVisible = showLines,
                        axisLabelVisible = showLabels,
                        title = "RSI:MA | ${String.format(Locale.US, "%.2f", lastVal)}"
                    )
                )
            }
        }
    }
}

internal fun applyInlineRsiPaneScale(
    refs: RsiPaneRefs,
    scaleMargins: PriceScaleMargins,
    borderColor: IntColor,
    visible: Boolean
) {
    refs.priceScaleOwner()?.priceScale()?.applyOptions(
        PriceScaleOptions(
            autoScale = true,
            scaleMargins = scaleMargins,
            visible = false,
            borderVisible = false,
            borderColor = borderColor,
            entireTextOnly = true,
            alignLabels = true,
            ticksVisible = false
        )
    )
}

private fun String.toComposeColor(): ComposeColor = try {
    ComposeColor(AndroidColor.parseColor(this))
} catch (_: Exception) {
    ComposeColor(0xFFD1D4DC)
}

private fun rsiAxisOffset(trackHeight: Dp, itemHeight: Dp, value: Float): Dp {
    val clampedValue = value.coerceIn(RSI_MIN, RSI_MAX)
    val centerLine = trackHeight * (1f - (clampedValue / RSI_MAX))
    return (centerLine - itemHeight / 2).coerceIn(0.dp, (trackHeight - itemHeight).coerceAtLeast(0.dp))
}

private fun formatRsiAxisValue(value: Float): String = String.format(Locale.US, "%.2f", value)

@Composable
internal fun BoxScope.RsiPaneOverlay(
    visible: Boolean,
    scaleMargins: PriceScaleMargins,
    data: RsiChartData,
    rsiPeriod: Int,
    scaleTextColor: String,
    scaleBorderColor: String,
    scaleFontSize: Int,
    axisWidthPx: Float,
    crosshairRsiValue: Float? = null,
    crosshairMaValue: Float? = null,
    scalesPlacement: String = "Right"
) {
    if (!visible) return

    val density = LocalDensity.current
    val textColor = remember(scaleTextColor) { scaleTextColor.toComposeColor() }
    val axisTextSize = scaleFontSize.sp
    val axisWidth = with(density) {
        axisWidthPx.takeIf { it > 0f }?.toDp() ?: 56.dp
    }.coerceAtLeast(56.dp)

    val isLeft = scalesPlacement == "Left"

    val displayRsiValue = crosshairRsiValue ?: data.latestValue
    val displayMaValue = crosshairMaValue ?: data.latestMovingAverageValue

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val paneTopPadding = maxHeight * (scaleMargins.top ?: 0f)
        val paneBottomPadding = maxHeight * (scaleMargins.bottom ?: 0f)
        val paneHeight = (maxHeight - paneTopPadding - paneBottomPadding).coerceAtLeast(0.dp)

        if (paneHeight <= 0.dp) {
            return@BoxWithConstraints
        }

        Row(
            modifier = Modifier
                .align(if (isLeft) Alignment.TopEnd else Alignment.TopStart)
                .padding(
                    start = if (isLeft) 0.dp else 12.dp,
                    top = paneTopPadding + 8.dp,
                    end = if (isLeft) 12.dp else axisWidth + 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RSI $rsiPeriod close",
                color = textColor,
                fontSize = axisTextSize
            )
            displayRsiValue?.let { valToDraw ->
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatRsiAxisValue(valToDraw),
                    color = ComposeColor(0xFF7E57C2),
                    fontSize = axisTextSize,
                    fontWeight = FontWeight.Bold
                )
            }
            displayMaValue?.let { valToDraw ->
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatRsiAxisValue(valToDraw),
                    color = ComposeColor(0xFFF2C94C),
                    fontSize = axisTextSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }

    }
}
