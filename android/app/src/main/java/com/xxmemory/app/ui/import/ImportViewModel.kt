package com.xxmemory.app.ui.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.domain.parser.DocumentParser
import com.xxmemory.app.domain.parser.CsvParser
import com.xxmemory.app.domain.parser.MarkdownParser
import com.xxmemory.app.domain.parser.TxtParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportUiState(
    val urlInput: String = "",
    val isImporting: Boolean = false,
    val importMessage: String? = null,
    val recentImports: List<Card> = emptyList(),
    val isLoading: Boolean = true
)

class ImportViewModel : ViewModel() {
    private val repository: CardRepository

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val parsers: Map<String, DocumentParser> = mapOf(
        "md" to MarkdownParser(),
        "markdown" to MarkdownParser(),
        "csv" to CsvParser(),
        "txt" to TxtParser()
    )

    init {
        val db = AppDatabase.getInstance(XxMemoryApplication.instance)
        repository = CardRepository(db.cardDao(), db.reviewLogDao())
        loadRecentImports()
    }

    private fun loadRecentImports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAllCards().collect { cards ->
                _uiState.value = _uiState.value.copy(
                    recentImports = cards.take(10),
                    isLoading = false
                )
            }
        }
    }

    fun importFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""

                if (content.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "文件内容为空"
                    )
                    return@launch
                }

                val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
                val parser = parsers[extension]
                if (parser != null) {
                    val cards = parser.parse(content)
                    for (card in cards) {
                        repository.insertCard(card)
                    }
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "成功导入 ${cards.size} 张卡片 (${parser.formatName}格式)",
                        urlInput = ""
                    )
                    loadRecentImports()
                } else {
                    importFromJson(content)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "文件读取失败: ${e.message}"
                )
            }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCard(cardId)
                _uiState.value = _uiState.value.copy(
                    importMessage = "卡片已删除"
                )
                loadRecentImports()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    importMessage = "删除失败: ${e.message}"
                )
            }
        }
    }

    fun updateUrlInput(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url)
    }

    fun importFromJson(jsonContent: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            try {
                val cards = parseCardsFromJson(jsonContent)
                for (card in cards) {
                    repository.insertCard(card)
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "成功导入 ${cards.size} 张卡片",
                    urlInput = ""
                )
                loadRecentImports()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "导入失败: ${e.message}"
                )
            }
        }
    }

    fun importManualCard(question: String, answer: String, subject: String, detail: String, cardType: String = Card.TYPE_QA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            try {
                val card = Card(
                    question = question,
                    answer = answer,
                    subject = subject,
                    detail = detail,
                    cardType = cardType
                )
                repository.insertCard(card)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "卡片创建成功"
                )
                loadRecentImports()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "创建失败: ${e.message}"
                )
            }
        }
    }

    private fun parseCardsFromJson(json: String): List<Card> {
        val cards = mutableListOf<Card>()
        val gson = com.google.gson.Gson()
        try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(
                java.util.List::class.java, java.util.Map::class.java, String::class.java, Any::class.java
            ).type
            val jsonArray: List<Map<String, Any>> = gson.fromJson(json, type)
            for (item in jsonArray) {
                cards.add(mapToCard(item))
            }
        } catch (e: Exception) {
            try {
                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val jsonObj: Map<String, Any>? = gson.fromJson(json, mapType)
                if (jsonObj == null) throw Exception("JSON格式错误")
                if (jsonObj.containsKey("cards")) {
                    val cardsArray = jsonObj["cards"] as? List<Map<String, Any>>
                    if (cardsArray != null) {
                        for (item in cardsArray) {
                            cards.add(mapToCard(item))
                        }
                    }
                } else {
                    cards.add(mapToCard(jsonObj))
                }
            } catch (e2: Exception) {
                throw e2
            }
        }
        return cards.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
    }

    private fun mapToCard(map: Map<*, *>): Card {
        return Card(
            question = map["question"] as? String ?: "",
            answer = map["answer"] as? String ?: "",
            detail = map["detail"] as? String ?: "",
            subject = map["subject"] as? String ?: "",
            cardType = map["cardType"] as? String ?: "qa",
            tags = map["tags"] as? String ?: "",
            isFavorite = map["isFavorite"] as? Boolean ?: (map["is_favorite"] as? Boolean ?: false)
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }
}