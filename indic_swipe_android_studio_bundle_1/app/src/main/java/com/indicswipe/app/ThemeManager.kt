package com.indicswipe.app

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils


class ThemeManager(private val context: Context) {

    data class Theme(
        val name: String,
        val keyboardBg: Int,
        val keyBg: Int,
        val keyPressed: Int,
        val keyText: Int,
        val specialKeyBg: Int,
        val specialKeyPressed: Int,
        val specialKeyIcon: Int,
        val specialKeyText: Int,
        val accent: Int,
        val accentText: Int,
        val enterKeyBg: Int,
        val enterKeyIcon: Int,
        val textSecondary: Int,
        val keyStroke: Int,
        val previewBg: Int,
        val previewText: Int,
        val previewShadow: Int,
        val trailColor: Int,
        val trailGlowColor: Int,
        val keyGradientStart: Int,
        val keyGradientEnd: Int,
        val specialKeyGradientStart: Int,
        val specialKeyGradientEnd: Int,
        val keyBorder: Int,
        val keyShadowRadius: Float,
        val keyShadowDy: Float,
        val keyShadowColor: Int,
        val keyRadius: Float,
        val specialKeyRadius: Float,
        val suggestionBarBg: Int,
        val popupBg: Int,
        val popupStroke: Int,
        val suggestionBg: Int,
        val suggestionText: Int,
        val suggestionChipBg: Int,
        val suggestionChipRadius: Float,
        val suggestionChipPaddingH: Int,
        val suggestionChipPaddingV: Int,
        val divider: Int
    )

