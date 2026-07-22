package com.pedro.palavradodia.ui

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.palavradodia.databinding.ActivitySettingsBinding
import com.pedro.palavradodia.scheduler.AlarmScheduler
import com.pedro.palavradodia.util.Prefs

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Configurações"

        val prefs = Prefs(this)
        binding.timePicker.setIs24HourView(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timePicker.hour = prefs.notificationHour
            binding.timePicker.minute = prefs.notificationMinute
        } else {
            @Suppress("DEPRECATION")
            binding.timePicker.currentHour = prefs.notificationHour
            @Suppress("DEPRECATION")
            binding.timePicker.currentMinute = prefs.notificationMinute
        }

        binding.btnSave.setOnClickListener {
            val hour: Int
            val minute: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = binding.timePicker.hour
                minute = binding.timePicker.minute
            } else {
                @Suppress("DEPRECATION")
                hour = binding.timePicker.currentHour
                @Suppress("DEPRECATION")
                minute = binding.timePicker.currentMinute
            }
            prefs.notificationHour = hour
            prefs.notificationMinute = minute
            // cancela o alarme anterior e cria um novo, evitando notificação duplicada no mesmo dia
            AlarmScheduler.cancel(this)
            AlarmScheduler.schedule(this)
            Toast.makeText(this, "Horário atualizado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
