package com.xxmemory.app.domain.parser

import com.xxmemory.app.data.entity.Card

class TxtParser : DocumentParser {
    override val formatName: String = "TXT"

    override fun parse(content: String): List<Card> {
        val paragraphs = content.split("\n\n", "\r\n\r\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (paragraphs.isEmpty()) return emptyList()

        // Try Q:A: format first
        val qaCards = extractQAParagraphs(paragraphs)
        if (qaCards.isNotEmpty()) {
            return qaCards.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
        }

        // Fallback: pair paragraphs as question/answer
        return extractPairedParagraphs(paragraphs)
    }

    private fun extractQAParagraphs(paragraphs: List<String>): List<Card> {
        val cards = mutableListOf<Card>()
        var currentQuestion: String? = null
        var currentAnswer = StringBuilder()

        fun flushCard() {
            val q = currentQuestion?.trim()
            val a = currentAnswer.toString().trim()
            if (!q.isNullOrBlank() && a.isNotBlank()) {
                cards.add(
                    Card(
                        question = q,
                        answer = a
                    )
                )
            }
            currentQuestion = null
            currentAnswer = StringBuilder()
        }

        for (paragraph in paragraphs) {
            val lines = paragraph.lines()
            var hasQMarker = false
            var hasAMarker = false

            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("Q:", ignoreCase = true) -> {
                        flushCard()
                        currentQuestion = trimmed.substringAfter("Q:", trimmed).trim()
                        hasQMarker = true
                    }
                    trimmed.startsWith("A:", ignoreCase = true) -> {
                        if (currentAnswer.isNotEmpty()) currentAnswer.append("\n")
                        currentAnswer.append(trimmed.substringAfter("A:", trimmed).trim())
                        hasAMarker = true
                    }
                    currentQuestion != null && hasAMarker -> {
                        currentAnswer.append("\n").append(trimmed)
                    }
                    currentQuestion != null && !hasAMarker -> {
                        // Treat subsequent lines before A: as part of question if no A: yet
                        currentQuestion += "\n" + trimmed
                    }
                }
            }

            if (hasQMarker && hasAMarker) {
                // paragraph contained both Q and A; flush now
                flushCard()
            } else if (hasQMarker && !hasAMarker) {
                // Q without A in this paragraph; keep open for next paragraph
            } else if (!hasQMarker && currentQuestion != null) {
                // No Q marker but we have an open question; append as answer continuation
                if (currentAnswer.isNotEmpty()) currentAnswer.append("\n")
                currentAnswer.append(paragraph)
            }
        }

        flushCard()
        return cards
    }

    private fun extractPairedParagraphs(paragraphs: List<String>): List<Card> {
        val cards = mutableListOf<Card>()
        for (i in paragraphs.indices step 2) {
            val question = paragraphs[i]
            val answer = paragraphs.getOrNull(i + 1) ?: ""
            if (question.isNotBlank() && answer.isNotBlank()) {
                cards.add(
                    Card(
                        question = question,
                        answer = answer
                    )
                )
            }
        }
        return cards
    }
}
