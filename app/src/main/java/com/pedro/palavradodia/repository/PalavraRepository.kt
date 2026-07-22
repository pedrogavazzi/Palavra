package com.pedro.palavradodia.repository

import android.content.Context
import com.pedro.palavradodia.data.AppDatabase
import com.pedro.palavradodia.data.AssetWordLoader
import com.pedro.palavradodia.data.CycleEntity
import com.pedro.palavradodia.data.ProgressEntity
import com.pedro.palavradodia.data.WordEntity
import com.pedro.palavradodia.util.DateUtils
import com.pedro.palavradodia.util.Prefs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Camada central de regras de negócio do app:
 * - decide quais palavras mostrar em cada dia (1 a 3, conforme configuração)
 * - controla o ciclo de 7 dias (iniciado no dia em que o usuário abre a primeira palavra)
 * - gera testes semanais e agrupamentos mensais (4 ciclos)
 * - garante que nenhuma palavra ainda não aprendida apareça em agenda/estatísticas/testes
 *
 * O acesso de escrita ao "dia atual" é serializado por um Mutex global (companion object),
 * compartilhado por todas as instâncias do repositório, evitando condições de corrida.
 *
 * Mudanças de configuração (horário, palavras por dia etc.) NUNCA alteram registros já
 * gerados no passado — cada palavra guarda o horário que estava configurado quando ela foi
 * criada (notifiedHour/notifiedMinute). Só as próximas palavras/notificações usam a nova config.
 */
class PalavraRepository(private val context: Context) {

    companion object {
        private val mutex = Mutex()
    }

    private val db = AppDatabase.getInstance(context)
    private val wordDao = db.wordDao()
    private val progressDao = db.progressDao()
    private val cycleDao = db.cycleDao()
    private val prefs = Prefs(context)

    data class TodayWord(val word: WordEntity, val progress: ProgressEntity, val cycle: CycleEntity)

    data class Stats(
        val learnedCount: Int,
        val totalWords: Int,
        val testedCount: Int,
        val correctCount: Int,
        val accuracyPercent: Int
    )

    data class MonthGroup(val monthIndex: Int, val cycles: List<CycleEntity>)

    data class AgendaEntry(
        val date: LocalDate,
        val cycleNumber: Int?,
        val learned: Boolean,
        val word: WordEntity?,
        /** true quando o dia já passou e nenhuma palavra chegou a ser gerada (ex.: notificação não disparou) */
        val notGenerated: Boolean,
        /** true quando é um dia futuro do ciclo atual, cuja palavra ainda nem foi sorteada */
        val future: Boolean,
        /** horário real do alerta naquele dia (histórico, não muda se o usuário mudar o horário depois) */
        val notifiedHour: Int,
        val notifiedMinute: Int
    )

    suspend fun ensureSeeded() {
        if (wordDao.count() == 0) {
            wordDao.insertAll(AssetWordLoader.loadWords(context))
        }
    }

