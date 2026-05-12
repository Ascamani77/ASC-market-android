package com.asc.markets.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class PreMoveLayer(
    val label: String,
    val status: String,
    val score: Int,
    val detail: String
)

data class LiquidityPool(
    val label: String,
    val side: String,
    val level: Double,
    val distancePercent: Double,
    val strength: Int
)

data class CorrelationSignal(
    val symbol: String,
    val coefficient: Double,
    val alignment: String
)

data class PreMoveCandidate(
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val price: Double,
    val changePercent: Double,
    val timeframe: String,
    val state: String,
    val directionBias: String,
    val preMoveScore: Int,
    val compressionScore: Int,
    val ignitionScore: Int,
    val structuralPressure: Int,
    val liquidityScore: Int,
    val sweepProbability: Int,
    val regime: String,
    val liquidityMagnet: String,
    val expectedWindow: String,
    val riskGate: String,
    val invalidationLevel: Double?,
    val buySideLiquidity: Double,
    val sellSideLiquidity: Double,
    val liquidityPools: List<LiquidityPool>,
    val layers: List<PreMoveLayer>,
    val deterministicReason: String,
    val triggerConditions: List<String>,
    val correlationGate: String,
    val correlations: List<CorrelationSignal>,
    val trapRisk: String
)

data class MicroJitterSnapshot(
    val symbol: String,
    val state: String,
    val preIgnitionScore: Int,
    val ticksPerSecond: Double,
    val baselineTicksPerSecond: Double,
    val burstMultiplier: Double,
    val averageIntervalMs: Long,
    val spreadJitter: Int,
    val microVolatility: Int,
    val directionalPressure: Int,
    val feedStatus: String,
    val lastTickAgeMs: Long,
    val bridgeLatencyLabel: String,
    val detail: String
)

object PreMoveIntelligenceStore {
    private val marketSnapshot = combine(
        MarketDataStore.allPairs,
        MarketDataStore.priceHistory,
        MarketDataStore.timedPriceHistory
    ) { pairs, history, timedHistory ->
        Triple(pairs, history, timedHistory)
    }

    private val binanceSnapshot = combine(
        BinanceDataStore.allPairs,
        BinanceDataStore.priceHistory,
        BinanceDataStore.timedPriceHistory
    ) { pairs, history, timedHistory ->
        Triple(pairs, history, timedHistory)
    }

    private val fallbackSnapshot = combine(
        CombinedFallbackDataStore.allPairs,
        CombinedFallbackDataStore.priceHistory,
        CombinedFallbackDataStore.timedPriceHistory
    ) { pairs, history, timedHistory ->
        Triple(pairs, history, timedHistory)
    }

    val candidates: Flow<List<PreMoveCandidate>> = combine(marketSnapshot, binanceSnapshot, fallbackSnapshot) { market, binance, fallback ->
        buildCandidates(
            pairs = market.first + binance.first + fallback.first,
            priceHistory = market.second + binance.second + fallback.second,
            timedPriceHistory = market.third + binance.third + fallback.third
        )
    }.distinctUntilChanged()

    fun buildCandidates(
        pairs: List<ForexPair>,
        priceHistory: Map<String, List<Double>>,
        timedPriceHistory: Map<String, List<TimedPrice>>
    ): List<PreMoveCandidate> {
        val usablePairs = pairs
            .filter { it.category != MarketCategory.BONDS }
            .filter { it.price.isFinite() && it.price > 0.0 }
            .distinctBy { normalize(it.symbol) }

        return usablePairs
            .map { pair -> buildCandidate(pair, priceHistory, timedPriceHistory, usablePairs) }
            .sortedWith(
                compareByDescending<PreMoveCandidate> { it.riskGate == "PASS" || it.riskGate == "WATCH" }
                    .thenByDescending { it.preMoveScore }
                    .thenByDescending { it.compressionScore }
            )
    }

    fun candidateFor(symbol: String, candidates: List<PreMoveCandidate>): PreMoveCandidate? {
        return candidates.firstOrNull { MarketDataStore.matchesSymbol(it.symbol, symbol) }
            ?: candidates.firstOrNull()
    }

