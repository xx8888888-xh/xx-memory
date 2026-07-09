package com.xxmemory.app.domain.parser

import com.xxmemory.app.data.entity.Card

interface DocumentParser {
    val formatName: String
    fun parse(content: String): List<Card>
}
