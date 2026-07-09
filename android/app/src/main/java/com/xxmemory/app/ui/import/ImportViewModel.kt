package com.xxmemory.app.ui.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.domain.parser.CsvParser
import com.xxmemory.app.domain.parser.DocumentParser
import com.xxmemory.app.domain.parser.MarkdownParser
import com.xxmemory.app.domain.parser.TxtParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportUiState(
    val urlInput: String = "",
    val selectedFormat: String = "",
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

    private var recentImportsJob: Job? = null

    init {
        val db = AppDatabase.getInstance(XxMemoryApplication.instance)
        repository = CardRepository(db.cardDao(), db.reviewLogDao())
        loadRecentImports()
    }

    private fun loadRecentImports() {
        recentImportsJob?.cancel()
        recentImportsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getAllCards().collect { cards ->
                    _uiState.value = _uiState.value.copy(
                        recentImports = cards.take(10),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectFormat(format: String) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun importFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            try {
                val content = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: ""

                if (content.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "文件内容为空"
                    )
                    return@launch
                }

                // Determine parser from explicit selection first, then file extension.
                val selectedFormat = _uiState.value.selectedFormat
                val parser: DocumentParser? = when {
                    selectedFormat.equals("Markdown", ignoreCase = true) -> MarkdownParser()
                    selectedFormat.equals("CSV", ignoreCase = true) -> CsvParser()
                    selectedFormat.equals("TXT", ignoreCase = true) -> TxtParser()
                    else -> {
                        val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
                        parsers[extension]
                    }
                }

                if (parser != null) {
                    val cards = parser.parse(content)
                    cards.forEach { repository.insertCard(it) }
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "成功导入 ${cards.size} 张卡片 (${parser.formatName}格式)",
                        urlInput = ""
                    )
                } else {
                    // Fallback to JSON if no parser matches.
                    importFromJson(content)
                    return@launch
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
                val trimmed = jsonContent.trim()
                if (trimmed.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "请输入 JSON 内容"
                    )
                    return@launch
                }
                val cards = parseCardsFromJson(trimmed)
                if (cards.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "未解析到有效卡片，请检查 JSON 格式"
                    )
                    return@launch
                }
                cards.forEach { repository.insertCard(it) }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "成功导入 ${cards.size} 张卡片",
                    urlInput = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "导入失败: ${e.message}"
                )
            }
        }
    }

    fun importManualCard(
        question: String,
        answer: String,
        subject: String,
        detail: String,
        cardType: String = Card.TYPE_QA,
        tags: String = "",
        imageUrl: String = "",
        audioUrl: String = "",
        phonetic: String = "",
        example: String = "",
        collocations: String = "",
        etymology: String = "",
        hint: String = "",
        rhyme: String = "",
        derivatives: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            try {
                val card = Card(
                    question = question,
                    answer = answer,
                    subject = subject,
                    detail = detail,
                    cardType = cardType,
                    tags = tags,
                    imageUrl = imageUrl.takeIf { it.isNotBlank() },
                    audioUrl = audioUrl.takeIf { it.isNotBlank() },
                    phonetic = phonetic,
                    example = example,
                    collocations = collocations,
                    etymology = etymology,
                    hint = hint,
                    rhyme = rhyme,
                    derivatives = derivatives
                )
                repository.insertCard(card)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "卡片创建成功"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "创建失败: ${e.message}"
                )
            }
        }
    }

    private fun parseCardsFromJson(json: String): List<Card> {
        val root = JsonParser.parseString(json) ?: throw IllegalArgumentException("JSON格式错误")
        val cardsJson = if (root is JsonObject && root.has("cards")) {
            root.getAsJsonArray("cards")
        } else if (root is JsonArray) {
            root
        } else {
            throw IllegalArgumentException("JSON格式错误：未找到卡片数组")
        }

        val cards = mutableListOf<Card>()
        for (element in cardsJson) {
            if (element is JsonObject) {
                cards.add(mapJsonToCard(element))
            }
        }
        return cards.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
    }

    private fun mapJsonToCard(json: JsonObject): Card {
        val question = json.get("question")?.asString
            ?: json.get("front")?.asString
            ?: ""
        val answer = json.get("answer")?.asString
            ?: json.get("back")?.asString
            ?: ""
        return Card(
            question = question,
            answer = answer,
            detail = json.get("detail")?.asString ?: "",
            subject = json.get("subject")?.asString ?: "",
            cardType = json.get("cardType")?.asString ?: json.get("card_type")?.asString ?: "qa",
            tags = json.get("tags")?.asString ?: "",
            audioUrl = json.get("audioUrl")?.asString ?: json.get("audio_url")?.asString,
            imageUrl = json.get("imageUrl")?.asString ?: json.get("image_url")?.asString,
            isFavorite = json.get("isFavorite")?.asBoolean
                ?: json.get("is_favorite")?.asBoolean
                ?: false,
            phonetic = json.get("phonetic")?.asString ?: "",
            example = json.get("example")?.asString ?: "",
            collocations = json.get("collocations")?.asString ?: "",
            etymology = json.get("etymology")?.asString ?: "",
            hint = json.get("hint")?.asString ?: "",
            rhyme = json.get("rhyme")?.asString ?: "",
            derivatives = json.get("derivatives")?.asString ?: "",
            mastered = json.get("mastered")?.asBoolean ?: false
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }
}
