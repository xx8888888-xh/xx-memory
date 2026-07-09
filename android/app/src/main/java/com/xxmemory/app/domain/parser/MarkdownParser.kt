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

        fun flushCard() {
            val q = currentQuestion?.trim()
            val a = currentAnswer.toString().trim()
            if (!q.isNullOrBlank() && a.isNotBlank()) {
                cards.add(
                    Card(
                        question = q,
                        answer = a,
                        subject = currentSubject,
                        detail = currentDetail
                    )
                )
            }
            currentQuestion = null
            currentAnswer = StringBuilder()
            currentSubject = ""
            currentDetail = ""
        }

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# Q:", ignoreCase = true) ||
                        trimmed.startsWith("## Q:", ignoreCase = true) ||
                        trimmed.startsWith("### Q:", ignoreCase = true) ||
                        trimmed.startsWith("#### Q:", ignoreCase = true) -> {
                    flushCard()
                    currentQuestion = trimmed.substringAfter("Q:", trimmed).trim()
                }
                trimmed.startsWith("# A:", ignoreCase = true) ||
                        trimmed.startsWith("## A:", ignoreCase = true) ||
                        trimmed.startsWith("### A:", ignoreCase = true) ||
                        trimmed.startsWith("#### A:", ignoreCase = true) -> {
                    currentAnswer.append(trimmed.substringAfter("A:", trimmed).trim()).append("\n")
                }
                trimmed.startsWith("# Subject:", ignoreCase = true) ||
                        trimmed.startsWith("## Subject:", ignoreCase = true) ||
                        trimmed.startsWith("### Subject:", ignoreCase = true) -> {
                    currentSubject = trimmed.substringAfter("Subject:", trimmed).trim()
                }
                trimmed.startsWith("# Detail:", ignoreCase = true) ||
                        trimmed.startsWith("## Detail:", ignoreCase = true) ||
                        trimmed.startsWith("### Detail:", ignoreCase = true) -> {
                    currentDetail = trimmed.substringAfter("Detail:", trimmed).trim()
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
