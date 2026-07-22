package com.pedro.palavradodia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.data.WordEntity
import com.pedro.palavradodia.databinding.FragmentTreinoBinding
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.launch

/**
 * Aba "Treino": prática livre (não altera as estatísticas oficiais) usando apenas
 * palavras já aprendidas. O usuário escolhe se quer relacionar a palavra com o
 * significado, com o francês ou com o inglês.
 */
class TreinoFragment : Fragment() {
    private var _binding: FragmentTreinoBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: PalavraRepository

    private enum class Modo { SIGNIFICADO, FRANCES, INGLES }

    private var modo = Modo.SIGNIFICADO
    private var pool: List<WordEntity> = emptyList()
    private var currentIndex = 0
    private var acertos = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTreinoBinding.inflate(inflater, container, false)
        repo = PalavraRepository(requireContext())

        binding.btnModoSignificado.setOnClickListener { iniciarTreino(Modo.SIGNIFICADO) }
        binding.btnModoFrances.setOnClickListener { iniciarTreino(Modo.FRANCES) }
        binding.btnModoIngles.setOnClickListener { iniciarTreino(Modo.INGLES) }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // volta sempre para a seleção de modo ao reabrir a aba, evitando retomar
        // um treino "no meio" de forma confusa
        mostrarSeletorDeModo()
    }

    private fun mostrarSeletorDeModo() {
        if (_binding == null) return
        binding.modeSelector.visibility = View.VISIBLE
        binding.quizContainer.visibility = View.GONE
    }

    private fun iniciarTreino(m: Modo) {
        modo = m
        lifecycleScope.launch {
            val learned = repo.getLearnedWords()
            if (_binding == null) return@launch
            if (learned.size < 4) {
                Toast.makeText(
                    requireContext(),
                    "Aprenda pelo menos 4 palavras para treinar nesse modo.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            pool = learned.shuffled()
            currentIndex = 0
            acertos = 0
            binding.modeSelector.visibility = View.GONE
            binding.quizContainer.visibility = View.VISIBLE
            showQuestion()
        }
    }

    private fun respostaCorreta(word: WordEntity): String = when (modo) {
        Modo.SIGNIFICADO -> word.definicao
        Modo.FRANCES -> word.frances
        Modo.INGLES -> word.ingles
    }

    private fun perguntaLabel(): String = when (modo) {
        Modo.SIGNIFICADO -> "Qual o significado de"
        Modo.FRANCES -> "Qual a tradução em francês de"
        Modo.INGLES -> "Qual a tradução em inglês de"
    }

    private fun showQuestion() {
        if (_binding == null) return
        if (currentIndex >= pool.size) {
            finishTraining()
            return
        }
        val word = pool[currentIndex]
        binding.tvProgress.text = "Pergunta ${currentIndex + 1} de ${pool.size} • Acertos: $acertos"
        binding.tvQuestion.text = "${perguntaLabel()} \u201C${word.palavra}\u201D?"

        val distractors = pool.filter { it.id != word.id }.shuffled().take(3)
        val options = (distractors + word).shuffled()
        binding.optionsContainer.removeAllViews()
        for (opt in options) {
            val btn = Button(requireContext())
            btn.text = respostaCorreta(opt)
            btn.isAllCaps = false
            btn.setOnClickListener { onAnswer(opt.id == word.id) }
            binding.optionsContainer.addView(btn)
        }
    }

    private fun onAnswer(correct: Boolean) {
        if (correct) acertos++
        currentIndex++
        showQuestion()
    }

    private fun finishTraining() {
        if (_binding == null) return
        binding.tvProgress.text = "Treino concluído!"
        binding.tvQuestion.text = "Você acertou $acertos de ${pool.size}."
        binding.optionsContainer.removeAllViews()
        val btn = Button(requireContext())
        btn.text = "Treinar novamente"
        btn.setOnClickListener { mostrarSeletorDeModo() }
        binding.optionsContainer.addView(btn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
