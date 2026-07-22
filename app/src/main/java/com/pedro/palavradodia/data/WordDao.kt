package com.pedro.palavradodia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): WordEntity?

    // Próxima palavra ainda não utilizada em nenhum registro de progresso,
    // sempre em ordem crescente de id (garante que o banco não repita palavras
    // enquanto houver palavras novas disponíveis).
    @Query(
        """
        SELECT * FROM words
        WHERE id NOT IN (SELECT wordId FROM progress)
        ORDER BY id ASC LIMIT 1
        """
    )
    suspend fun getNextUnusedWord(): WordEntity?

    @Query("SELECT * FROM words WHERE id != :excludeId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomDistractors(excludeId: Int, limit: Int): List<WordEntity>
}
