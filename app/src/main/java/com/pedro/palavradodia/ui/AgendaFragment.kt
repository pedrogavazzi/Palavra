package com.pedro.palavradodia.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pedro.palavradodia.R
import com.pedro.palavradodia.databinding.FragmentAgendaBinding
import com.pedro.palavradodia.databinding.ItemAgendaCardBinding
import com.pedro.palavradodia.repository.PalavraRepository
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Aba "Agenda": um cartão por dia, da data mais ANTIGA para a mais NOVA — do início do
 * primeiro ciclo até o fim do ciclo atual (incluindo dias futuros ainda sem palavra sorteada).
 * Cada cartão mostra o horário real que estava agendado NAQUELE dia (não muda retroativamente
 * se o usuário alterar o horário depois). Palavras não aprendidas nunca têm o texto revelado.
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
                val dateLabel = "${entry.date.format(formatter)} • ${String.format("%02dh%02d", entry.notifiedHour, entry.notifiedMinute)}"
                val (content, colorRes, stripeColorRes) = when {
                    entry.future -> Triple("Ainda por vir — a palavra será sorteada nesse dia", R.color.text_secondary, R.color.accent)
                    entry.notGenerated -> Triple("— (nenhuma palavra foi gerada nesse dia)", R.color.text_secondary, R.color.error)
                    entry.learned && entry.word != null -> Triple("${entry.word.palavra} — ${entry.word.definicao}", R.color.text_primary, R.color.success)
                    else -> Triple("Palavra ainda não aprendida", R.color.text_secondary, R.color.text_secondary)
                }
                addCard(dateLabel, content, colorRes, stripeColorRes)
            }
        }
    }

    private fun addSectionTitle(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 16f
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
        tv.setPadding(0, 24, 0, 4)
        binding.container.addView(tv)
    }

    private fun addCard(date: String, content: String, contentColorRes: Int, stripeColorRes: Int) {
        val cardBinding = ItemAgendaCardBinding.inflate(LayoutInflater.from(requireContext()), binding.container, false)
        cardBinding.tvDate.text = date
        cardBinding.tvContent.text = content
        cardBinding.tvContent.setTextColor(ContextCompat.getColor(requireContext(), contentColorRes))
        cardBinding.statusStripe.setBackgroundColor(ContextCompat.getColor(requireContext(), stripeColorRes))
        binding.container.addView(cardBinding.root)
    }

    private fun addText(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 15f
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        binding.container.addView(tv)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
