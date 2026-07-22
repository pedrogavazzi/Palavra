package com.pedro.palavradodia.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.pedro.palavradodia.databinding.ActivityOnboardingBinding
import com.pedro.palavradodia.scheduler.AlarmScheduler
import com.pedro.palavradodia.util.Prefs

class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.timePicker.setIs24HourView(true)

        binding.btnStart.setOnClickListener {
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

            val prefs = Prefs(this)
            prefs.notificationHour = hour
            prefs.notificationMinute = minute
            prefs.onboardingDone = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }

            AlarmScheduler.schedule(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
