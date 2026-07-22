package com.pedro.palavradodia.repository

import android.content.Context
import com.pedro.palavradodia.data.AppDatabase
import com.pedro.palavradodia.data.AssetWordLoader
import com.pedro.palavradodia.data.CycleEntity
import com.pedro.palavradodia.data.ProgressEntity
import com.pedro.palavradodia.data.WordEntity
import com.pedro.palavradodia.util.DateUtils
import java.time.temporal.ChronoUnit

/**
 * Camada central de regras de negócio do app:
 * - decide qual palavra mostrar em cada dia
 * - controla o ciclo de 7 dias (iniciado no dia em que o usuário abre a primeira palavra)
 * - gera testes semanais e agrupamentos mensais (4 ciclos)
 * - garante que nenhuma palavra ainda não aprendida apareça em histórico/estatísticas/testes
 */
class PalavraRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val wordDao = db.wordDao()
    private val progressDao = db.progressDao()
    private val cycleDao = db.cycleDao()

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
        val date: java.time.LocalDate,
        val cycleNumber: Int?,
        val learned: Boolean,
        val word: WordEntity?,
        /** true quando nenhuma palavra chegou a ser gerada naquele dia (ex.: notificação não disparou) */
        val notGenerated: Boolean
    )

    suspend fun ensureSeeded() {
        if (wordDao.count() == 0) {
            wordDao.insertAll(AssetWordLoader.loadWords(context))
        }
    }

    /**
     * Retorna a palavra do dia, criando um novo registro de progresso se ainda
     * não existir um para a data de hoje. Nunca recua nem repete palavras já usadas.
     */
    suspend fun getOrCreateTodayWord(): TodayWord? {
        ensureSeeded()
        val todayStr = DateUtils.toStr(DateUtils.today())

        val existing = progressDao.getByDate(todayStr)
        if (existing != null) {
            val word = wordDao.getById(existing.wordId) ?: return null
            val cycle = cycleDao.getByNumber(existing.cycleNumber) ?: return null
            return TodayWord(word, existing, cycle)
        }

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

        val nextWord = wordDao.getNextUnusedWord() ?: return null // banco de palavras esgotado
        val progress = ProgressEntity(
            wordId = nextWord.id,
            dateShown = todayStr,
            cycleNumber = cycleNumber,
            positionInCycle = positionInCycle
        )
        val id = progressDao.insert(progress)
        val saved = progressDao.getById(id) ?: progress
        return TodayWord(nextWord, saved, cycle)
    }

    /** Marca a palavra do dia como efetivamente aprendida (ação explícita do usuário). */
    suspend fun markOpened(progress: ProgressEntity) {
        if (!progress.opened) {
            progressDao.update(
                progress.copy(opened = true, openedAt = DateUtils.toStr(DateUtils.today()))
            )
        }
    }

    /**
     * Retorna o ciclo mais antigo com teste pendente (7 dias corridos já passaram
     * e o teste ainda não foi respondido), ou null se não houver pendência.
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

    /** Histórico de revisão: somente palavras já aprendidas — nunca as futuras/não vistas. */
    suspend fun getLearnedHistory(): List<Pair<ProgressEntity, WordEntity>> {
        val list = progressDao.getAllLearned()
        return list.mapNotNull { p -> wordDao.getById(p.wordId)?.let { p to it } }
    }

    suspend fun getStats(): Stats {
        val learned = progressDao.countLearned()
        val tested = progressDao.countTested()
        val correct = progressDao.countCorrect()
        val total = wordDao.count()
        val accuracy = if (tested > 0) (correct * 100) / tested else 0
        return Stats(learned, total, tested, correct, accuracy)
    }

    /** Agrupa os ciclos concluídos/andamento em blocos de 4 (revisão mensal). */
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
     * Linha do tempo completa: uma entrada por dia, do início do primeiro ciclo até hoje.
     * A data de cada entrada corresponde ao agendamento da notificação diária.
     * Palavras não aprendidas NUNCA são reveladas — a entrada aparece, mas sem o texto da palavra.
     */
    suspend fun getAgenda(): List<AgendaEntry> {
        val all = progressDao.getAll()
        if (all.isEmpty()) return emptyList()

        val byDate = all.associateBy { it.dateShown }
        val firstDate = all.minOf { DateUtils.fromStr(it.dateShown) }
        val today = DateUtils.today()

        val entries = mutableListOf<AgendaEntry>()
        var d = today
        while (!d.isBefore(firstDate)) {
            val p = byDate[DateUtils.toStr(d)]
            if (p == null) {
                entries.add(AgendaEntry(d, null, false, null, notGenerated = true))
            } else {
                val word = if (p.opened) wordDao.getById(p.wordId) else null
                entries.add(AgendaEntry(d, p.cycleNumber, p.opened, word, notGenerated = false))
            }
            d = d.minusDays(1)
        }
        return entries
    }

    /** Palavras já aprendidas, sem repetição — usadas no modo de Treino livre. */
    suspend fun getLearnedWords(): List<WordEntity> {
        val learned = progressDao.getAllLearned()
        return learned.mapNotNull { wordDao.getById(it.wordId) }.distinctBy { it.id }
    }
}
