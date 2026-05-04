package com.asc.markets.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.trading.app.models.OHLCData
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.enums.PriceAxisPosition
import com.tradingview.lightweightcharts.api.options.models.AxisPressedMouseMoveOptions
import com.tradingview.lightweightcharts.api.options.models.BaselineSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.GridLineOptions
import com.tradingview.lightweightcharts.api.options.models.GridOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScaleOptions
import com.tradingview.lightweightcharts.api.options.models.HandleScrollOptions
import com.tradingview.lightweightcharts.api.options.models.KineticScrollOptions
import com.tradingview.lightweightcharts.api.options.models.LayoutOptions
import com.tradingview.lightweightcharts.api.options.models.PriceScaleMargins
import com.tradingview.lightweightcharts.api.options.models.PriceScaleOptions
import com.tradingview.lightweightcharts.api.options.models.TimeScaleOptions
import com.tradingview.lightweightcharts.api.series.enums.LineStyle
import com.tradingview.lightweightcharts.api.series.enums.LineWidth
import com.tradingview.lightweightcharts.api.series.enums.SeriesMarkerPosition
import com.tradingview.lightweightcharts.api.series.enums.SeriesMarkerShape
import com.tradingview.lightweightcharts.api.series.models.BaseValuePrice
import com.tradingview.lightweightcharts.api.series.models.BaseValuePriceType
import com.tradingview.lightweightcharts.api.series.models.BaselineData
import com.tradingview.lightweightcharts.api.series.models.SeriesMarker
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.view.ChartsView
import kotlin.math.roundToInt

private val OrderFlowPositive = Color(0xFF68BFAF)
private val OrderFlowNegative = Color(0xFFEE6A78)

@Composable
fun OrderFlowMiniChart(
    candles: List<OHLCData>,
    priceScaleMarginTop: Float = 0.02f,
    priceScaleMarginBottom: Float = 0.02f,
    modifier: Modifier = Modifier
) {
    val chartData = remember(candles) { candles.map(OHLCData::toBaselineData) }
    val baseValue = remember(candles) { candles.firstOrNull()?.close?.roundToInt() ?: 0 }
    val positiveColor = remember { IntColor(OrderFlowPositive.toArgb()) }
    val negativeColor = remember { IntColor(OrderFlowNegative.toArgb()) }
    var seriesApi by remember { mutableStateOf<SeriesApi?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            seriesApi = null
        }
    }

    AndroidView(
        factory = { context ->
            ChartsView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setOnTouchListener { _, _ -> true }

                api.applyOptions {
                    layout = LayoutOptions(
                        background = SolidColor(IntColor(AndroidColor.TRANSPARENT)),
                        textColor = IntColor(Color.White.copy(alpha = 0.78f).toArgb()),
                        fontSize = 9
                    )
                    grid = GridOptions(
                        vertLines = GridLineOptions(visible = false),
                        horzLines = GridLineOptions(visible = false)
                    )
                    rightPriceScale = PriceScaleOptions(
                        autoScale = true,
                        visible = false,
                        borderVisible = false,
                        position = PriceAxisPosition.RIGHT
                    )
                    leftPriceScale = PriceScaleOptions(
                        autoScale = true,
                        visible = false,
                        borderVisible = false,
                        position = PriceAxisPosition.LEFT
                    )
                    timeScale = TimeScaleOptions(
                        rightOffset = 0f,
                        barSpacing = 2.1f,
                        minBarSpacing = 0.6f,
                        fixLeftEdge = true,
                        fixRightEdge = true,
                        lockVisibleTimeRangeOnResize = true,
                        borderVisible = false,
                        visible = true,
                        timeVisible = true,
                        secondsVisible = false,
                        shiftVisibleRangeOnNewBar = true,
                        ticksVisible = true
                    )
                    handleScroll = HandleScrollOptions(
                        pressedMouseMove = false,
                        horzTouchDrag = false,
                        vertTouchDrag = false
                    )
                    handleScale = HandleScaleOptions(
                        mouseWheel = false,
                        pinch = false,
                        axisPressedMouseMove = AxisPressedMouseMoveOptions(
                            time = false,
                            price = false
                        )
                    )
                    kineticScroll = KineticScrollOptions(
                        touch = false,
                        mouse = false
                    )
                }

                api.addBaselineSeries(
                    options = baselineOptions(
                        baseValue = baseValue,
                        positiveColor = positiveColor,
                        negativeColor = negativeColor
                    ),
                    onSeriesCreated = { createdSeries ->
                        seriesApi = createdSeries
                        createdSeries.priceScale().applyOptions(
                            PriceScaleOptions(
                                autoScale = true,
                                visible = false,
                                borderVisible = false,
                                scaleMargins = PriceScaleMargins(
                                    top = priceScaleMarginTop,
                                    bottom = priceScaleMarginBottom
                                )
                            )
                        )
                        if (chartData.isNotEmpty()) {
                            createdSeries.setData(chartData)
                            createdSeries.setMarkers(buildLastMarker(candles, baseValue, positiveColor, negativeColor))
                            api.timeScale.fitContent()
                        }
                    }
                )
            }
        },
        update = { chartView ->
            seriesApi?.applyOptions(
                baselineOptions(
                    baseValue = baseValue,
                    positiveColor = positiveColor,
                    negativeColor = negativeColor
                )
            )
            seriesApi?.priceScale()?.applyOptions(
                PriceScaleOptions(
                    autoScale = true,
                    visible = false,
                    borderVisible = false,
                    scaleMargins = PriceScaleMargins(
                        top = priceScaleMarginTop,
                        bottom = priceScaleMarginBottom
                    )
                )
            )
            if (chartData.isEmpty()) {
                seriesApi?.setData(emptyList<BaselineData>())
                seriesApi?.setMarkers(emptyList())
            } else {
                seriesApi?.setData(chartData)
                seriesApi?.setMarkers(buildLastMarker(candles, baseValue, positiveColor, negativeColor))
                chartView.api.timeScale.applyOptions(
                    TimeScaleOptions(
                        rightOffset = 0f,
                        barSpacing = 2.1f,
                        minBarSpacing = 0.6f,
                        fixLeftEdge = true,
                        fixRightEdge = true,
                        lockVisibleTimeRangeOnResize = true,
                        borderVisible = false,
                        visible = true,
                        timeVisible = true,
                        secondsVisible = false,
                        shiftVisibleRangeOnNewBar = true,
                        ticksVisible = true
                    )
                )
                chartView.api.timeScale.fitContent()
            }
        },
        modifier = modifier
    )
}

