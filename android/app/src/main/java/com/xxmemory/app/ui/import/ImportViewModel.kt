package com.xxmemory.app.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.XxMemoryApplication
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

    fun importManualCard(question: String, answer: String, subject: String, detail: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importMessage = null)
            try {
                val card = Card(
                    question = question,
                    answer = answer,
                    subject = subject,
                    detail = detail,
                    cardType = "qa"
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
                cards.add(
                    Card(
                        question = item["question"] as? String ?: "",
                        answer = item["answer"] as? String ?: "",
                        detail = item["detail"] as? String ?: "",
                        subject = item["subject"] as? String ?: "",
                        cardType = item["cardType"] as? String ?: "qa"
                    )
                )
            }
        } catch (e: Exception) {
            // Try single object
            val jsonObj = gson.fromJson(json, Map::class.java)
            cards.add(
                Card(
                    question = jsonObj["question"] as? String ?: "",
                    answer = jsonObj["answer"] as? String ?: "",
                    detail = jsonObj["detail"] as? String ?: "",
                    subject = jsonObj["subject"] as? String ?: "",
                    cardType = jsonObj["cardType"] as? String ?: "qa"
                )
            )
        }
        return cards
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }
}