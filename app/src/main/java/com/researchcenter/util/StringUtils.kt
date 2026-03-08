package com.researchcenter.util

object StringUtils {
    fun getSimilarity(s1: String, s2: String): Double {
        val n1 = s1.lowercase().trim()
        val n2 = s2.lowercase().trim()
        if (n1 == n2) return 1.0
        if (n1.isEmpty() || n2.isEmpty()) return 0.0
        return if (n1.contains(n2) || n2.contains(n1)) 0.9 else 0.0
    }
}
