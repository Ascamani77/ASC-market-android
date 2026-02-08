package com.asc.markets.data

import java.time.Instant

/**
 * Shared sample data used by UI previews and ViewModel defaults.
 */
fun sampleMacroEvents(): List<MacroEvent> = listOf(
    MacroEvent(
           title = formatEventTitle("US Non-Farm Payrolls [NFP]"),
        currency = "USD",
        datetimeUtc = System.currentTimeMillis() + 3600_000,
        priority = ImpactPriority.CRITICAL,
        status = MacroEventStatus.UPCOMING,
        source = "Economic Calendar",
        details = "Monthly payroll growth forecast compared against actual employment numbers"
    ),
    MacroEvent(
           title = formatEventTitle("ECB Rate Decision [ECB]"),
        currency = "EUR",
        datetimeUtc = System.currentTimeMillis() - 7200_000,
        priority = ImpactPriority.CRITICAL,
        status = MacroEventStatus.CONFIRMED,
        source = "ECB",
        details = "Interest rate decision announcement with accompanying monetary policy press conference"
    ),
    MacroEvent(
        title = formatEventTitle("BLS Employment Report [BLS]"),
        currency = "USD",
        datetimeUtc = System.currentTimeMillis() + 7200_000,
        priority = ImpactPriority.CRITICAL,
        status = MacroEventStatus.UPCOMING,
        source = "Bureau of Labor Statistics",
        details = "Monthly employment report focusing on job creation and wage growth metrics"
    ),
    MacroEvent(
        title = formatEventTitle("Consumer Price Index [CPI]"),
        currency = "USD",
        datetimeUtc = System.currentTimeMillis() + 10800_000,
        priority = ImpactPriority.CRITICAL,
        status = MacroEventStatus.UPCOMING,
        source = "Economic Calendar",
        details = "Consumer price index measuring month over month inflation trends and purchasing power"
    )
)
