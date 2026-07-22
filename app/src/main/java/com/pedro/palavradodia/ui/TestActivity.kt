package com.pedro.palavradodia.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.data.AppDatabase
import com.pedro.palavradodia.data.CycleEntity
import com.pedro.palavradodia.data.ProgressEntity
import com.pedro.palavradodia.data.WordEntity
import com.pedro.palavradodia.databinding.ActivityTestBinding
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.launch

/**
 * Teste de múltipla escolha com as palavras aprendidas em um ciclo de 7 dias.
 * Palavras não marcadas como aprendidas nunca entram no teste.
 */
class TestActivity : BaseActivity() {
    private lateinit var binding: ActivityTestBinding
    private lateinit var repo: PalavraRepository
    private var cycleNumber = 0
    private lateinit var cycle: CycleEntity
    private var questions: List<Pair<ProgressEntity, WordEntity>> = emptyList()
    private var currentIndex = 0
    private val results = mutableMapOf<Long, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Teste semanal"

        repo = PalavraRepository(this)
        cycleNumber = intent.getIntExtra("cycleNumber", 0)

        lifecycleScope.launch {
            val c = AppDatabase.getInstance(this@TestActivity).cycleDao().getByNumber(cycleNumber)
            if (c == null) {
                finish()
                return@launch
            }
            cycle = c
            questions = repo.getTestWords(cycleNumber)
            if (questions.isEmpty()) {
                binding.tvQuestion.text = "Nenhuma palavra foi marcada como aprendida neste ciclo, então não há o que testar."
                binding.optionsContainer.visibility = View.GONE
                return@launch
            }
            showQuestion()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showQuestion() {
        if (currentIndex >= questions.size) {
            finishTest()
            return
        }
        val (progress, word) = questions[currentIndex]
        binding.tvProgress.text = "Pergunta ${currentIndex + 1} de ${questions.size}"
        binding.tvQuestion.text = "Qual o significado de \u201C${word.palavra}\u201D?"

        lifecycleScope.launch {
            val distractors = repo.getDistractors(word.id, 3)
            val options = (distractors + word).shuffled()
            binding.optionsContainer.removeAllViews()
            for (opt in options) {
                val btn = Button(this@TestActivity)
                btn.text = opt.definicao
                btn.isAllCaps = false
                btn.setOnClickListener { onAnswer(progress, opt.id == word.id) }
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = 12
                btn.layoutParams = params
                binding.optionsContainer.addView(btn)
            }
        }
    }

    private fun onAnswer(progress: ProgressEntity, correct: Boolean) {
        results[progress.progressId] = correct
        currentIndex++
        showQuestion()
    }

    private fun finishTest() {
        lifecycleScope.launch {
            repo.saveTestResult(cycle, results)
            val acertos = results.values.count { it }
            binding.tvProgress.text = "Teste concluído!"
            binding.tvQuestion.text = "Você acertou $acertos de ${results.size} palavras."
            binding.optionsContainer.removeAllViews()
            val btn = Button(this@TestActivity)
            btn.text = "Voltar"
            btn.setOnClickListener { finish() }
            binding.optionsContainer.addView(btn)
        }
    }
}
