package com.xxmemory.app.domain.parser

import com.xxmemory.app.data.entity.Card
import com.opencsv.CSVReader
import java.io.StringReader

class CsvParser : DocumentParser {
    override val formatName: String = "CSV"

    override fun parse(content: String): List<Card> {
        val cards = mutableListOf<Card>()
        val reader = CSVReader(StringReader(content))
        val rows = reader.readAll()
        reader.close()

        if (rows.isEmpty()) return emptyList()

        val header = rows.first()
        val hasHeader = isHeaderRow(header)

        fun indexOf(name: String): Int =
            if (hasHeader) header.indexOfFirst { it.equals(name, ignoreCase = true) } else -1

        val startIndex = if (hasHeader) 1 else 0
        val questionIndex = indexOf("question").coerceAtLeast(0)
        val answerIndex = indexOf("answer").coerceAtLeast(1)
        val subjectIndex = indexOf("subject")
        val detailIndex = indexOf("detail")
        val tagsIndex = indexOf("tags")
        val cardTypeIndex = indexOf("cardType").let { if (it < 0) indexOf("card_type") else it }
        val imageUrlIndex = indexOf("imageUrl").let { if (it < 0) indexOf("image_url") else it }
        val audioUrlIndex = indexOf("audioUrl").let { if (it < 0) indexOf("audio_url") else it }
        val isFavoriteIndex = indexOf("isFavorite").let { if (it < 0) indexOf("is_favorite") else it }

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue

            val question = row.getOrNull(questionIndex)?.trim() ?: ""
            val answer = row.getOrNull(answerIndex)?.trim() ?: ""
            if (question.isBlank() || answer.isBlank()) continue

            val valueAt = { idx: Int -> if (idx >= 0) row.getOrNull(idx)?.trim() else null }
            val subject = valueAt(subjectIndex) ?: ""
            val detail = valueAt(detailIndex) ?: ""
            val tags = valueAt(tagsIndex) ?: ""
            val cardType = valueAt(cardTypeIndex)?.takeIf { it.isNotBlank() } ?: Card.TYPE_QA
            val imageUrl = valueAt(imageUrlIndex)?.takeIf { it.isNotBlank() }
            val audioUrl = valueAt(audioUrlIndex)?.takeIf { it.isNotBlank() }
            val isFavorite = valueAt(isFavoriteIndex)?.lowercase() in setOf("true", "1", "yes")

            cards.add(
                Card(
                    question = question,
                    answer = answer,
                    subject = subject,
                    detail = detail,
                    cardType = cardType,
                    tags = tags,
                    imageUrl = imageUrl,
                    audioUrl = audioUrl,
                    isFavorite = isFavorite
                )
            )
        }

        return cards
    }

    private fun isHeaderRow(row: Array<String>): Boolean {
        if (row.isEmpty()) return false
        val lower = row.map { it.lowercase() }
        val knownHeaders = setOf(
            "question", "answer", "subject", "detail", "tags",
            "cardtype", "card_type", "imageurl", "image_url",
            "audiourl", "audio_url", "isfavorite", "is_favorite"
        )
        return lower.any { it in knownHeaders }
    }
}
