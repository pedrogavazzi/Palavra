package com.pedro.palavradodia.data

import android.content.Context
import org.json.JSONArray

// Lê o banco curado de palavras (assets/words.json) e converte para entidades Room.
// Esse arquivo é a fonte única do banco de vocabulário do app.
object AssetWordLoader {
    fun loadWords(context: Context): List<WordEntity> {
        val json = context.assets.open("words.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(json)
        val list = mutableListOf<WordEntity>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                WordEntity(
                    id = o.getInt("id"),
                    palavra = o.getString("palavra"),
                    classe = o.getString("classe"),
                    definicao = o.getString("definicao"),
                    exemplo = o.getString("exemplo"),
                    frances = o.getString("frances"),
                    ingles = o.getString("ingles")
                )
            )
        }
        return list
    }
}
