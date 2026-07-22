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
 * Aba "Agenda": uma linha por dia, desde o início do primeiro ciclo até hoje.
 * A data/horário de cada linha reflete o agendamento configurado pelo usuário.
 * Palavras não aprendidas nunca têm o texto revelado.
 *
 * Recarrega sempre em onResume (não só em onCreateView), corrigindo o bug em que
 * uma palavra marcada como aprendida não aparecia até reabrir a aba do zero.
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
                addText("Sua agenda vai aparecer aqui assim que a primeira palavra do dia for gerada.")
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
                    entry.notGenerated -> addRow(dateLabel, "— (nenhuma palavra foi gerada nesse dia)")
                    entry.learned && entry.word != null -> addRow(dateLabel, "${entry.word.palavra} — ${entry.word.definicao}")
                    else -> addRow(dateLabel, "Palavra ainda não aprendida")
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

    private fun addRow(date: String, content: String) {
        val tv = TextView(requireContext())
        tv.text = "$date\n$content"
        tv.textSize = 14f
        tv.setPadding(0, 8, 0, 8)
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