    /**
     * Retorna as palavras do dia (1 a 3, conforme a configuração "palavras por dia"),
     * criando os registros que ainda não existirem. Nunca recua nem repete palavras já usadas.
     * Protegido por mutex para nunca gerar duas vezes a mesma posição em chamadas concorrentes.
     */
    suspend fun getOrCreateTodayWords(): List<TodayWord> = mutex.withLock {
        ensureSeeded()
        val todayStr = DateUtils.toStr(DateUtils.today())
        val wordsPerDay = prefs.wordsPerDay

        val existing = progressDao.getByDate(todayStr)
        val results = mutableListOf<TodayWord>()

        var cycleForToday: CycleEntity? = null
        var positionForToday = 1

        if (existing.isNotEmpty()) {
            cycleForToday = cycleDao.getByNumber(existing.first().cycleNumber)
            positionForToday = existing.first().positionInCycle
            for (p in existing) {
                val word = wordDao.getById(p.wordId) ?: continue
                val cycle = cycleForToday ?: continue
                results.add(TodayWord(word, p, cycle))
            }
        }

        if (existing.size >= wordsPerDay) {
            return@withLock results
        }

        // Precisa determinar (ou criar) o ciclo e a posição de hoje, caso ainda não existam.
        if (cycleForToday == null) {
            val latest = progressDao.getLatest()
            val (cycleNumber, positionInCycle, cycle) = if (latest == null) {
                // Primeiro uso do app: o ciclo 1 começa hoje.
                val newCycle = CycleEntity(cycleNumber = 1, startDate = todayStr)
                cycleDao.insert(newCycle)
                Triple(1, 1, newCycle)
            } else {
                val currentCycle = cycleDao.getByNumber(latest.cycleNumber)
                    ?: CycleEntity(cycleNumber = latest.cycleNumber, startDate = latest.dateShown)
                val start = DateUtils.fromStr(currentCycle.startDate)
                val daysSince = ChronoUnit.DAYS.between(start, DateUtils.today()).toInt()

                if (daysSince in 0..6) {
                    Triple(currentCycle.cycleNumber, daysSince + 1, currentCycle)
                } else {
                    // Passaram-se 7 dias corridos desde o início do ciclo atual: novo ciclo começa hoje.
                    val newCycleNumber = currentCycle.cycleNumber + 1
                    val newCycle = CycleEntity(cycleNumber = newCycleNumber, startDate = todayStr)
                    cycleDao.insert(newCycle)
                    Triple(newCycleNumber, 1, newCycle)
                }
            }
            cycleForToday = cycle
            positionForToday = positionInCycle
        }

        var nextIndex = existing.size + 1
        while (nextIndex <= wordsPerDay) {
            val nextWord = wordDao.getNextUnusedWord() ?: break // banco de palavras esgotado
            val progress = ProgressEntity(
                wordId = nextWord.id,
                dateShown = todayStr,
                cycleNumber = cycleForToday.cycleNumber,
                positionInCycle = positionForToday,
                wordIndexInDay = nextIndex,
                notifiedHour = prefs.notificationHour,
                notifiedMinute = prefs.notificationMinute
            )
            val id = progressDao.insert(progress)
            val saved = progressDao.getById(id) ?: progress
            results.add(TodayWord(nextWord, saved, cycleForToday))
            nextIndex++
        }

        results
    }

    /** Marca uma palavra do dia como efetivamente aprendida (ação explícita do usuário). */
    suspend fun markOpened(progress: ProgressEntity) {
        if (!progress.opened) {
            progressDao.update(
                progress.copy(opened = true, openedAt = DateUtils.toStr(DateUtils.today()))
            )
        }
    }

    /**
     * Reinicia a contagem do ciclo ATUAL a partir de hoje (volta a ser "dia 1 de 7"), e limpa
     * um eventual teste pendente desse ciclo. Não apaga nenhuma palavra já aprendida — apenas
     * afeta a partir de agora, os dias já passados continuam registrados como estavam.
     */
    suspend fun resetCurrentCycle(): Boolean = mutex.withLock {
        val latest = cycleDao.getLatest() ?: return@withLock false
        cycleDao.update(
            latest.copy(
                startDate = DateUtils.toStr(DateUtils.today()),
                testCompleted = false,
                testScore = null,
                testTotal = null,
                testDate = null
            )
        )
        true
    }

    /**
     * Retorna o ciclo mais antigo com teste pendente (7 dias corridos já passaram e o teste
     * ainda não foi respondido), ou null se não houver pendência. O teste libera mesmo que
     * apenas 1 palavra tenha sido aprendida no ciclo — quem decide quantas palavras testar
     * é a tela do teste, que simplesmente usa todas as aprendidas disponíveis.
     */
    suspend fun pendingTest(): CycleEntity? {
        val latest = progressDao.getLatest() ?: return null
        val currentCycle = cycleDao.getByNumber(latest.cycleNumber) ?: return null

        val pendingOlder = cycleDao.getPendingTests(currentCycle.cycleNumber)
        if (pendingOlder.isNotEmpty()) return pendingOlder.first()

        val start = DateUtils.fromStr(currentCycle.startDate)
        val daysSince = ChronoUnit.DAYS.between(start, DateUtils.today()).toInt()
        return if (daysSince > 6 && !currentCycle.testCompleted) currentCycle else null
    }

    /** Palavras de um ciclo, incluindo apenas as que o usuário marcou como aprendidas. */
    suspend fun getTestWords(cycleNumber: Int): List<Pair<ProgressEntity, WordEntity>> {
        val progresses = progressDao.getByCycle(cycleNumber).filter { it.opened }
        return progresses.mapNotNull { p -> wordDao.getById(p.wordId)?.let { p to it } }
    }

    suspend fun getDistractors(excludeId: Int, count: Int): List<WordEntity> =
        wordDao.getRandomDistractors(excludeId, count)

