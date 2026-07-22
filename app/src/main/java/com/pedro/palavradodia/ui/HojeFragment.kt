package com.pedro.palavradodia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.data.ProgressEntity
import com.pedro.palavradodia.databinding.FragmentHojeBinding
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.launch

/** Aba "Hoje": mostra a palavra do dia e permite marcá-la como aprendida. */
class HojeFragment : Fragment() {
    private var _binding: FragmentHojeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: PalavraRepository
    private var currentProgress: ProgressEntity? = null
    private var pendingCycleNumber: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHojeBinding.inflate(inflater, container, false)
        repo = PalavraRepository(requireContext())

        binding.btnMarkLearned.setOnClickListener { markLearned() }
        binding.btnTest.setOnClickListener {
            pendingCycleNumber?.let { cycle ->
                startActivity(
                    android.content.Intent(requireContext(), TestActivity::class.java)
                        .putExtra("cycleNumber", cycle)
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
        lifecycleScope.launch {
            val today = repo.getOrCreateTodayWord()
            if (_binding == null) return@launch
            if (today == null) {
                binding.tvWord.text = "Banco de palavras esgotado!"
                binding.tvClasse.text = ""
                binding.tvDefinition.text =
                    "Você aprendeu todas as palavras disponíveis. Novas palavras chegarão em uma futura atualização."
                binding.tvExample.text = ""
                binding.tvFrances.text = ""
                binding.tvIngles.text = ""
                binding.btnMarkLearned.visibility = View.GONE
                return@launch
            }

            currentProgress = today.progress
            binding.tvCycleInfo.text = "Ciclo ${today.cycle.cycleNumber} • Dia ${today.progress.positionInCycle} de 7"
            binding.tvWord.text = today.word.palavra
            binding.tvClasse.text = today.word.classe
            binding.tvDefinition.text = today.word.definicao
            binding.tvExample.text = "\u201C${today.word.exemplo}\u201D"
            binding.tvFrances.text = "Francês: ${today.word.frances}"
            binding.tvIngles.text = "Inglês: ${today.word.ingles}"

            binding.btnMarkLearned.visibility = if (today.progress.opened) View.GONE else View.VISIBLE
            binding.tvLearnedBadge.visibility = if (today.progress.opened) View.VISIBLE else View.GONE

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

    private fun markLearned() {
        val p = currentProgress ?: return
        lifecycleScope.launch {
            repo.markOpened(p)
            loadToday()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
