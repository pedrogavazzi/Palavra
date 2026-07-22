package com.pedro.palavradodia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CycleDao {
    @Insert
    suspend fun insert(cycle: CycleEntity)

    @Update
    suspend fun update(cycle: CycleEntity)

    @Query("SELECT * FROM cycles WHERE cycleNumber = :n LIMIT 1")
    suspend fun getByNumber(n: Int): CycleEntity?

    @Query("SELECT * FROM cycles ORDER BY cycleNumber DESC LIMIT 1")
    suspend fun getLatest(): CycleEntity?

    @Query("SELECT * FROM cycles WHERE cycleNumber BETWEEN :start AND :end ORDER BY cycleNumber ASC")
    suspend fun getRange(start: Int, end: Int): List<CycleEntity>

    @Query("SELECT * FROM cycles WHERE testCompleted = 0 AND cycleNumber < :currentCycle ORDER BY cycleNumber ASC")
    suspend fun getPendingTests(currentCycle: Int): List<CycleEntity>
}
