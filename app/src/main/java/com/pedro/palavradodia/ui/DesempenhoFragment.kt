package com.pedro.palavradodia.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.databinding.FragmentDesempenhoBinding
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.launch

/** Aba "Desempenho": progresso geral e resumo por mês (4 ciclos). */
class DesempenhoFragment : Fragment() {
    private var _binding: FragmentDesempenhoBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: PalavraRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDesempenhoBinding.inflate(inflater, container, false)
        repo = PalavraRepository(requireContext())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        binding.container.removeAllViews()
        lifecycleScope.launch {
            val stats = repo.getStats()
            if (_binding == null) return@launch

            addBoldText("Progresso geral")
            addText("Palavras aprendidas: ${stats.learnedCount} de ${stats.totalWords}")
            addText("Palavras testadas: ${stats.testedCount}")
            addText("Acertos nos testes: ${stats.correctCount}")
            addText("Taxa de acerto geral: ${stats.accuracyPercent}%")

            val groups = repo.getMonthlyGroups()
            if (_binding == null) return@launch
            if (groups.isEmpty()) {
                addBoldText("\nRevisão mensal")
                addText("Ainda não há ciclos suficientes para gerar uma revisão mensal.")
                return@launch
            }

            addBoldText("\nRevisão mensal (toque para ver detalhes)")
            for (group in groups) {
                val doneCycles = group.cycles.count { it.testCompleted }
                val totalScore = group.cycles.sumOf { it.testScore ?: 0 }
                val totalMax = group.cycles.sumOf { it.testTotal ?: 0 }
                val pct = if (totalMax > 0) (totalScore * 100) / totalMax else 0

                val tv = TextView(requireContext())
                tv.text = "Mês ${group.monthIndex}: $doneCycles/${group.cycles.size} ciclos testados — aproveitamento $pct%"
                tv.textSize = 15f
                tv.setPadding(0, 14, 0, 14)
                tv.setOnClickListener {
                    startActivity(
                        Intent(requireContext(), MonthlyReviewActivity::class.java)
                            .putExtra("monthIndex", group.monthIndex)
                    )
                }
                binding.container.addView(tv)
            }
        }
    }

    private fun addBoldText(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 17f
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setPadding(0, 20, 0, 8)
        binding.container.addView(tv)
    }

    private fun addText(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 15f
        tv.setPadding(0, 4, 0, 4)
        binding.container.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
