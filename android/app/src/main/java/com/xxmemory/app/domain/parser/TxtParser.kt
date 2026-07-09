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
        var currentSubject = ""
        var currentDetail = ""
        var currentTags = ""
        var currentCardType = Card.TYPE_QA
        var currentImageUrl: String? = null
        var currentAudioUrl: String? = null
        var currentIsFavorite = false
        var currentPhonetic = ""
        var currentExample = ""
        var currentCollocations = ""
        var currentEtymology = ""
        var currentHint = ""
        var currentRhyme = ""
        var currentDerivatives = ""
        var currentMastered = false

        fun parseValue(line: String, prefix: String): String {
            val idx = line.indexOf(prefix, ignoreCase = true)
            return if (idx >= 0) line.substring(idx + prefix.length).trim() else line.trim()
        }

        fun flushCard() {
            val q = currentQuestion?.trim()
            val a = currentAnswer.toString().trim()
            if (!q.isNullOrBlank() && a.isNotBlank()) {
                cards.add(
                    Card(
                        question = q,
                        answer = a,
                        subject = currentSubject,
                        detail = currentDetail,
                        cardType = currentCardType,
                        tags = currentTags,
                        imageUrl = currentImageUrl?.takeIf { it.isNotBlank() },
                        audioUrl = currentAudioUrl?.takeIf { it.isNotBlank() },
                        isFavorite = currentIsFavorite,
                        phonetic = currentPhonetic,
                        example = currentExample,
                        collocations = currentCollocations,
                        etymology = currentEtymology,
                        hint = currentHint,
                        rhyme = currentRhyme,
                        derivatives = currentDerivatives,
                        mastered = currentMastered
                    )
                )
            }
            currentQuestion = null
            currentAnswer = StringBuilder()
            currentSubject = ""
            currentDetail = ""
            currentTags = ""
            currentCardType = Card.TYPE_QA
            currentImageUrl = null
            currentAudioUrl = null
            currentIsFavorite = false
            currentPhonetic = ""
            currentExample = ""
            currentCollocations = ""
            currentEtymology = ""
            currentHint = ""
            currentRhyme = ""
            currentDerivatives = ""
            currentMastered = false
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
                        currentQuestion = parseValue(trimmed, "Q:")
                        hasQMarker = true
                    }
                    trimmed.startsWith("A:", ignoreCase = true) -> {
                        if (currentAnswer.isNotEmpty()) currentAnswer.append("\n")
                        currentAnswer.append(parseValue(trimmed, "A:"))
                        hasAMarker = true
                    }
                    trimmed.startsWith("Subject:", ignoreCase = true) -> {
                        currentSubject = parseValue(trimmed, "Subject:")
                    }
                    trimmed.startsWith("Detail:", ignoreCase = true) -> {
                        currentDetail = parseValue(trimmed, "Detail:")
                    }
                    trimmed.startsWith("Tags:", ignoreCase = true) -> {
                        currentTags = parseValue(trimmed, "Tags:")
                    }
                    trimmed.startsWith("CardType:", ignoreCase = true) -> {
                        currentCardType = parseValue(trimmed, "CardType:").takeIf { it.isNotBlank() } ?: Card.TYPE_QA
                    }
                    trimmed.startsWith("ImageUrl:", ignoreCase = true) -> {
                        currentImageUrl = parseValue(trimmed, "ImageUrl:").takeIf { it.isNotBlank() }
                    }
                    trimmed.startsWith("AudioUrl:", ignoreCase = true) -> {
                        currentAudioUrl = parseValue(trimmed, "AudioUrl:").takeIf { it.isNotBlank() }
                    }
                    trimmed.startsWith("IsFavorite:", ignoreCase = true) -> {
                        currentIsFavorite = parseValue(trimmed, "IsFavorite:").lowercase() in setOf("true", "1", "yes")
                    }
                    trimmed.startsWith("Phonetic:", ignoreCase = true) -> {
                        currentPhonetic = parseValue(trimmed, "Phonetic:")
                    }
                    trimmed.startsWith("Example:", ignoreCase = true) -> {
                        currentExample = parseValue(trimmed, "Example:")
                    }
                    trimmed.startsWith("Collocations:", ignoreCase = true) -> {
                        currentCollocations = parseValue(trimmed, "Collocations:")
                    }
                    trimmed.startsWith("Etymology:", ignoreCase = true) -> {
                        currentEtymology = parseValue(trimmed, "Etymology:")
                    }
                    trimmed.startsWith("Hint:", ignoreCase = true) -> {
                        currentHint = parseValue(trimmed, "Hint:")
                    }
                    trimmed.startsWith("Rhyme:", ignoreCase = true) -> {
                        currentRhyme = parseValue(trimmed, "Rhyme:")
                    }
                    trimmed.startsWith("Derivatives:", ignoreCase = true) -> {
                        currentDerivatives = parseValue(trimmed, "Derivatives:")
                    }
                    trimmed.startsWith("Mastered:", ignoreCase = true) -> {
                        currentMastered = parseValue(trimmed, "Mastered:").lowercase() in setOf("true", "1", "yes")
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
            } else if (!hasQMarker && !hasAMarker && currentQuestion != null) {
                // No Q/A marker but we have an open question; append as answer continuation
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
