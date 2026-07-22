package com.pedro.palavradodia.scheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pedro.palavradodia.R
import com.pedro.palavradodia.data.WordEntity
import com.pedro.palavradodia.ui.MainActivity
import com.pedro.palavradodia.util.Prefs

object NotificationHelper {
    const val CHANNEL_ID = "palavra_do_dia_channel"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Palavra do dia",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificação diária com a(s) nova(s) palavra(s) do vocabulário"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /** Mostra uma notificação para 1 ou mais palavras geradas no dia. */
    fun showWordNotification(context: Context, words: List<WordEntity>) {
        if (words.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        val bigText: String
        if (words.size == 1) {
            title = "Palavra do dia: ${words[0].palavra}"
            text = words[0].definicao
            bigText = words[0].definicao
        } else {
            title = "${words.size} palavras novas hoje"
            text = words.joinToString(", ") { it.palavra }
            bigText = words.joinToString("\n") { "${it.palavra} — ${it.definicao}" }
        }

        val prefs = Prefs(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (prefs.vibrarAoNotificar) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        NotificationManagerCompat.from(context).notify(1, builder.build())
    }
}
