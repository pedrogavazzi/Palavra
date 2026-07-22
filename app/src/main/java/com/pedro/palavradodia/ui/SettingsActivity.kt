package com.pedro.palavradodia.ui

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.pedro.palavradodia.databinding.ActivitySettingsBinding
import com.pedro.palavradodia.scheduler.AlarmScheduler
import com.pedro.palavradodia.util.Prefs

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Configurações"

        prefs = Prefs(this)

        setupTabs()
        setupHorario()
        setupPreferencias()
    }

    private fun setupTabs() {
        binding.btnTabHorario.setOnClickListener { showTab(horario = true) }
        binding.btnTabPreferencias.setOnClickListener { showTab(horario = false) }
    }

    private fun showTab(horario: Boolean) {
        binding.sectionHorario.visibility = if (horario) android.view.View.VISIBLE else android.view.View.GONE
        binding.sectionPreferencias.visibility = if (horario) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnTabHorario.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                resources.getColor(if (horario) com.pedro.palavradodia.R.color.primary else android.R.color.darker_gray, theme)
            )
        binding.btnTabPreferencias.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                resources.getColor(if (!horario) com.pedro.palavradodia.R.color.primary else android.R.color.darker_gray, theme)
            )
    }

    private fun setupHorario() {
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

        binding.btnSaveHorario.setOnClickListener {
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
        }
    }

    private fun setupPreferencias() {
        when (prefs.wordsPerDay) {
            1 -> binding.radio1Word.isChecked = true
            2 -> binding.radio2Words.isChecked = true
            3 -> binding.radio3Words.isChecked = true
        }
        when (prefs.fontSizeOption) {
            0 -> binding.radioFontSmall.isChecked = true
            2 -> binding.radioFontLarge.isChecked = true
            else -> binding.radioFontMedium.isChecked = true
        }
        when (prefs.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.radioThemeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.radioThemeDark.isChecked = true
            else -> binding.radioThemeAuto.isChecked = true
        }
        binding.switchFrances.isChecked = prefs.showFrances
        binding.switchIngles.isChecked = prefs.showIngles
        binding.switchVibrar.isChecked = prefs.vibrarAoNotificar

        binding.btnSavePreferencias.setOnClickListener {
            prefs.wordsPerDay = when (binding.radioWordsPerDay.checkedRadioButtonId) {
                com.pedro.palavradodia.R.id.radio2Words -> 2
                com.pedro.palavradodia.R.id.radio3Words -> 3
                else -> 1
            }
            prefs.fontSizeOption = when (binding.radioFontSize.checkedRadioButtonId) {
                com.pedro.palavradodia.R.id.radioFontSmall -> 0
                com.pedro.palavradodia.R.id.radioFontLarge -> 2
                else -> 1
            }
            prefs.themeMode = when (binding.radioTheme.checkedRadioButtonId) {
                com.pedro.palavradodia.R.id.radioThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                com.pedro.palavradodia.R.id.radioThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.showFrances = binding.switchFrances.isChecked
            prefs.showIngles = binding.switchIngles.isChecked
            prefs.vibrarAoNotificar = binding.switchVibrar.isChecked

            Toast.makeText(this, "Preferências salvas", Toast.LENGTH_SHORT).show()
            recreate() // aplica tamanho de fonte e tema imediatamente nesta tela
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
