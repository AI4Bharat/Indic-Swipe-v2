package com.indicswipe.app

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_ENABLED_LANGS = "enabled_languages"
        private const val DEFAULT_LANGS = "hindi"
    }

    fun getEnabledLanguageIds(): List<String> {
        val encoded = prefs.getString(PREF_ENABLED_LANGS, DEFAULT_LANGS) ?: DEFAULT_LANGS
        return encoded.split(",").filter { it.isNotEmpty() }
    }

    fun setEnabledLanguageIds(ids: List<String>) {
        prefs.edit().putString(PREF_ENABLED_LANGS, ids.joinToString(",")).apply()
    }

    fun addLanguage(id: String) {
        val current = getEnabledLanguageIds().toMutableList()
        if (!current.contains(id)) {
            current.add(id)
            setEnabledLanguageIds(current)
        }
    }

    fun removeLanguage(id: String) {
        val current = getEnabledLanguageIds().toMutableList()
        if (current.size > 1) {
            current.remove(id)
            setEnabledLanguageIds(current)
        }
    }
}