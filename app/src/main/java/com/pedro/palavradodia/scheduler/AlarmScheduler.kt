package com.pedro.palavradodia.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pedro.palavradodia.util.Prefs
import java.util.Calendar

/**
 * Agenda o alarme exato diário. Usa AlarmManager (não apenas WorkManager) porque
 * o usuário escolhe um horário específico e isso precisa disparar mesmo com o
 * dispositivo ocioso ou com otimização de bateria ativa.
 */
object AlarmScheduler {
    private const val REQUEST_CODE = 1001

    fun schedule(context: Context) {
        val prefs = Prefs(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.notificationHour)
            set(Calendar.MINUTE, prefs.notificationMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.cancel(pendingIntent)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Em alguns dispositivos/versões, alarme exato pode exigir permissão especial
            // negada pelo usuário; nesse caso caímos para um alarme aproximado.
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
