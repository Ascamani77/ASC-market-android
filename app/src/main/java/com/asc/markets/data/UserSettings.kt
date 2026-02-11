package com.asc.markets.data

data class UserSettings(
    var themeMode: String = "DARK",
    var analyticalFocus: String = "H1",
    var intelligenceThreshold: Int = 50,
    var riskLevel: String = "Balanced",
    var assetWhitelist: List<String> = emptyList(),
    var notificationsEnabled: Boolean = true,
    var sleepStartHour: Int = 23,
    var sleepEndHour: Int = 7,
    var newsImpactFilter: Boolean = true
)
