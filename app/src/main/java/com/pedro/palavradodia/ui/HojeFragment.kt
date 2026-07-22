package com.pedro.palavradodia.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.data.ProgressEntity
import com.pedro.palavradodia.databinding.FragmentHojeBinding
import com.pedro.palavradodia.databinding.ItemWordCardBinding
import com.pedro.palavradodia.repository.PalavraRepository
import com.pedro.palavradodia.util.Prefs
import kotlinx.coroutines.launch

/** Aba "Hoje": mostra a(s) palavra(s) do dia (1 a 3, conforme configuração) e o teste pendente. */
class HojeFragment : Fragment() {
    private var _binding: FragmentHojeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: PalavraRepository
    private var pendingCycleNumber: Int? = null
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHojeBinding.inflate(inflater, container, false)
        repo = PalavraRepository(requireContext())

        binding.btnTest.setOnClickListener {
            pendingCycleNumber?.let { cycle ->
                startActivity(
                    Intent(requireContext(), TestActivity::class.java).putExtra("cycleNumber", cycle)
                )
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadToday()
    }

    private fun loadToday() {
        if (isLoading) return // evita disparar buscas concorrentes se onResume for chamado de novo rapidamente
        isLoading = true
        lifecycleScope.launch {
            val today = repo.getOrCreateTodayWords()
            isLoading = false
            if (_binding == null) return@launch

            binding.wordsContainer.removeAllViews()

            if (today.isEmpty()) {
                val infoBinding = ItemWordCardBinding.inflate(LayoutInflater.from(requireContext()), binding.wordsContainer, false)
                infoBinding.tvWord.text = "Banco de palavras esgotado!"
                infoBinding.tvClasse.text = ""
                infoBinding.tvDefinition.text =
                    "Você aprendeu todas as palavras disponíveis. Novas palavras chegarão em uma futura atualização."
                infoBinding.tvExample.text = ""
                infoBinding.divider.visibility = View.GONE
                infoBinding.tvFrances.text = ""
                infoBinding.tvIngles.text = ""
                infoBinding.btnMarkLearned.visibility = View.GONE
                binding.wordsContainer.addView(infoBinding.root)
                binding.btnTest.visibility = View.GONE
                return@launch
            }

            binding.tvCycleInfo.text = "Ciclo ${today.first().cycle.cycleNumber} • Dia ${today.first().progress.positionInCycle} de 7"

            val prefs = Prefs(requireContext())
            for (todayWord in today) {
                val cardBinding = ItemWordCardBinding.inflate(LayoutInflater.from(requireContext()), binding.wordsContainer, false)
                bindCard(cardBinding, todayWord.word, todayWord.progress, prefs)
                binding.wordsContainer.addView(cardBinding.root)
            }

            val pending = repo.pendingTest()
            if (pending != null) {
                binding.btnTest.visibility = View.VISIBLE
                binding.btnTest.text = "Fazer teste do ciclo ${pending.cycleNumber}"
                pendingCycleNumber = pending.cycleNumber
            } else {
                binding.btnTest.visibility = View.GONE
                pendingCycleNumber = null
            }
        }
    }

    private fun bindCard(
        cardBinding: ItemWordCardBinding,
        word: com.pedro.palavradodia.data.WordEntity,
        progress: ProgressEntity,
        prefs: Prefs
    ) {
        cardBinding.tvWord.text = word.palavra
        cardBinding.tvClasse.text = word.classe
        cardBinding.tvDefinition.text = word.definicao
        cardBinding.tvExample.text = "\u201C${word.exemplo}\u201D"

        val mostrarFrances = prefs.showFrances
        val mostrarIngles = prefs.showIngles
        cardBinding.tvFrances.visibility = if (mostrarFrances) View.VISIBLE else View.GONE
        cardBinding.tvIngles.visibility = if (mostrarIngles) View.VISIBLE else View.GONE
        cardBinding.divider.visibility = if (mostrarFrances || mostrarIngles) View.VISIBLE else View.GONE
        cardBinding.tvFrances.text = "Francês: ${word.frances}"
        cardBinding.tvIngles.text = "Inglês: ${word.ingles}"

        cardBinding.btnMarkLearned.visibility = if (progress.opened) View.GONE else View.VISIBLE
        cardBinding.tvLearnedBadge.visibility = if (progress.opened) View.VISIBLE else View.GONE

        cardBinding.btnMarkLearned.setOnClickListener {
            lifecycleScope.launch {
                repo.markOpened(progress)
                loadToday()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
