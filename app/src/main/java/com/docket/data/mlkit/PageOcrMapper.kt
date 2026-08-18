package com.docket.data.mlkit

import com.docket.domain.model.OcrWord

/** (De)serializes [OcrWord] lists to/from [com.docket.data.local.entity.PageOcrEntity.wordsBlob]
 *  — see that entity for why this is a delimited string instead of a table or a JSON library. */
object PageOcrMapper {

    fun serializeWords(words: List<OcrWord>): String =
        words.joinToString("\n") { word ->
            // Tabs/newlines in a recognized "word" token are not expected in practice, but strip
            // them defensively rather than let them corrupt the line format.
            val safeText = word.text.replace("\t", " ").replace("\n", " ")
            "$safeText\t${word.leftFrac}\t${word.topFrac}\t${word.rightFrac}\t${word.bottomFrac}"
        }

    fun deserializeWords(blob: String): List<OcrWord> {
        if (blob.isBlank()) return emptyList()
        return blob.lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size != 5) return@mapNotNull null
            runCatching {
                OcrWord(
                    text = parts[0],
                    leftFrac = parts[1].toFloat(),
                    topFrac = parts[2].toFloat(),
                    rightFrac = parts[3].toFloat(),
                    bottomFrac = parts[4].toFloat()
                )
            }.getOrNull()
        }
    }
}
