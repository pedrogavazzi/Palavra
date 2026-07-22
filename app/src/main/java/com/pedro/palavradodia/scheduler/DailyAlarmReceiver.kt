package com.pedro.palavradodia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Disparado pelo AlarmManager no horário escolhido pelo usuário. Garante que a
 * palavra do dia exista (criando-a se necessário) e mostra a notificação.
 * Também reagenda o próprio alarme para o dia seguinte.
 */
class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PalavraRepository(context.applicationContext)
                val today = repo.getOrCreateTodayWord()
                if (today != null) {
                    NotificationHelper.showWordNotification(context.applicationContext, today.word)
                }
            } finally {
                AlarmScheduler.schedule(context.applicationContext)
                pendingResult.finish()
            }
        }
    }
}
