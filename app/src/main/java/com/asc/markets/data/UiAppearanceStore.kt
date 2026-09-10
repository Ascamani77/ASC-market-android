package com.asc.markets.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI appearance preferences that apply app-wide and update live.
 *
 * Currently hosts the InfoBox outline brightness: the alpha of the white
 * hairline border drawn around every InfoBox card (0 = invisible, 1 = solid
 * white). Persisted in "asc_prefs" so it survives restarts.
 */
object UiAppearanceStore {
    private const val PREFS_NAME = "asc_prefs"
    private const val KEY_BORDER_ALPHA = "infobox_border_alpha"

    /** Default matches the original HairlineBorder (8% white). */
    const val DEFAULT_BORDER_ALPHA = 0.08f

    private val _borderAlpha = MutableStateFlow(DEFAULT_BORDER_ALPHA)
    val borderAlpha: StateFlow<Float> = _borderAlpha.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _borderAlpha.value = prefs!!.getFloat(KEY_BORDER_ALPHA, DEFAULT_BORDER_ALPHA)
            .coerceIn(0f, 1f)
    }

    fun setBorderAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0f, 1f)
        _borderAlpha.value = clamped
        prefs?.edit()?.putFloat(KEY_BORDER_ALPHA, clamped)?.apply()
    }
}
