package com.pedro.palavradodia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reagenda o alarme diário após reinício do dispositivo, já que o AlarmManager
 * perde todos os alarmes agendados quando o aparelho é desligado.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.schedule(context.applicationContext)
        }
    }
}
