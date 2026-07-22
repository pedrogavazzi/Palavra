package com.pedro.palavradodia.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress",
    indices = [Index("wordId"), Index(value = ["dateShown"], unique = true)]
)
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val progressId: Long = 0,
    val wordId: Int,
    val dateShown: String,       // yyyy-MM-dd
    val cycleNumber: Int,
    val positionInCycle: Int,    // 1..7
    val opened: Boolean = false, // true = usuário marcou como aprendida
    val openedAt: String? = null,
    val testCorrect: Boolean? = null
)
