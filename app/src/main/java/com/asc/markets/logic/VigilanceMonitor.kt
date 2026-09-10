package com.asc.markets.logic

import android.content.Context
import android.util.Log
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.ScannerSignalsStore
import com.asc.markets.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Live monitor for deployed EA vigilance nodes.
 *
 * Every time fresh EA write-ups arrive, each active EA_LIVE node is evaluated
 * against its asset's signal. On a match the node enters cooldown, the event is
 * recorded for the MY ALERTS triggered list, and a system notification fires —
 * so a deployed node actually alerts you instead of sitting idle.
 */
object VigilanceMonitor {
    private const val TAG = "VigilanceMonitor"

    @Volatile
    private var started = false

    /**
     * Called on every fired alert (after the system notification). Wired in
     * MainActivity to push the event into the in-app NOTIFICATIONS inbox.
     */
    @Volatile
    var onTriggered: ((TriggeredAlert) -> Unit)? = null

    // Mirrors every NEW actionable EA write-up to the app (like the Telegram
    // feed does), even when no Vigilance node is deployed for that asset.
    // Keyed by normalized asset symbol.
    private val mirrorLastTs = mutableMapOf<String, Long>()
    private val mirrorLastFireAt = mutableMapOf<String, Long>()

    /** Minimum silence between mirrored alerts per asset (avoid notification spam on refreshes). */
    private const val MIRROR_THROTTLE_MS = 5L * 60L * 1000L

