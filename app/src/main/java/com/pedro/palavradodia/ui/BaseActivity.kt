package com.pedro.palavradodia.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.pedro.palavradodia.util.Prefs

/**
 * Base para todas as Activities: aplica o tamanho de fonte escolhido pelo usuário
 * (via override de Configuration.fontScale) e o tema claro/escuro/automático.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = Prefs(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = prefs.fontScale
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        AppCompatDelegate.setDefaultNightMode(Prefs(this).themeMode)
        super.onCreate(savedInstanceState)
    }
}