    suspend fun saveTestResult(cycle: CycleEntity, results: Map<Long, Boolean>) {
        for ((progressId, correct) in results) {
            val p = progressDao.getById(progressId) ?: continue
            progressDao.update(p.copy(testCorrect = correct))
        }
        val correctCount = results.values.count { it }
        cycleDao.update(
            cycle.copy(
                testCompleted = true,
                testScore = correctCount,
                testTotal = results.size,
                testDate = DateUtils.toStr(DateUtils.today())
            )
        )
    }

    suspend fun getStats(): Stats {
        val learned = progressDao.countLearned()
        val tested = progressDao.countTested()
        val correct = progressDao.countCorrect()
        val total = wordDao.count()
        val accuracy = if (tested > 0) (correct * 100) / tested else 0
        return Stats(learned, total, tested, correct, accuracy)
    }

    /** Agrupa os ciclos concluídos/em andamento em blocos de 4 (revisão mensal). */
    suspend fun getMonthlyGroups(): List<MonthGroup> {
        val latest = cycleDao.getLatest() ?: return emptyList()
        val totalCycles = latest.cycleNumber
        val groups = mutableListOf<MonthGroup>()
        var start = 1
        while (start <= totalCycles) {
            val end = minOf(start + 3, totalCycles)
            val cycles = cycleDao.getRange(start, end)
            groups.add(MonthGroup(monthIndex = (start - 1) / 4 + 1, cycles = cycles))
            start += 4
        }
        return groups.reversed()
    }

    /**
     * Linha do tempo completa, da data mais ANTIGA para a mais NOVA: do início do primeiro
     * ciclo até o fim do ciclo atual (incluindo dias futuros que ainda não tiveram palavra
     * sorteada). Cada entrada guarda o horário real de alerta daquele dia (histórico) — dias
     * futuros usam o horário atualmente configurado, já que ainda serão gerados com ele.
     * Palavras não aprendidas NUNCA são reveladas — a entrada aparece, mas sem o texto da palavra.
     */
    suspend fun getAgenda(): List<AgendaEntry> {
        val all = progressDao.getAll()
        val today = DateUtils.today()
        val entries = mutableListOf<AgendaEntry>()

        if (all.isNotEmpty()) {
            val byDate = all.groupBy { it.dateShown }
            val firstDate = all.minOf { DateUtils.fromStr(it.dateShown) }
            var d = today
            while (!d.isBefore(firstDate)) {
                val dayEntries = byDate[DateUtils.toStr(d)]
                if (dayEntries.isNullOrEmpty()) {
                    entries.add(
                        AgendaEntry(
                            d, null, false, null, notGenerated = true, future = false,
                            notifiedHour = prefs.notificationHour, notifiedMinute = prefs.notificationMinute
                        )
                    )
                } else {
                    for (p in dayEntries.sortedBy { it.wordIndexInDay }) {
                        val word = if (p.opened) wordDao.getById(p.wordId) else null
                        entries.add(
                            AgendaEntry(
                                d, p.cycleNumber, p.opened, word, notGenerated = false, future = false,
                                notifiedHour = p.notifiedHour, notifiedMinute = p.notifiedMinute
                            )
                        )
                    }
                }
                d = d.minusDays(1)
            }
        }

        // Dias futuros dentro do ciclo atual: mostram a data, sem palavra (ainda não sorteada).
        val latestCycle = cycleDao.getLatest()
        val futureEntries = mutableListOf<AgendaEntry>()
        if (latestCycle != null) {
            val cycleStart = DateUtils.fromStr(latestCycle.startDate)
            val cycleEnd = cycleStart.plusDays(6)
            var f = today.plusDays(1)
            while (!f.isAfter(cycleEnd)) {
                futureEntries.add(
                    AgendaEntry(
                        f, latestCycle.cycleNumber, false, null, notGenerated = false, future = true,
                        notifiedHour = prefs.notificationHour, notifiedMinute = prefs.notificationMinute
                    )
                )
                f = f.plusDays(1)
            }
        }

        // Da mais antiga para a mais nova.
        return (futureEntries.reversed() + entries).sortedBy { it.date }
    }

    /** Palavras já aprendidas, sem repetição — usadas no modo de Treino livre. */
    suspend fun getLearnedWords(): List<WordEntity> {
        val learned = progressDao.getAllLearned()
        return learned.mapNotNull { wordDao.getById(it.wordId) }.distinctBy { it.id }
    }
}
