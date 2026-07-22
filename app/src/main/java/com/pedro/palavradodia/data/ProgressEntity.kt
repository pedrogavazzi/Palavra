package com.pedro.palavradodia.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress",
    indices = [Index("wordId"), Index(value = ["dateShown", "wordIndexInDay"], unique = true)]
)
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val progressId: Long = 0,
    val wordId: Int,
    val dateShown: String,          // yyyy-MM-dd
    val cycleNumber: Int,
    val positionInCycle: Int,       // 1..7 (dia dentro do ciclo)
    val wordIndexInDay: Int = 1,    // 1..N (quando o usuário escolhe mais de 1 palavra por dia)
    val notifiedHour: Int = 9,      // horário real configurado no momento em que ESTA palavra foi gerada
    val notifiedMinute: Int = 0,    // (não muda retroativamente se o usuário alterar o horário depois)
    val opened: Boolean = false,    // true = usuário marcou como aprendida
    val openedAt: String? = null,
    val testCorrect: Boolean? = null
)
