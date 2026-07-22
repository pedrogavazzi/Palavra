package com.pedro.palavradodia.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: Int,
    val palavra: String,
    val classe: String,
    val definicao: String,
    val exemplo: String,
    val frances: String,
    val ingles: String
)
