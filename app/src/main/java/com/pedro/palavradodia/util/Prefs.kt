package com.pedro.palavradodia.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("palavra_prefs", Context.MODE_PRIVATE)

    var notificationHour: Int
        get() = sp.getInt("hour", 9)
        set(value) = sp.edit().putInt("hour", value).apply()

    var notificationMinute: Int
        get() = sp.getInt("minute", 0)
        set(value) = sp.edit().putInt("minute", value).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(value) = sp.edit().putBoolean("onboarding_done", value).apply()

    // --- Preferências de conteúdo ---

    /** Quantas palavras novas gerar por dia (1 a 3). */
    var wordsPerDay: Int
        get() = sp.getInt("words_per_day", 1).coerceIn(1, 3)
        set(value) = sp.edit().putInt("words_per_day", value.coerceIn(1, 3)).apply()

    var showFrances: Boolean
        get() = sp.getBoolean("show_frances", true)
        set(value) = sp.edit().putBoolean("show_frances", value).apply()

    var showIngles: Boolean
        get() = sp.getBoolean("show_ingles", true)
        set(value) = sp.edit().putBoolean("show_ingles", value).apply()

    // --- Preferências de aparência ---

    /** 0 = pequena, 1 = média (padrão), 2 = grande */
    var fontSizeOption: Int
        get() = sp.getInt("font_size_option", 1)
        set(value) = sp.edit().putInt("font_size_option", value).apply()

    val fontScale: Float
        get() = when (fontSizeOption) {
            0 -> 0.88f
            2 -> 1.18f
            else -> 1.0f
        }

    /** Usa as constantes de AppCompatDelegate.MODE_NIGHT_*. */
    var themeMode: Int
        get() = sp.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = sp.edit().putInt("theme_mode", value).apply()

    // --- Preferências de notificação ---

    var vibrarAoNotificar: Boolean
        get() = sp.getBoolean("vibrar_notificacao", true)
        set(value) = sp.edit().putBoolean("vibrar_notificacao", value).apply()
}
