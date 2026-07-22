package com.pedro.palavradodia

import android.app.Application
import com.pedro.palavradodia.scheduler.NotificationHelper

class PalavraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
