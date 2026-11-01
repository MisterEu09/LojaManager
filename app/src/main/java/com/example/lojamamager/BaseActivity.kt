package com.example.lojamamager

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        const val PREF_THEME_KEY = "app_theme"
        const val THEME_DEFAULT = "default"
        const val THEME_SERIOUS = "serious"
        const val THEME_VIBRANT = "vibrant"
        const val THEME_PASTEL = "pastel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyAppTheme() {
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedTheme = sharedPrefs.getString(PREF_THEME_KEY, THEME_DEFAULT)

        when (savedTheme) {
            THEME_SERIOUS -> setTheme(R.style.Theme_LojaManager_Serious)
            THEME_VIBRANT -> setTheme(R.style.Theme_LojaManager_Vibrant)
            THEME_PASTEL -> setTheme(R.style.Theme_LojaManager_Pastel) // *** ADICIONADO ***
            THEME_DEFAULT -> setTheme(R.style.Theme_LojaManager)
            else -> setTheme(R.style.Theme_LojaManager)
        }
    }


}