package com.pedro.palavradodia.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun toStr(date: LocalDate): String = date.format(FORMATTER)

    fun fromStr(s: String): LocalDate = LocalDate.parse(s, FORMATTER)
}
