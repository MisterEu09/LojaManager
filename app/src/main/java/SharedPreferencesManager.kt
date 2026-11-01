package com.example.lojamamager.util

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesManager {
    private const val PREF_NAME = "app_preferences"
    private const val KEY_CURRENT_THEME = "current_theme"

    private lateinit var sharedPreferences: SharedPreferences
    private var _isInitialized = false // Adicione esta flag

    fun initialize(context: Context) {
        if (!_isInitialized) { // Garante que a inicialização ocorre apenas uma vez
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            _isInitialized = true
        }
    }

    fun isInitialized(): Boolean { // Novo método
        return _isInitialized
    }

    fun saveTheme(themeName: String) {
        if (!_isInitialized) {
            // Log de erro ou exceção se tentar salvar antes de inicializar
            return
        }
        sharedPreferences.edit().putString(KEY_CURRENT_THEME, themeName).apply()
    }

    fun getTheme(): String {
        if (!_isInitialized) {
            // Log de erro ou exceção se tentar obter antes de inicializar
            return "Default"
        }
        return sharedPreferences.getString(KEY_CURRENT_THEME, "Default") ?: "Default"
    }
}