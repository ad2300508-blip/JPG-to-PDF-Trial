package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("Sistema", "Segue il tema del dispositivo"),
    LIGHT("Chiaro", "Sempre chiaro"),
    DARK("Scuro", "Sempre scuro, riposante per gli occhi")
}

/**
 * Small app-wide settings store backed by SharedPreferences.
 */
object AppPreferences {

    private const val PREFS_NAME = "omniconvert_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_HAPTICS = "haptics"
    private const val KEY_FAVORITES = "favorite_tools"

    private var prefs: SharedPreferences? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _reduceMotion = MutableStateFlow(false)
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _haptics = MutableStateFlow(true)
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()

    private val _favoriteTools = MutableStateFlow<Set<String>>(emptySet())
    /** Names of the tools pinned on the home screen (OmniTool.name). */
    val favoriteTools: StateFlow<Set<String>> = _favoriteTools.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        _themeMode.value = p.getString(KEY_THEME_MODE, null)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
        _reduceMotion.value = p.getBoolean(KEY_REDUCE_MOTION, false)
        _haptics.value = p.getBoolean(KEY_HAPTICS, true)
        _favoriteTools.value = p.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
        prefs?.edit()?.putBoolean(KEY_REDUCE_MOTION, enabled)?.apply()
    }

    fun setHaptics(enabled: Boolean) {
        _haptics.value = enabled
        prefs?.edit()?.putBoolean(KEY_HAPTICS, enabled)?.apply()
    }

    /** Pins or unpins a tool; returns true when the tool is now a favorite. */
    fun toggleFavorite(toolName: String): Boolean {
        val current = _favoriteTools.value
        val updated = if (toolName in current) current - toolName else current + toolName
        _favoriteTools.value = updated
        prefs?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
        return toolName in updated
    }
}
