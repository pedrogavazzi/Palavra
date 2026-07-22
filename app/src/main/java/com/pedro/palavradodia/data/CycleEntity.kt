package com.pedro.palavradodia.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey val cycleNumber: Int,
    val startDate: String,
    val testCompleted: Boolean = false,
    val testScore: Int? = null,
    val testTotal: Int? = null,
    val testDate: String? = null
)
