package com.pedro.palavradodia.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.databinding.FragmentAgendaBinding
import com.pedro.palavradodia.repository.PalavraRepository
import com.pedro.palavradodia.util.Prefs
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Aba "Agenda": uma linha por dia, do início do primeiro ciclo até o fim do ciclo atual
 * (incluindo dias futuros ainda sem palavra sorteada). A data/horário de cada linha reflete
 * o agendamento configurado pelo usuário. Palavras não aprendidas nunca têm o texto revelado.
 *
 * Recarrega sempre em onResume, garantindo que a palavra marcada como aprendida hoje
 * apareça imediatamente, sem precisar sair e voltar à aba.
 */
class AgendaFragment : Fragment() {
    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: PalavraRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        repo = PalavraRepository(requireContext())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadAgenda()
    }

    private fun loadAgenda() {
        binding.container.removeAllViews()
        lifecycleScope.launch {
            val prefs = Prefs(requireContext())
            val horario = String.format("%02dh%02d", prefs.notificationHour, prefs.notificationMinute)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

            val agenda = repo.getAgenda()
            if (_binding == null) return@launch
            if (agenda.isEmpty()) {
                addText("Sua agenda vai aparecer aqui assim que a primeira palavra do dia for gerada. Abra a aba \"Hoje\" para começar.")
                return@launch
            }

            var currentCycle: Int? = -1
            for (entry in agenda) {
                if (entry.cycleNumber != currentCycle) {
                    currentCycle = entry.cycleNumber
                    addSectionTitle(if (currentCycle != null) "Ciclo $currentCycle" else "Dia sem ciclo registrado")
                }
                val dateLabel = "${entry.date.format(formatter)} • $horario"
                when {
                    entry.future -> addRow(dateLabel, "Ainda por vir — a palavra será sorteada nesse dia", muted = true)
                    entry.notGenerated -> addRow(dateLabel, "— (nenhuma palavra foi gerada nesse dia)", muted = true)
                    entry.learned && entry.word != null -> addRow(dateLabel, "${entry.word.palavra} — ${entry.word.definicao}", muted = false)
                    else -> addRow(dateLabel, "Palavra ainda não aprendida", muted = true)
                }
            }
        }
    }

    private fun addSectionTitle(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 16f
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setPadding(0, 28, 0, 6)
        binding.container.addView(tv)
    }

    private fun addRow(date: String, content: String, muted: Boolean) {
        val tv = TextView(requireContext())
        tv.text = "$date\n$content"
        tv.textSize = 14f
        tv.setPadding(0, 8, 0, 8)
        if (muted) {
            tv.alpha = 0.6f
        }
        binding.container.addView(tv)
    }

    private fun addText(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 15f
        binding.container.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
