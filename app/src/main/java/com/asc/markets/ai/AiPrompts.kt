package com.asc.markets.ai

object AiPrompts {
    val INSTITUTIONAL_OPERATIONAL_DIRECTIVE = """
        INSTITUTIONAL_OPERATIONAL_DIRECTIVE: Dedicate 90% of your logic to PRE-MOVE timing. Strictly suppress all retail microstructure 'trading advice'. Prioritize macro event accumulation, timing, and institutional liquidity signals; avoid providing trade-level action or dispatch suggestions.
    """.trimIndent()

    fun buildAnalysisPrompt(question: String): String {
        return "${INSTITUTIONAL_OPERATIONAL_DIRECTIVE} Provide a concise surveillance-style analysis for: $question"
    }

    fun buildNewsPrompt(): String {
        return """
            ${INSTITUTIONAL_OPERATIONAL_DIRECTIVE}
            Generate up to 10 macro-relevant headlines or event notices for the Macro Intelligence Stream. Prefer events tied to macro calendars, accumulation signs across sessions, or institutional rebalancing cues.
            For each item return a JSON object with fields: title, source, datetime_utc (ISO 8601), priority (one of: \"CRITICAL\",\"HIGH\",\"MEDIUM\"), status (\"UPCOMING\" or \"CONFIRMED\"), details (brief). Return ONLY a valid JSON array (no extra text).
            Example:
            [{"title":"...","source":"...","datetime_utc":"2024-06-12T13:30:00Z","priority":"HIGH","status":"UPCOMING","details":"..."}, ...]
        """.trimIndent()
    }
}