    fun buildMicroJitterSnapshot(pair: ForexPair, timedPriceHistory: Map<String, List<TimedPrice>>): MicroJitterSnapshot {
        val samples = matchingTimedHistory(pair, timedPriceHistory)
        val now = System.currentTimeMillis()
        val lastTickAge = samples.lastOrNull()?.let { now - it.timestampMillis } ?: Long.MAX_VALUE
        val recent = samples.takeLast(80)
        val baseline = samples.dropLast(recent.size).takeLast(240).ifEmpty { samples.take(max(samples.size - recent.size, 0)) }
        val recentWindowMs = windowMillis(recent)
        val baselineWindowMs = windowMillis(baseline)
        val ticksPerSecond = if (recentWindowMs > 0) recent.size * 1000.0 / recentWindowMs else 0.0
        val baselineTicksPerSecond = if (baselineWindowMs > 0) baseline.size * 1000.0 / baselineWindowMs else ticksPerSecond.coerceAtLeast(0.1)
        val burstMultiplier = if (baselineTicksPerSecond > 0.0) ticksPerSecond / baselineTicksPerSecond else 0.0
        val avgInterval = averageInterval(recent)
        val recentVol = percentRange(recent.map { it.price })
        val baseVol = percentRange(baseline.map { it.price }).coerceAtLeast(0.0001)
        val microVolatility = ((recentVol / baseVol) * 35.0).toInt().coerceIn(0, 100)
        val spreadJitter = ((abs(burstMultiplier - 1.0) * 28.0) + microVolatility * 0.35).toInt().coerceIn(0, 100)
        val directionalPressure = directionalPressure(recent.map { it.price })
        val score = (spreadJitter * 0.30 + microVolatility * 0.30 + abs(directionalPressure - 50) * 0.40 + burstMultiplier.coerceAtMost(3.0) * 10.0).toInt().coerceIn(0, 100)
        val state = when {
            samples.size < 12 -> "INSUFFICIENT TICKS"
            lastTickAge > 10_000L -> "FEED STALE"
            score >= 78 -> "IGNITION"
            score >= 62 -> "UNSTABLE"
            score >= 45 -> "BUILDING"
            else -> "CALM"
        }
        val detail = when (state) {
            "IGNITION" -> "Tick instability is elevated enough to confirm pre-displacement pressure."
            "UNSTABLE" -> "Microstructure is destabilizing; wait for deterministic layer agreement."
            "BUILDING" -> "Jitter is building but not yet an ignition confirmation."
            "FEED STALE" -> "Last MT5 tick is too old for micro-jitter confirmation."
            "INSUFFICIENT TICKS" -> "More live MT5 ticks are needed before this monitor can validate ignition."
            else -> "Microstructure is calm; no pre-ignition confirmation yet."
        }

        return MicroJitterSnapshot(
            symbol = pair.symbol,
            state = state,
            preIgnitionScore = score,
            ticksPerSecond = ticksPerSecond,
            baselineTicksPerSecond = baselineTicksPerSecond,
            burstMultiplier = burstMultiplier,
            averageIntervalMs = avgInterval,
            spreadJitter = spreadJitter,
            microVolatility = microVolatility,
            directionalPressure = directionalPressure,
            feedStatus = if (lastTickAge <= 5_000L) "MT5 LIVE" else "STALE",
            lastTickAgeMs = if (lastTickAge == Long.MAX_VALUE) -1L else lastTickAge,
            bridgeLatencyLabel = if (lastTickAge <= 5_000L) "VALID" else "WAITING",
            detail = detail
        )
    }

