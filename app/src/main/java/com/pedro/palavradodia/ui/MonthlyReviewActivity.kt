package com.pedro.palavradodia.ui

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.databinding.ActivityMonthlyReviewBinding
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.launch

/** Detalha as 4 semanas (até 28 palavras) que compõem uma revisão mensal específica. */
class MonthlyReviewActivity : BaseActivity() {
    private lateinit var binding: ActivityMonthlyReviewBinding
    private lateinit var repo: PalavraRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val monthIndex = intent.getIntExtra("monthIndex", 1)
        title = "Revisão do mês $monthIndex"
        repo = PalavraRepository(this)

        lifecycleScope.launch {
            val startCycle = (monthIndex - 1) * 4 + 1
            val endCycle = monthIndex * 4
            val history = repo.getLearnedHistory().filter { it.first.cycleNumber in startCycle..endCycle }

            if (history.isEmpty()) {
                addText("Nenhuma palavra aprendida neste mês ainda.")
                return@launch
            }

            val grouped = history.groupBy { it.first.cycleNumber }.toSortedMap()
            for ((cycleNum, items) in grouped) {
                addSectionTitle("Semana (ciclo $cycleNum)")
                for ((progress, word) in items) {
                    val status = when (progress.testCorrect) {
                        true -> "✔"
                        false -> "✘"
                        null -> "—"
                    }
                    addWordRow("$status ${word.palavra}: ${word.definicao}")
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun addSectionTitle(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 16f
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setPadding(0, 24, 0, 8)
        binding.container.addView(tv)
    }

    private fun addWordRow(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        tv.setPadding(0, 4, 0, 4)
        binding.container.addView(tv)
    }

    private fun addText(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 15f
        binding.container.addView(tv)
    }
}
