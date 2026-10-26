package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.PdfQuality
import com.example.ui.theme.AccentChoice
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
    private const val KEY_ACCENT = "accent"
    private const val KEY_DEFAULT_QUALITY = "default_quality"
    private const val KEY_DEFAULT_PAGE_SIZE = "default_page_size"
    private const val KEY_AUTO_CLEAN = "default_auto_clean"
    private const val KEY_STRIP_METADATA = "strip_metadata"

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

    private val _accent = MutableStateFlow(AccentChoice.INDIGO)
    val accent: StateFlow<AccentChoice> = _accent.asStateFlow()

    private val _defaultQuality = MutableStateFlow(PdfQuality.BALANCED)
    /** Quality preselected for new PDFs. */
    val defaultQuality: StateFlow<PdfQuality> = _defaultQuality.asStateFlow()

    private val _defaultPageSize = MutableStateFlow("A4")
    /** One of "A4", "Letter", "Original". */
    val defaultPageSize: StateFlow<String> = _defaultPageSize.asStateFlow()

    private val _autoClean = MutableStateFlow(true)
    val autoClean: StateFlow<Boolean> = _autoClean.asStateFlow()

    private val _stripMetadata = MutableStateFlow(true)
    /** Leaves author, subject, keywords and custom dates out of generated PDFs. */
    val stripMetadata: StateFlow<Boolean> = _stripMetadata.asStateFlow()

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
        _accent.value = AccentChoice.entries.firstOrNull { it.name == p.getString(KEY_ACCENT, null) } ?: AccentChoice.INDIGO
        _defaultQuality.value = PdfQuality.entries.firstOrNull { it.name == p.getString(KEY_DEFAULT_QUALITY, null) } ?: PdfQuality.BALANCED
        _defaultPageSize.value = p.getString(KEY_DEFAULT_PAGE_SIZE, null)?.takeIf { it in setOf("A4", "Letter", "Original") } ?: "A4"
        _autoClean.value = p.getBoolean(KEY_AUTO_CLEAN, true)
        _stripMetadata.value = p.getBoolean(KEY_STRIP_METADATA, true)
    }

    fun setAccent(choice: AccentChoice) {
        _accent.value = choice
        prefs?.edit()?.putString(KEY_ACCENT, choice.name)?.apply()
    }

    fun setDefaultQuality(quality: PdfQuality) {
        _defaultQuality.value = quality
        prefs?.edit()?.putString(KEY_DEFAULT_QUALITY, quality.name)?.apply()
    }

    fun setDefaultPageSize(size: String) {
        _defaultPageSize.value = size
        prefs?.edit()?.putString(KEY_DEFAULT_PAGE_SIZE, size)?.apply()
    }

    fun setAutoClean(enabled: Boolean) {
        _autoClean.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_CLEAN, enabled)?.apply()
    }

    fun setStripMetadata(enabled: Boolean) {
        _stripMetadata.value = enabled
        prefs?.edit()?.putBoolean(KEY_STRIP_METADATA, enabled)?.apply()
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