    private fun buildCandidate(
        pair: ForexPair,
        priceHistory: Map<String, List<Double>>,
        timedPriceHistory: Map<String, List<TimedPrice>>,
        allPairs: List<ForexPair>
    ): PreMoveCandidate {
        val history = matchingHistory(pair, priceHistory).ifEmpty { listOf(pair.price) }
        val prices = sanitizePrices(history, pair.price)
        val recent = prices.takeLast(12)
        val older = prices.dropLast(recent.size).takeLast(24).ifEmpty { prices.take(max(prices.size - recent.size, 0)) }
        val fullRange = percentRange(prices)
        val recentRange = percentRange(recent)
        val olderRange = percentRange(older).coerceAtLeast(recentRange)
        val directional = directionalPressure(prices)
        val compressionScore = (100.0 - recentRange * 38.0 - abs(pair.changePercent) * 6.0).toInt().coerceIn(0, 100)
        val volatilityContraction = if (olderRange > 0.0) (1.0 - recentRange / olderRange).coerceIn(0.0, 1.0) else 0.0
        val pressureDistance = abs(directional - 50)
        val ignitionScore = (volatilityContraction * 45.0 + pressureDistance * 0.45 + abs(pair.changePercent) * 8.0).toInt().coerceIn(0, 100)
        val structuralPressure = directional.coerceIn(0, 100)
        val high = prices.maxOrNull() ?: pair.price
        val low = prices.minOrNull() ?: pair.price
        val buyDistance = distancePercent(pair.price, high)
        val sellDistance = distancePercent(pair.price, low)
        val nearestDistance = min(buyDistance, sellDistance)
        val liquidityScore = (100.0 - nearestDistance * 32.0 + compressionScore * 0.20).toInt().coerceIn(0, 100)
        val sweepProbability = (liquidityScore * 0.45 + compressionScore * 0.25 + ignitionScore * 0.30).toInt().coerceIn(0, 100)
        val preMoveScore = (compressionScore * 0.30 + ignitionScore * 0.25 + liquidityScore * 0.25 + pressureDistance * 0.20).toInt().coerceIn(0, 100)
        val bias = when {
            structuralPressure >= 58 -> "BULLISH"
            structuralPressure <= 42 -> "BEARISH"
            else -> "NEUTRAL"
        }
        val state = when {
            abs(pair.changePercent) > 2.8 && compressionScore < 45 -> "LATE MOVE"
            preMoveScore >= 78 && ignitionScore >= 60 -> "ARMED"
            preMoveScore >= 62 -> "WATCH"
            compressionScore >= 70 -> "COMPRESSING"
            else -> "FILTERING"
        }
        val regime = when {
            compressionScore >= 75 && ignitionScore >= 55 -> "Expansion candidate"
            compressionScore >= 70 -> "Compression"
            ignitionScore >= 65 -> "Transition"
            abs(pair.changePercent) > 2.8 -> "Post-move"
            else -> "Idle"
        }
        val riskGate = when {
            state == "LATE MOVE" -> "BLOCKED"
            preMoveScore >= 72 -> "PASS"
            preMoveScore >= 55 -> "WATCH"
            else -> "WAIT"
        }
        val magnet = if (buyDistance <= sellDistance) "Buy-side liquidity" else "Sell-side liquidity"
        val invalidation = when (bias) {
            "BULLISH" -> low
            "BEARISH" -> high
            else -> null
        }
        val pools = listOf(
            LiquidityPool("Previous range high", "BUY-SIDE", high, buyDistance, liquidityStrength(buyDistance, compressionScore)),
            LiquidityPool("Previous range low", "SELL-SIDE", low, sellDistance, liquidityStrength(sellDistance, compressionScore))
        ).sortedBy { it.distancePercent }
        val correlations = buildCorrelations(pair, allPairs, priceHistory)
        val correlationGate = when {
            correlations.isEmpty() -> "NO CROSS-ASSET SAMPLE"
            correlations.any { it.alignment == "CONFLICT" && abs(it.coefficient) >= 0.65 } -> "CONFLICT"
            correlations.any { it.alignment == "SUPPORT" && abs(it.coefficient) >= 0.55 } -> "SUPPORT"
            else -> "NEUTRAL"
        }
        val trapRisk = when {
            sweepProbability >= 76 && compressionScore >= 65 -> "HIGH"
            sweepProbability >= 58 -> "MEDIUM"
            else -> "LOW"
        }
        val layers = listOf(
            PreMoveLayer("L1 STRUCTURE", passLabel(structuralPressure), structuralPressure, "$bias pressure measured from recent structure."),
            PreMoveLayer("L3 REGIME", if (regime == "Post-move") "BLOCK" else "PASS", if (regime == "Post-move") 35 else preMoveScore, regime),
            PreMoveLayer("L5 PRESSURE", passLabel(pressureDistance + 50), (pressureDistance + 50).coerceIn(0, 100), "Directional pressure is ${pressureDistance} points away from neutral."),
            PreMoveLayer("L6A IDLE FILTER", if (state == "LATE MOVE") "FAIL" else "PASS", compressionScore, "Compression prevents chasing late movement."),
            PreMoveLayer("L7 EXPANSION", passLabel(preMoveScore), preMoveScore, "Expansion probability is weighted from compression, liquidity and ignition."),
            PreMoveLayer("RISKAI", riskGate, if (riskGate == "PASS") 90 else preMoveScore, "Execution remains gated until ignition and invalidation align.")
        )
        val reason = "$regime on $timeframeLabel with $magnet nearest, $bias structural pressure and $riskGate risk gate."
        val triggers = listOf(
            "Wait for ignition score above 70 without a late-move block.",
            "Confirm sweep or rejection around ${formatLevel(pools.firstOrNull()?.level ?: pair.price)}.",
            "Do not arm if price invalidates ${invalidation?.let(::formatLevel) ?: "the current compression range"}.",
            "News and calendar guard must remain clear before dispatch."
        )

        return PreMoveCandidate(
            symbol = pair.symbol,
            name = pair.name,
            category = pair.category,
            price = pair.price,
            changePercent = pair.changePercent,
            timeframe = timeframeLabel,
            state = state,
            directionBias = bias,
            preMoveScore = preMoveScore,
            compressionScore = compressionScore,
            ignitionScore = ignitionScore,
            structuralPressure = structuralPressure,
            liquidityScore = liquidityScore,
            sweepProbability = sweepProbability,
            regime = regime,
            liquidityMagnet = magnet,
            expectedWindow = expectedWindow(preMoveScore, ignitionScore),
            riskGate = riskGate,
            invalidationLevel = invalidation,
            buySideLiquidity = high,
            sellSideLiquidity = low,
            liquidityPools = pools,
            layers = layers,
            deterministicReason = reason,
            triggerConditions = triggers,
            correlationGate = correlationGate,
            correlations = correlations,
            trapRisk = trapRisk
        )
    }