private fun baselineOptions(
    baseValue: Int,
    positiveColor: IntColor,
    negativeColor: IntColor
): BaselineSeriesOptions {
    return BaselineSeriesOptions(
        baseValue = BaseValuePrice(baseValue, BaseValuePriceType.PRICE),
        baseLineVisible = false,
        baseLineColor = IntColor(AndroidColor.TRANSPARENT),
        topLineColor = positiveColor,
        topFillColor1 = IntColor(OrderFlowPositive.copy(alpha = 0.28f).toArgb()),
        topFillColor2 = IntColor(OrderFlowPositive.copy(alpha = 0.05f).toArgb()),
        bottomLineColor = negativeColor,
        bottomFillColor1 = IntColor(OrderFlowNegative.copy(alpha = 0.26f).toArgb()),
        bottomFillColor2 = IntColor(OrderFlowNegative.copy(alpha = 0.06f).toArgb()),
        lineWidth = LineWidth.THREE,
        lineStyle = LineStyle.SOLID,
        priceLineVisible = false,
        lastValueVisible = false,
        crosshairMarkerVisible = false
    )
}

private fun buildLastMarker(
    candles: List<OHLCData>,
    baseValue: Int,
    positiveColor: IntColor,
    negativeColor: IntColor
): List<SeriesMarker> {
    val lastCandle = candles.lastOrNull() ?: return emptyList()
    return listOf(
        SeriesMarker(
            time = Time.Utc(lastCandle.time),
            position = SeriesMarkerPosition.IN_BAR,
            shape = SeriesMarkerShape.CIRCLE,
            size = 2,
            color = if (lastCandle.close >= baseValue.toFloat()) positiveColor else negativeColor
        )
    )
}

private fun OHLCData.toBaselineData(): BaselineData {
    return BaselineData(
        time = Time.Utc(time),
        value = close
    )
}