    /**
     * EA write-up timestamps are unix seconds (accept millis too). Fresh means
     * written within the last ~30 minutes — generous enough to tolerate the
     * VM/phone clock skew, tight enough to skip hours-old signals on restart.
     */
    private fun isFreshWriteup(ts: Long): Boolean {
        val nowSec = System.currentTimeMillis() / 1000L
        val tsSec = if (ts > 1_000_000_000_000L) ts / 1000L else ts
        return tsSec >= nowSec - 1800L
    }

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            EASignalLiveStore.signalsByAsset.collect { byAsset ->
                try {
                    val firedKeys = evaluateAll(appContext, byAsset)
                    mirrorEaAlerts(appContext, byAsset, firedKeys)
                } catch (e: Exception) {
                    Log.e(TAG, "Monitor pass failed: ${e.message}")
                }
            }
        }
        // Keep every deployed node's asset streaming: without this, nodes for
        // assets nobody opened would never receive write-ups to evaluate. Also
        // request signals for the FULL EA universe so the mirror (Telegram 1:1)
        // sees alerts for every asset the backend reports, not just watched ones.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            while (true) {
                try {
                    val symbols = mutableSetOf<String>()
                    VigilanceNodeEngine.getActiveNodes()
                        .filter { it.alertType == "EA_LIVE" && it.isActive }
                        .map { it.pair }
                        .forEach { symbols.add(it) }
                    EALiveDataStore.liveAssets.value.map { it.symbol }
                        .filter { it.isNotBlank() }
                        .forEach { symbols.add(it) }
                    symbols.forEach { EASignalLiveStore.requestSignal(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Signal request pass failed: ${e.message}")
                }
                kotlinx.coroutines.delay(30_000L)
            }
        }
        Log.d(TAG, "Vigilance monitor started")
    }

    private fun normalizeKey(raw: String): String = raw.uppercase()
        .replace("/", "").replace("-", "").replace("_", "")
        .replace(" ", "").replace(".", "").removeSuffix("M")

    // Handles node evaluation. Returns the normalized keys of assets whose
    // deployed node actually fired (so the mirror below won't re-notify them).
    private fun evaluateAll(
        context: Context,
        byAsset: Map<String, com.asc.markets.data.ASCSignalData>
    ): Set<String> {
        val firedKeys = mutableSetOf<String>()
        val nodes = VigilanceNodeEngine.getActiveNodes()
            .filter { it.alertType == "EA_LIVE" && it.isActive }
        if (nodes.isEmpty() || byAsset.isEmpty()) return firedKeys

        nodes.forEach { node ->
            val nodeKey = normalizeKey(node.pair)
            val signal = byAsset.entries.find {
                it.key.equals(node.pair, ignoreCase = true) || normalizeKey(it.key) == nodeKey
            }?.value ?: return@forEach

            // EA direction is BUY/SELL; node filters are LONG/SHORT/ANY.
            val dir = when (signal.direction.uppercase()) {
                "BUY" -> "LONG"
                "SELL" -> "SHORT"
                else -> signal.direction.uppercase()
            }
            // EA sends win_pct 0-100; accept 0-1 too.
            val rawVote = signal.chart_panel?.votes?.win_pct ?: 0.0
            val votePct = if (rawVote > 1.0) rawVote else rawVote * 100.0
            // evaluateEANode expects confidence 0-1.
            val rawConf = signal.confidence
            val conf01 = if (rawConf > 1.0) (rawConf / 100.0).coerceIn(0.0, 1.0) else rawConf.coerceIn(0.0, 1.0)
            val liq = signal.liquidity
            val tier = signal.chart_panel?.quality_tier?.ifBlank { "NONE" } ?: "NONE"

            // Scanner P(T) — normalized to 0-100.
            val scanKey = normalizeKey(signal.asset.ifBlank { node.pair })
            val scannerSig = ScannerSignalsStore.signals.value.firstOrNull {
                normalizeKey(it.asset) == scanKey
            }
            val pTradePct = scannerSig?.pTrade?.let { if (it > 1.0) it else it * 100.0 }?.toInt() ?: -1
            // Validator P(WIN) — normalized to 0-100.
            val rawPwin = signal.chart_panel?.validator_pwin ?: 0.0
            val pwinPct = (if (rawPwin > 1.0) rawPwin else rawPwin * 100.0).toInt()
            val validatorActive = signal.chart_panel?.validator_active == true
            val validatorAllowed = signal.chart_panel?.validator_allowed == true
            // Premium/Discount zone flag derived from zone context / SMC panels.
            val pdActive = signal.zone_context_type.contains("PREMIUM", true) ||
                signal.zone_context_type.contains("DISCOUNT", true) ||
                signal.target_zone.contains("PREMIUM", true) ||
                signal.target_zone.contains("DISCOUNT", true) ||
                signal.chart_panels?.smc_details.orEmpty().contains("PREMIUM", true) ||
                signal.chart_panels?.smc_details.orEmpty().contains("DISCOUNT", true) ||
                signal.chart_panels?.smc_status.orEmpty().contains("PREMIUM", true) ||
                signal.chart_panels?.smc_status.orEmpty().contains("DISCOUNT", true)

            val fired = try {
                VigilanceNodeEngine.evaluateEANode(
                    nodeId = node.id,
                    votePct = votePct,
                    confidence = conf01,
                    direction = dir,
                    qualityTier = tier,
                    fvgBull = liq?.fvg_bull == true,
                    fvgBear = liq?.fvg_bear == true,
                    bosBull = liq?.bos_bull == true,
                    bosBear = liq?.bos_bear == true,
                    sweepHigh = liq?.sweep_high == true,
                    sweepLow = liq?.sweep_low == true,
                    pdActive = pdActive,
                    pTradePct = pTradePct.toDouble(),
                    pwinPct = pwinPct.toDouble(),
                    validatorActive = validatorActive,
                    validatorAllowed = validatorAllowed,
                    validatorDirection = signal.chart_panel?.validator_direction ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Evaluate failed for ${node.pair}: ${e.message}")
                false
            }
            if (!fired) return@forEach

            VigilanceNodeEngine.markEANodeTriggered(node.id) ?: return@forEach
            val title = "Vigilance alert: ${node.pair} $dir"
            val body = "${node.description} • Vote ${votePct.toInt()}% • AI ${(conf01 * 100).toInt()}% • $tier"
            val alert = TriggeredAlert(
                nodeId = node.id,
                pair = node.pair,
                title = title,
                body = body
            )
            VigilanceNodeEngine.recordTriggeredAlert(alert)
            try {
                NotificationHelper.showAlert(context, title, body, "vigilance", node.pair)
            } catch (e: Exception) {
                Log.e(TAG, "Notification failed: ${e.message}")
            }
            try {
                onTriggered?.invoke(alert)
            } catch (e: Exception) {
                Log.e(TAG, "onTriggered hook failed: ${e.message}")
            }
            Log.i(TAG, "FIRED ${node.pair} ($dir) vote=${votePct.toInt()}% conf=${(conf01 * 100).toInt()}%")
            firedKeys.add(nodeKey)
        }
        return firedKeys
    }

    /**
     * Mirrors the EA feed the way the Telegram channel does: every fresh,
     * actionable write-up becomes a local alert so the app never misses an EA
     * signal — regardless of deployed nodes. Assets already fired by a node in
     * this pass are skipped so they don't double-notify.
     */
    private fun mirrorEaAlerts(
        context: Context,
        byAsset: Map<String, com.asc.markets.data.ASCSignalData>,
        firedKeys: Set<String>
    ) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("allow_signal_push", true)) return
        byAsset.forEach { (key, signal) ->
            if (key in firedKeys) return@forEach

            val ts = signal.timestamp
            if (ts <= 0L) return@forEach
            val last = mirrorLastTs[key] ?: 0L
            if (ts <= last) return@forEach      // same write-up, not a new alert
            mirrorLastTs[key] = ts
            // First sighting this session (fresh app start / reconnect): catch up
            // on genuinely fresh write-ups so a restart doesn't eat the alert
            // Telegram just sent — but stay silent for stale ones to avoid a
            // burst of hours-old signals on every launch.
            if (last == 0L && !isFreshWriteup(ts)) return@forEach

            val dir = when (signal.direction.uppercase()) {
                "BUY" -> "LONG"
                "SELL" -> "SHORT"
                else -> signal.direction.uppercase()
            }
            // Only actionable EA signals get posted to the Telegram feed.
            if (dir != "LONG" && dir != "SHORT") return@forEach

            val lastFire = mirrorLastFireAt[key] ?: 0L
            if (now - lastFire < MIRROR_THROTTLE_MS) return@forEach

            // Collapsed line: asset + direction + EA (SMC vote) + AI (ML vote) + combined.
            val votes = signal.chart_panel?.votes
            fun voteShare(favour: Int, against: Int): String {
                val total = favour + against
                if (total <= 0) return "—"
                return "${(favour.toDouble() / total.toDouble() * 100.0).toInt()}%"
            }
            val eaShare = when (dir) {
                "SHORT" -> voteShare(votes?.smc_bear ?: 0, votes?.smc_bull ?: 0)
                else -> voteShare(votes?.smc_bull ?: 0, votes?.smc_bear ?: 0)
            }
            val aiShare = when (dir) {
                "SHORT" -> voteShare(votes?.ai_bear ?: 0, votes?.ai_bull ?: 0)
                else -> voteShare(votes?.ai_bull ?: 0, votes?.ai_bear ?: 0)
            }
            val rawVote = votes?.win_pct ?: 0.0
            val combinedPct = (if (rawVote > 1.0) rawVote else rawVote * 100.0).toInt()
            val title = "${signal.asset} $dir • EA $eaShare • AI $aiShare • Combined ${combinedPct}%"
            // Expanded lines: validator direction+score, P(Trade), P(Win), confidence.
            // (Shown via BigTextStyle on expand — no tap needed.)
            val validatorDir = signal.chart_panel?.validator_direction?.ifBlank { "—" } ?: "—"
            val rawPwin = signal.chart_panel?.validator_pwin ?: 0.0
            val pwinPct = (if (rawPwin > 1.0) rawPwin else rawPwin * 100.0).toInt()
            val scanKey = normalizeKey(signal.asset.ifBlank { key })
            val pTradeRaw = ScannerSignalsStore.signals.value.firstOrNull {
                normalizeKey(it.asset) == scanKey
            }?.pTrade
            val pTradeText = when {
                pTradeRaw == null || pTradeRaw < 0.0 -> "—"
                pTradeRaw > 1.0 -> "${pTradeRaw.toInt()}%"
                else -> "${(pTradeRaw * 100.0).toInt()}%"
            }
            val confRaw = signal.confidence
            val confPct = (if (confRaw > 1.0) confRaw else confRaw * 100.0).toInt()
            val body = "Validator: $validatorDir @ ${pwinPct}%\n" +
                "P(Trade): $pTradeText\n" +
                "P(Win): ${pwinPct}%\n" +
                "Confidence: ${confPct}%"
            val alert = TriggeredAlert(
                nodeId = "ea_mirror_$key",
                pair = signal.asset,
                title = title,
                body = body
            )
            VigilanceNodeEngine.recordTriggeredAlert(alert)
            try {
                NotificationHelper.showAlert(context, title, body, "ea_signal", signal.asset)
            } catch (e: Exception) {
                Log.e(TAG, "Mirror notification failed: ${e.message}")
            }
            try {
                onTriggered?.invoke(alert)
            } catch (e: Exception) {
                Log.e(TAG, "Mirror onTriggered hook failed: ${e.message}")
            }
            mirrorLastFireAt[key] = now
            Log.i(TAG, "MIRRORED ${signal.asset} ($dir) conf=${confPct}% ts=$ts")
        }
    }
}
