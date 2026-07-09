package com.xxmemory.app.domain.parser

import com.xxmemory.app.data.entity.Card
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Document
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.Parser

class MarkdownParser : DocumentParser {
    override val formatName: String = "Markdown"

    override fun parse(content: String): List<Card> {
        val cards = mutableListOf<Card>()
        val parser = Parser.builder().build()
        val document = parser.parse(content) as? Document ?: return emptyList()

        // First try to detect Q/A format explicitly marked with headings
        val qaCards = extractQAPairs(content)
        if (qaCards.isNotEmpty()) {
            cards.addAll(qaCards)
        } else {
            // Fallback: use heading-based extraction
            cards.addAll(extractByHeadings(document))
        }

        return cards.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
    }

    private fun extractQAPairs(content: String): List<Card> {
        val cards = mutableListOf<Card>()
        val lines = content.lines()
        var currentQuestion: String? = null
        var currentAnswer = StringBuilder()
        var currentSubject = ""
        var currentDetail = ""
        var currentTags = ""
        var currentCardType = Card.TYPE_QA
        var currentImageUrl: String? = null
        var currentAudioUrl: String? = null
        var currentIsFavorite = false

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
                        isFavorite = currentIsFavorite
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
        }

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# Q:", ignoreCase = true) ||
                        trimmed.startsWith("## Q:", ignoreCase = true) ||
                        trimmed.startsWith("### Q:", ignoreCase = true) ||
                        trimmed.startsWith("#### Q:", ignoreCase = true) -> {
                    flushCard()
                    currentQuestion = parseValue(trimmed, "Q:")
                }
                trimmed.startsWith("# A:", ignoreCase = true) ||
                        trimmed.startsWith("## A:", ignoreCase = true) ||
                        trimmed.startsWith("### A:", ignoreCase = true) ||
                        trimmed.startsWith("#### A:", ignoreCase = true) -> {
                    currentAnswer.append(parseValue(trimmed, "A:")).append("\n")
                }
                trimmed.startsWith("# Subject:", ignoreCase = true) ||
                        trimmed.startsWith("## Subject:", ignoreCase = true) ||
                        trimmed.startsWith("### Subject:", ignoreCase = true) -> {
                    currentSubject = parseValue(trimmed, "Subject:")
                }
                trimmed.startsWith("# Detail:", ignoreCase = true) ||
                        trimmed.startsWith("## Detail:", ignoreCase = true) ||
                        trimmed.startsWith("### Detail:", ignoreCase = true) -> {
                    currentDetail = parseValue(trimmed, "Detail:")
                }
                trimmed.startsWith("# Tags:", ignoreCase = true) ||
                        trimmed.startsWith("## Tags:", ignoreCase = true) ||
                        trimmed.startsWith("### Tags:", ignoreCase = true) -> {
                    currentTags = parseValue(trimmed, "Tags:")
                }
                trimmed.startsWith("# CardType:", ignoreCase = true) ||
                        trimmed.startsWith("## CardType:", ignoreCase = true) ||
                        trimmed.startsWith("### CardType:", ignoreCase = true) -> {
                    currentCardType = parseValue(trimmed, "CardType:").takeIf { it.isNotBlank() } ?: Card.TYPE_QA
                }
                trimmed.startsWith("# ImageUrl:", ignoreCase = true) ||
                        trimmed.startsWith("## ImageUrl:", ignoreCase = true) ||
                        trimmed.startsWith("### ImageUrl:", ignoreCase = true) -> {
                    currentImageUrl = parseValue(trimmed, "ImageUrl:").takeIf { it.isNotBlank() }
                }
                trimmed.startsWith("# AudioUrl:", ignoreCase = true) ||
                        trimmed.startsWith("## AudioUrl:", ignoreCase = true) ||
                        trimmed.startsWith("### AudioUrl:", ignoreCase = true) -> {
                    currentAudioUrl = parseValue(trimmed, "AudioUrl:").takeIf { it.isNotBlank() }
                }
                trimmed.startsWith("# IsFavorite:", ignoreCase = true) ||
                        trimmed.startsWith("## IsFavorite:", ignoreCase = true) ||
                        trimmed.startsWith("### IsFavorite:", ignoreCase = true) -> {
                    currentIsFavorite = parseValue(trimmed, "IsFavorite:").lowercase() in setOf("true", "1", "yes")
                }
                currentQuestion != null -> {
                    currentAnswer.append(trimmed).append("\n")
                }
            }
        }
        flushCard()
        return cards
    }

    private fun extractByHeadings(document: Document): List<Card> {
        val cards = mutableListOf<Card>()
        val sections = mutableListOf<Pair<String, StringBuilder>>()
        var currentTitle: String? = null
        var currentBody = StringBuilder()

        document.accept(object : AbstractVisitor() {
            override fun visit(heading: Heading) {
                if (heading.level == 2) {
                    if (currentTitle != null) {
                        sections.add(currentTitle!! to currentBody)
                    }
                    val titleText = StringBuilder()
                    heading.accept(object : AbstractVisitor() {
                        override fun visit(text: Text) {
                            titleText.append(text.literal)
                        }
                    })
                    currentTitle = titleText.toString().trim()
                    currentBody = StringBuilder()
                }
                super.visit(heading)
            }

            override fun visit(paragraph: Paragraph) {
                if (currentTitle != null) {
                    val paraText = StringBuilder()
                    paragraph.accept(object : AbstractVisitor() {
                        override fun visit(text: Text) {
                            paraText.append(text.literal)
                        }
                    })
                    if (currentBody.isNotEmpty()) {
                        currentBody.append("\n")
                    }
                    currentBody.append(paraText.toString().trim())
                }
                super.visit(paragraph)
            }
        })

        if (currentTitle != null) {
            sections.add(currentTitle!! to currentBody)
        }

        for ((title, body) in sections) {
            val answerText = body.toString().trim()
            if (answerText.isNotBlank()) {
                cards.add(
                    Card(
                        question = title,
                        answer = answerText
                    )
                )
            }
        }

        return cards
    }
}
