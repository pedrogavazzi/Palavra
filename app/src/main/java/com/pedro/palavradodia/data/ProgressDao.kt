package com.pedro.palavradodia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProgressDao {
    @Insert
    suspend fun insert(progress: ProgressEntity): Long

    @Update
    suspend fun update(progress: ProgressEntity)

    // Todas as palavras já geradas para uma data (pode ser mais de 1 quando o
    // usuário configurou mais de 1 palavra por dia), ordenadas pela ordem de geração.
    @Query("SELECT * FROM progress WHERE dateShown = :date ORDER BY wordIndexInDay ASC")
    suspend fun getByDate(date: String): List<ProgressEntity>

    @Query("SELECT * FROM progress ORDER BY dateShown DESC, wordIndexInDay DESC LIMIT 1")
    suspend fun getLatest(): ProgressEntity?

    @Query("SELECT * FROM progress WHERE cycleNumber = :cycle ORDER BY positionInCycle ASC, wordIndexInDay ASC")
    suspend fun getByCycle(cycle: Int): List<ProgressEntity>

    // Usado pela Agenda: todos os registros (aprendidos ou não), para montar a linha
    // do tempo completa desde o início do primeiro ciclo.
    @Query("SELECT * FROM progress ORDER BY dateShown DESC, wordIndexInDay DESC")
    suspend fun getAll(): List<ProgressEntity>

    // Apenas palavras marcadas como aprendidas (opened = 1) — nunca palavras
    // ainda não vistas pelo usuário.
    @Query("SELECT * FROM progress WHERE opened = 1 ORDER BY dateShown DESC, wordIndexInDay DESC")
    suspend fun getAllLearned(): List<ProgressEntity>

    @Query("SELECT COUNT(*) FROM progress WHERE opened = 1")
    suspend fun countLearned(): Int

    @Query("SELECT COUNT(*) FROM progress WHERE testCorrect = 1")
    suspend fun countCorrect(): Int

    @Query("SELECT COUNT(*) FROM progress WHERE testCorrect IS NOT NULL")
    suspend fun countTested(): Int

    @Query("SELECT * FROM progress WHERE progressId = :id LIMIT 1")
    suspend fun getById(id: Long): ProgressEntity?
}
