package com.asc.markets.data

import java.time.Instant

/**
 * Shared sample data used by UI previews and ViewModel defaults.
 */
fun sampleMacroEvents(): List<MacroEvent> = listOf(
    MacroEvent(
        title = "US NFP",
        currency = "USD",
        datetimeUtc = System.currentTimeMillis() + 3600_000,
        priority = ImpactPriority.CRITICAL,
        status = MacroEventStatus.UPCOMING,
        source = "Economic Calendar",
        details = "Payrolls estimate vs actual"
    ),
    MacroEvent(
        title = "ECB Rate Decision",
        currency = "EUR",
        datetimeUtc = System.currentTimeMillis() - 7200_000,
        priority = ImpactPriority.HIGH,
        status = MacroEventStatus.CONFIRMED,
        source = "ECB",
        details = "Decision and press conference"
    )
)
