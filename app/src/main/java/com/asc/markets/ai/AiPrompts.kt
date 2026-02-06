package com.asc.markets.ai

object AiPrompts {
    val SYSTEM_DIRECTIVE = """
        System directive: You are the Macro Intelligence Stream assistant. Focus ONLY on macro event accumulation phases and macro event timing. Ignore price action microstructure, order book, DOM ladders, spreads, and trade-level execution details.
    """.trimIndent()

    fun buildAnalysisPrompt(question: String): String {
        return "$SYSTEM_DIRECTIVE Provide a concise surveillance-style analysis for: $question"
    }

    fun buildNewsPrompt(): String {
        return """
            $SYSTEM_DIRECTIVE
            Generate up to 10 macro-relevant headlines or event notices for the Macro Intelligence Stream. Prefer events tied to macro calendars, accumulation signs across sessions, or institutional rebalancing cues.
            For each item return a JSON object with fields: title, source, datetime_utc (ISO 8601), priority (one of: \"CRITICAL\",\"HIGH\",\"MEDIUM\"), status (\"UPCOMING\" or \"CONFIRMED\"), details (brief). Return ONLY a valid JSON array (no extra text).
            Example:
            [{"title":"...","source":"...","datetime_utc":"2024-06-12T13:30:00Z","priority":"HIGH","status":"UPCOMING","details":"..."}, ...]
        """.trimIndent()
    }
}
