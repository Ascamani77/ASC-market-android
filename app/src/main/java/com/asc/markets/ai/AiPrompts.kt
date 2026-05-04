package com.asc.markets.ai

object AiPrompts {
    val INSTITUTIONAL_OPERATIONAL_DIRECTIVE = """
        INSTITUTIONAL_OPERATIONAL_DIRECTIVE: Dedicate 90% of your logic to PRE-MOVE timing. Strictly suppress all retail microstructure 'trading advice'. Prioritize macro event accumulation, timing, and institutional liquidity signals; avoid providing trade-level action or dispatch suggestions.
    """.trimIndent()

    fun buildAnalysisPrompt(question: String): String {
        val hardConstraint = "ONLY analyze assets within the active AssetContext. Explicitly ignore all others."
        return "${INSTITUTIONAL_OPERATIONAL_DIRECTIVE} $hardConstraint Provide a concise surveillance-style analysis for: $question"
    }

    fun buildNewsPrompt(): String {
        return """
            ${INSTITUTIONAL_OPERATIONAL_DIRECTIVE}
            ONLY analyze assets within the active AssetContext. Explicitly ignore all others.
            Generate up to 10 macro-relevant headlines for the Market Overview raw feed. Prefer events tied to macro calendars, accumulation signs across sessions, or institutional rebalancing cues.
            For each item return a JSON object with fields:
            headline,
            source,
            timestamp (short display label like "2h ago", "45m ago", or "Upcoming"),
            assetType (one of: "forex","crypto","commodities","indices","stocks","futures","bonds"),
            assetSymbol (examples: "EUR/USD", "BTC/USD", "XAU/USD", "SPX", "INTC", "US10Y"),
            imageUrl (optional, use empty string when unavailable).
            Return ONLY a valid JSON array (no extra text).
            Example:
            [{"headline":"Dollar elevated near multi-year highs against the yen","source":"Reuters","timestamp":"2h ago","assetType":"forex","assetSymbol":"USD/JPY","imageUrl":""}, ...]
        """.trimIndent()
    }
}
