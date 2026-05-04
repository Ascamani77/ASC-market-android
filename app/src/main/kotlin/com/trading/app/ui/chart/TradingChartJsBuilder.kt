package com.trading.app.ui.chart

import javax.inject.Inject
import javax.inject.Singleton

class TradingChartJsBuilder @Inject constructor() {

    fun buildHtml(): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <script src="https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js"></script>
    <style>
        body { 
            margin: 0; 
            padding: 0; 
            background-color: #0d0f1a; 
            overflow: hidden; 
            display: flex;
            flex-direction: column;
            height: 100vh;
        }
        #price-chart { flex: 3; width: 100%; border-bottom: 1px solid #2b2b43; }
        #rsi-chart { flex: 1; width: 100%; display: none; }
    </style>
</head>
<body>
    <div id="price-chart"></div>
    <div id="rsi-chart"></div>
    <script>
        const chartOptions = {
            layout: {
                background: { type: 'solid', color: '#0d0f1a' },
                textColor: '#d1d4dc',
            },
            grid: {
                vertLines: { color: 'rgba(42, 46, 57, 0.5)' },
                horzLines: { color: 'rgba(42, 46, 57, 0.5)' },
            },
            rightPriceScale: { borderColor: '#2b2b43' },
            timeScale: { borderColor: '#2b2b43', visible: false },
            crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
            handleScroll: { vertTouchDrag: false },
        };

        const priceChart = LightweightCharts.createChart(document.getElementById('price-chart'), {
            ...chartOptions,
            height: document.getElementById('price-chart').clientHeight,
        });
        const rsiChart = LightweightCharts.createChart(document.getElementById('rsi-chart'), {
            ...chartOptions,
            height: document.getElementById('rsi-chart').clientHeight,
            timeScale: { ...chartOptions.timeScale, visible: true },
        });

        const candleSeries = priceChart.addCandlestickSeries({
            upColor: '#089981', downColor: '#f23645',
            borderVisible: false, wickUpColor: '#089981', wickDownColor: '#f23645'
        });

        const rsiSeries = rsiChart.addLineSeries({
            color: '#7e57c2', lineWidth: 2,
            priceFormat: { type: 'price', precision: 2 }
        });

        // RSI Levels
        [30, 70].forEach(level => {
            rsiSeries.createPriceLine({
                price: level, color: 'rgba(126, 87, 194, 0.4)',
                lineWidth: 1, lineStyle: LightweightCharts.LineStyle.Dashed,
                axisLabelVisible: true, title: level.toString()
            });
        });

        // --- Synchronization Logic ---
        function syncCharts(source, target) {
            source.timeScale().subscribeVisibleTimeRangeChange(range => {
                target.timeScale().setVisibleRange(range);
            });
            source.subscribeCrosshairMove(param => {
                if (!param.time) {
                    target.clearCrosshairPosition();
                    return;
                }
                target.setCrosshairPosition(param.price, param.time, rsiSeries);
            });
        }
        syncCharts(priceChart, rsiChart);
        syncCharts(rsiChart, priceChart);

        // --- JavaScript RSI Calculation ---
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
                        results.push({ time: data[i].time, value: 100 - (100 / (1 + (avgGain / (avgLoss || 1)))) });
                    }
                } else {
                    avgGain = (avgGain * (period - 1) + gain) / period;
                    avgLoss = (avgLoss * (period - 1) + loss) / period;
                    results.push({ time: data[i].time, value: 100 - (100 / (1 + (avgGain / (avgLoss || 1)))) });
                }
            }
            return results;
        }

        let currentRsiPeriod = 14;

        window.updateData = (jsonData, rsiPeriod) => {
            if (rsiPeriod) currentRsiPeriod = rsiPeriod;
            const data = JSON.parse(jsonData);
            if (data && data.length > 0) {
                const rsiData = calculateRSI(data, currentRsiPeriod);
                candleSeries.setData(data);
                rsiSeries.setData(rsiData);
            }
        };

        window.setRsiVisible = (visible) => {
            document.getElementById('rsi-chart').style.display = visible ? 'block' : 'none';
            priceChart.resize(window.innerWidth, document.getElementById('price-chart').offsetHeight);
            rsiChart.resize(window.innerWidth, document.getElementById('rsi-chart').offsetHeight);
        };

        window.addEventListener('resize', () => {
            priceChart.resize(window.innerWidth, document.getElementById('price-chart').offsetHeight);
            rsiChart.resize(window.innerWidth, document.getElementById('rsi-chart').offsetHeight);
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
}
