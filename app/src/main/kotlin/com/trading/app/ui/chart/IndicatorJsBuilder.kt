package com.trading.app.ui.chart

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndicatorJsBuilder @Inject constructor() {

    fun buildFullChartHtml(): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <script src="https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        body { margin: 0; padding: 0; background-color: #0d0f1a; overflow: hidden; }
        #chart { width: 100vw; height: 100vh; }
    </style>
</head>
<body>
    <div id="chart"></div>
    <script>
        const container = document.getElementById('chart');
        const chart = LightweightCharts.createChart(container, {
            layout: {
                background: { type: 'solid', color: '#0d0f1a' },
                textColor: '#d1d4dc',
            },
            grid: {
                vertLines: { color: '#1e222d' },
                horzLines: { color: '#1e222d' },
            },
            rightPriceScale: {
                borderColor: '#2b2b43',
            },
            timeScale: {
                borderColor: '#2b2b43',
                timeVisible: true,
                secondsVisible: false,
            },
            crosshair: {
                mode: LightweightCharts.CrosshairMode.Normal,
            },
        });

        // 1. Candlestick Series (Main Pane)
        const candleSeries = chart.addCandlestickSeries({
            upColor: '#089981',
            downColor: '#f23645',
            borderVisible: false,
            wickUpColor: '#089981',
            wickDownColor: '#f23645',
            pane: 0,
        });

        // 2. RSI Series (Lower Pane)
        const rsiSeries = chart.addLineSeries({
            color: '#7e57c2',
            lineWidth: 2,
            pane: 1, // This puts it in a separate pane below
            priceFormat: {
                type: 'price',
                precision: 2,
            },
        });

        // Add 30/70 Levels to RSI
        [30, 70].forEach(level => {
            rsiSeries.createPriceLine({
                price: level,
                color: 'rgba(126, 87, 194, 0.4)',
                lineWidth: 1,
                lineStyle: LightweightCharts.LineStyle.Dashed,
                axisLabelVisible: true,
            });
        });

        // Adjust Pane Proportions (Price 80%, RSI 20%)
        chart.priceScale('right').applyOptions({
            scaleMargins: {
                top: 0.1,
                bottom: 0.1,
            },
        });

        // RSI Calculation Logic
        function calculateRSI(data, period = 14) {
            let results = [];
            let avgGain = 0;
            let avgLoss = 0;

            for (let i = 1; i < data.length; i++) {
                const change = data[i].close - data[i-1].close;
                const gain = change > 0 ? change : 0;
                const loss = change < 0 ? -change : 0;

                if (i <= period) {
                    avgGain += gain / period;
                    avgLoss += loss / period;
                    if (i === period) {
                        results.push({ time: data[i].time, value: 100 - (100 / (1 + avgGain / avgLoss)) });
                    }
                } else {
                    avgGain = (avgGain * (period - 1) + gain) / period;
                    avgLoss = (avgLoss * (period - 1) + loss) / period;
                    results.push({ time: data[i].time, value: 100 - (100 / (1 + avgGain / avgLoss)) });
                }
            }
            return results;
        }

        // Bridge function called from Kotlin
        window.updateData = (jsonData) => {
            const data = JSON.parse(jsonData);
            if (data && data.length > 0) {
                candleSeries.setData(data);
                const rsiData = calculateRSI(data);
                rsiSeries.setData(rsiData);
            }
        };

        // Handle resizing
        window.addEventListener('resize', () => {
            chart.resize(window.innerWidth, window.innerHeight);
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
}