    companion object {
        const val PREF_NAME = "theme_prefs"
        const val KEY_THEME = "current_theme"
        const val KEY_SHOW_BORDERS = "show_borders"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var showBorders: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BORDERS, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_BORDERS, value).apply() }

    val currentTheme: Theme
        get() = getThemeByName(prefs.getString(KEY_THEME, "Dark") ?: "Dark")

    fun setTheme(name: String) {
        prefs.edit().putString(KEY_THEME, name).apply()
    }

    val themes: Map<String, Theme>
        get() = getAllThemes().associateBy { it.name }

    val currentThemeKey: String
        get() = prefs.getString(KEY_THEME, "Dark") ?: "Dark"

    fun getAllThemes(): List<Theme> = listOf(
        createLightTheme(),
        createDarkTheme(),
        createAmoledTheme()
    )

    private fun getThemeByName(name: String): Theme {
        return getAllThemes().find { it.name == name } ?: createDarkTheme()
    }

    private fun createLightTheme(): Theme {
        val accent = Color.parseColor("#FF5722")
        val keyboardBg = Color.parseColor("#EBEDED")
        val keyBg = Color.WHITE
        val keyText = Color.parseColor("#1A1C1E")
        val secondary = Color.parseColor("#5F6368")
        val specialBg = Color.parseColor("#D8DBE0")
        
        return Theme(
            name = "Light",
            keyboardBg = keyboardBg,
            keyBg = keyBg,
            keyPressed = Color.parseColor("#E8EAED"), 
            keyText = keyText,
            specialKeyBg = specialBg,
            specialKeyPressed = Color.parseColor("#D1D5DB"), 
            specialKeyIcon = keyText,
            specialKeyText = keyText,
            accent = accent,
            accentText = Color.WHITE,
            enterKeyBg = accent,
            enterKeyIcon = Color.WHITE,
            textSecondary = secondary,
            keyStroke = Color.TRANSPARENT,
            previewBg = Color.WHITE,
            previewText = keyText,
            previewShadow = Color.parseColor("#26000000"),
            trailColor = accent,
            trailGlowColor = ColorUtils.setAlphaComponent(accent, 60),
            keyGradientStart = keyBg,
            keyGradientEnd = keyBg,
            specialKeyGradientStart = specialBg,
            specialKeyGradientEnd = specialBg,
            keyBorder = Color.TRANSPARENT,
            keyShadowRadius = 3.0f,
            keyShadowDy = 1.5f,
            keyShadowColor = Color.parseColor("#30000000"), 
            keyRadius = 8f,
            specialKeyRadius = 8f,
            suggestionBarBg = keyboardBg,
            popupBg = Color.WHITE,
            popupStroke = Color.parseColor("#D1D5DB"),
            suggestionBg = keyboardBg,
            suggestionText = keyText,
            suggestionChipBg = Color.WHITE,
            suggestionChipRadius = 8f,
            suggestionChipPaddingH = 18,
            suggestionChipPaddingV = 10,
            divider = Color.parseColor("#D1D5DB")
        )
    }

    private fun createDarkTheme(): Theme {
        val accent = Color.parseColor("#FF5722")
        val keyboardBg = Color.parseColor("#1B1B1E")
        val keyBg = Color.parseColor("#353538")
        val keyText = Color.WHITE
        val secondary = Color.parseColor("#9AA0A6")
        val specialBg = Color.parseColor("#2B2B2E")
        
        return Theme(
            name = "Dark",
            keyboardBg = keyboardBg,
            keyBg = keyBg,
            keyPressed = Color.parseColor("#424548"),
            keyText = keyText,
            specialKeyBg = specialBg,
            specialKeyPressed = Color.parseColor("#3C4043"),
            specialKeyIcon = Color.WHITE,
            specialKeyText = Color.WHITE,
            accent = accent,
            accentText = Color.WHITE,
            enterKeyBg = accent,
            enterKeyIcon = Color.WHITE,
            textSecondary = secondary,
            keyStroke = Color.TRANSPARENT,
            previewBg = Color.parseColor("#3C4043"),
            previewText = Color.WHITE,
            previewShadow = Color.parseColor("#40000000"),
            trailColor = accent,
            trailGlowColor = ColorUtils.setAlphaComponent(accent, 80),
            keyGradientStart = keyBg,
            keyGradientEnd = keyBg,
            specialKeyGradientStart = specialBg,
            specialKeyGradientEnd = specialBg,
            keyBorder = Color.TRANSPARENT,
            keyShadowRadius = 3.0f,
            keyShadowDy = 1.5f,
            keyShadowColor = Color.parseColor("#50000000"),
            keyRadius = 8f,
            specialKeyRadius = 8f,
            suggestionBarBg = keyboardBg,
            popupBg = Color.parseColor("#202124"),
            popupStroke = Color.parseColor("#3C4043"),
            suggestionBg = keyboardBg,
            suggestionText = Color.WHITE,
            suggestionChipBg = keyBg,
            suggestionChipRadius = 8f,
            suggestionChipPaddingH = 18,
            suggestionChipPaddingV = 10,
            divider = Color.parseColor("#3C4043")
        )
    }

    private fun createAmoledTheme(): Theme {
        val accent = Color.parseColor("#FF5722")
        val keyboardBg = Color.BLACK
        val keyBg = Color.parseColor("#1B1B1E")
        val keyText = Color.WHITE
        val secondary = Color.parseColor("#80868B")
        val specialBg = Color.parseColor("#111111")
        
        return Theme(
            name = "AMOLED",
            keyboardBg = keyboardBg,
            keyBg = keyBg,
            keyPressed = Color.parseColor("#2C2C2E"),
            keyText = keyText,
            specialKeyBg = specialBg,
            specialKeyPressed = Color.parseColor("#1C1C1E"),
            specialKeyIcon = Color.WHITE,
            specialKeyText = Color.WHITE,
            accent = accent,
            accentText = Color.WHITE,
            enterKeyBg = accent,
            enterKeyIcon = Color.WHITE,
            textSecondary = secondary,
            keyStroke = Color.TRANSPARENT,
            previewBg = Color.parseColor("#1C1C1E"),
            previewText = Color.WHITE,
            previewShadow = Color.parseColor("#60000000"),
            trailColor = accent,
            trailGlowColor = ColorUtils.setAlphaComponent(accent, 90),
            keyGradientStart = keyBg,
            keyGradientEnd = keyBg,
            specialKeyGradientStart = specialBg,
            specialKeyGradientEnd = specialBg,
            keyBorder = Color.TRANSPARENT,
            keyShadowRadius = 0f,
            keyShadowDy = 0f,
            keyShadowColor = Color.TRANSPARENT,
            keyRadius = 8f,
            specialKeyRadius = 8f,
            suggestionBarBg = keyboardBg,
            popupBg = Color.BLACK,
            popupStroke = Color.parseColor("#1C1C1E"),
            suggestionBg = keyboardBg,
            suggestionText = Color.WHITE,
            suggestionChipBg = keyBg,
            suggestionChipRadius = 8f,
            suggestionChipPaddingH = 18,
            suggestionChipPaddingV = 10,
            divider = Color.parseColor("#1C1C1E")
        )
    }
}