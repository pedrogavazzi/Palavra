package com.pedro.palavradodia.util

import android.content.Context

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
}
