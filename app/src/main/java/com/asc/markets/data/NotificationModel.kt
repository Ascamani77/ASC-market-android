package com.asc.markets.data

data class NotificationModel(
    val id: String,
    val type: String,
    val msg: String,
    val time: String,
    val severity: String,
    val seen: Boolean = false
)