    private const val timeframeLabel = "H1"

    private fun matchingHistory(pair: ForexPair, histories: Map<String, List<Double>>): List<Double> {
        return histories[pair.symbol]
            ?: histories.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, pair.symbol) }?.value
            ?: emptyList()
    }

    private fun matchingTimedHistory(pair: ForexPair, histories: Map<String, List<TimedPrice>>): List<TimedPrice> {
        return histories[pair.symbol]
            ?: histories.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, pair.symbol) }?.value
            ?: emptyList()
    }

    private fun sanitizePrices(values: List<Double>, fallback: Double): List<Double> {
        val filtered = values.filter { it.isFinite() && it > 0.0 }
        return when {
            filtered.size >= 6 -> filtered.takeLast(80)
            fallback.isFinite() && fallback > 0.0 -> List(12) { index -> fallback * (1.0 + ((index - 6) * 0.00005)) }
            else -> emptyList()
        }
    }

    private fun percentRange(values: List<Double>): Double {
        val clean = values.filter { it.isFinite() && it > 0.0 }
        if (clean.size < 2) return 0.0
        val high = clean.maxOrNull() ?: return 0.0
        val low = clean.minOrNull() ?: return 0.0
        val last = clean.last().takeIf { it != 0.0 } ?: return 0.0
        return abs(high - low) / last * 100.0
    }

    private fun directionalPressure(values: List<Double>): Int {
        val clean = values.filter { it.isFinite() && it > 0.0 }
        if (clean.size < 2) return 50
        val first = clean.first()
        val last = clean.last()
        val changePct = if (first != 0.0) (last - first) / first * 100.0 else 0.0
        val upMoves = clean.zipWithNext().count { it.second >= it.first }
        val moveBalance = upMoves.toDouble() / (clean.size - 1).coerceAtLeast(1)
        return (50.0 + changePct * 15.0 + (moveBalance - 0.5) * 40.0).toInt().coerceIn(0, 100)
    }

    private fun distancePercent(price: Double, level: Double): Double {
        if (price <= 0.0 || level <= 0.0) return 100.0
        return abs(level - price) / price * 100.0
    }

    private fun liquidityStrength(distance: Double, compression: Int): Int {
        return (100.0 - distance * 30.0 + compression * 0.20).toInt().coerceIn(0, 100)
    }

    private fun buildCorrelations(pair: ForexPair, pairs: List<ForexPair>, history: Map<String, List<Double>>): List<CorrelationSignal> {
        val base = matchingHistory(pair, history).takeLast(40)
        if (base.size < 8) return emptyList()
        return pairs
            .filter { !MarketDataStore.matchesSymbol(it.symbol, pair.symbol) }
            .filter { it.category != MarketCategory.BONDS }
            .mapNotNull { other ->
                val otherHistory = matchingHistory(other, history).takeLast(base.size)
                if (otherHistory.size < 8) return@mapNotNull null
                val coeff = pearson(base.takeLast(otherHistory.size), otherHistory)
                val alignment = when {
                    coeff >= 0.55 -> "SUPPORT"
                    coeff <= -0.55 -> "INVERSE"
                    abs(coeff) >= 0.65 -> "CONFLICT"
                    else -> "NEUTRAL"
                }
                CorrelationSignal(other.symbol, coeff, alignment)
            }
            .sortedByDescending { abs(it.coefficient) }
            .take(5)
    }

    private fun pearson(a: List<Double>, b: List<Double>): Double {
        val size = min(a.size, b.size)
        if (size < 2) return 0.0
        val x = a.takeLast(size)
        val y = b.takeLast(size)
        val avgX = x.average()
        val avgY = y.average()
        val numerator = x.indices.sumOf { (x[it] - avgX) * (y[it] - avgY) }
        val denX = sqrt(x.sumOf { (it - avgX).pow(2.0) })
        val denY = sqrt(y.sumOf { (it - avgY).pow(2.0) })
        val denominator = denX * denY
        return if (denominator == 0.0) 0.0 else (numerator / denominator).coerceIn(-1.0, 1.0)
    }

    private fun windowMillis(values: List<TimedPrice>): Long {
        if (values.size < 2) return 0L
        return (values.last().timestampMillis - values.first().timestampMillis).coerceAtLeast(0L)
    }

    private fun averageInterval(values: List<TimedPrice>): Long {
        if (values.size < 2) return 0L
        return values.zipWithNext().map { it.second.timestampMillis - it.first.timestampMillis }.filter { it >= 0L }.average().toLong()
    }

    private fun passLabel(score: Int): String = when {
        score >= 70 -> "PASS"
        score >= 50 -> "WATCH"
        else -> "FAIL"
    }

    private fun expectedWindow(score: Int, ignition: Int): String = when {
        score >= 80 && ignition >= 70 -> "Next 1-2 H1 candles"
        score >= 65 -> "Next 2-4 H1 candles"
        score >= 50 -> "Developing session"
        else -> "No deterministic window"
    }

    private fun normalize(symbol: String): String = symbol.uppercase().replace("/", "").replace("_", "").replace("-", "")

    private fun formatLevel(value: Double): String = when {
        value >= 1000.0 -> String.format(java.util.Locale.US, "%,.2f", value)
        value >= 1.0 -> String.format(java.util.Locale.US, "%.5f", value)
        else -> String.format(java.util.Locale.US, "%.6f", value)
    }
}
