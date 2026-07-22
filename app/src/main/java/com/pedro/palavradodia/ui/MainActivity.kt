package com.pedro.palavradodia.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.pedro.palavradodia.databinding.ActivityMainBinding
import com.pedro.palavradodia.scheduler.AlarmScheduler
import com.pedro.palavradodia.util.Prefs

/** Host das 4 abas (Hoje, Agenda, Treino, Desempenho) + acesso rápido a Configurações. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = Prefs(this)
        if (!prefs.onboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        requestNotificationPermissionIfNeeded()
        AlarmScheduler.schedule(this)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                com.pedro.palavradodia.R.id.nav_hoje -> HojeFragment()
                com.pedro.palavradodia.R.id.nav_agenda -> AgendaFragment()
                com.pedro.palavradodia.R.id.nav_treino -> TreinoFragment()
                com.pedro.palavradodia.R.id.nav_desempenho -> DesempenhoFragment()
                else -> return@setOnItemSelectedListener false
            }
            showFragment(fragment)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = com.pedro.palavradodia.R.id.nav_hoje
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(com.pedro.palavradodia.R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}
