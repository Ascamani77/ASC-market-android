package com.researchcenter.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    fun formatRelativeTime(dateStr: String): String {
        if (dateStr.isEmpty()) return "Active"

        // PRESERVE EXACT WEBSITE STRING if not ISO
        val isISO = dateStr.contains("T") && dateStr.contains(":")
        if (!isISO) return dateStr

        return try {
            val odt = OffsetDateTime.parse(dateStr)
            val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
            odt.format(formatter)
        } catch (e: Exception) {
            dateStr
        }
    }
}

