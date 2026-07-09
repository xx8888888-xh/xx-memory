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

        val startIndex = if (hasHeader) 1 else 0
        val questionIndex = if (hasHeader) header.indexOfFirst { it.equals("question", ignoreCase = true) }.coerceAtLeast(0) else 0
        val answerIndex = if (hasHeader) header.indexOfFirst { it.equals("answer", ignoreCase = true) }.coerceAtLeast(1) else 1
        val subjectIndex = if (hasHeader) header.indexOfFirst { it.equals("subject", ignoreCase = true) }.coerceAtLeast(-1) else -1
        val detailIndex = if (hasHeader) header.indexOfFirst { it.equals("detail", ignoreCase = true) }.coerceAtLeast(-1) else -1

        for (i in startIndex until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue

            val question = row.getOrNull(questionIndex)?.trim() ?: ""
            val answer = row.getOrNull(answerIndex)?.trim() ?: ""
            val subject = if (subjectIndex >= 0) row.getOrNull(subjectIndex)?.trim() ?: "" else ""
            val detail = if (detailIndex >= 0) row.getOrNull(detailIndex)?.trim() ?: "" else ""

            if (question.isNotBlank() && answer.isNotBlank()) {
                cards.add(
                    Card(
                        question = question,
                        answer = answer,
                        subject = subject,
                        detail = detail
                    )
                )
            }
        }

        return cards
    }

    private fun isHeaderRow(row: Array<String>): Boolean {
        if (row.isEmpty()) return false
        val lower = row.map { it.lowercase() }
        return lower.contains("question") || lower.contains("answer") ||
                lower.contains("subject") || lower.contains("detail")
    }
}
