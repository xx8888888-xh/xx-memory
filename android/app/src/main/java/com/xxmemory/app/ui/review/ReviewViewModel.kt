package com.xxmemory.app.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.entity.ReviewLog
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.domain.EbbinghausAlgorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class ReviewUiState(
    val cards: List<Card> = emptyList(),
    val currentIndex: Int = 0,
    val currentCard: Card? = null,
    val isFlipped: Boolean = false,
    val progress: Float = 0f,
    val totalCount: Int = 0,
    val currentNumber: Int = 0,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val completedCount: Int = 0,
    val nextScheduleInfo: String? = null
)

class ReviewViewModel : ViewModel() {
    private val repository: CardRepository
    private val settingsManager = XxMemoryApplication.instance.settingsManager

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val isAssessing = AtomicBoolean(false)

    init {
        val db = AppDatabase.getInstance(XxMemoryApplication.instance)
        repository = CardRepository(db.cardDao(), db.reviewLogDao())
        loadDueCards()
    }

    fun loadDueCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val now = System.currentTimeMillis()
                val startOfDay = CardRepository.getStartOfDay(now)
                val cards = repository.getDueCardsList(startOfDay)

                if (cards.isEmpty()) {
                    _uiState.value = ReviewUiState(
                        isLoading = false,
                        isComplete = true
                    )
                    return@launch
                }

                // Apply daily card limit
                val limit = settingsManager.dailyCardLimit.coerceAtLeast(1)
                val limitedCards = cards.take(limit)

                // Apply shuffle
                val finalCards = if (settingsManager.shuffleCards) limitedCards.shuffled() else limitedCards

                _uiState.value = ReviewUiState(
                    cards = finalCards,
                    currentIndex = 0,
                    currentCard = finalCards.firstOrNull(),
                    isFlipped = settingsManager.showDetailFirst,
                    totalCount = finalCards.size,
                    currentNumber = 1,
                    isLoading = false,
                    progress = 1f / finalCards.size.coerceAtLeast(1)
                )
            } catch (e: Exception) {
                Log.e("ReviewViewModel", "loadDueCards failed", e)
                _uiState.value = ReviewUiState(isLoading = false, isComplete = true)
            }
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = true)
    }

    fun assessCard(quality: Int) {
        if (!isAssessing.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val card = state.currentCard ?: return@launch

                val algorithm = EbbinghausAlgorithm.getAlgorithm(settingsManager.algorithmType)
                val result = algorithm.calculate(
                    quality = quality,
                    repetitions = card.repetitions,
                    easeFactor = card.easeFactor,
                    currentInterval = card.interval
                )

                val updatedCard = card.copy(
                    repetitions = result.nextRepetitions,
                    easeFactor = result.nextEaseFactor,
                    difficulty = if (result.nextDifficulty > 0) result.nextDifficulty.toFloat() else card.difficulty,
                    interval = result.nextInterval,
                    nextReviewDate = result.nextReviewDate
                )
                repository.updateCard(updatedCard)

                val log = ReviewLog(
                    cardId = card.id,
                    quality = quality,
                    reviewDate = System.currentTimeMillis(),
                    nextInterval = result.nextInterval
                )
                repository.insertReviewLog(log)

                val nextIndex = state.currentIndex + 1
                if (nextIndex >= state.cards.size) {
                    _uiState.value = state.copy(
                        isComplete = true,
                        completedCount = state.completedCount + 1,
                        currentCard = null,
                        isFlipped = false,
                        nextScheduleInfo = null,
                        progress = 1f
                    )
                } else {
                    val nextCard = state.cards[nextIndex]
                    _uiState.value = state.copy(
                        currentIndex = nextIndex,
                        currentCard = nextCard,
                        isFlipped = settingsManager.showDetailFirst,
                        currentNumber = nextIndex + 1,
                        progress = (nextIndex + 1).toFloat() / state.cards.size,
                        completedCount = state.completedCount + 1,
                        nextScheduleInfo = "下次复习: ${getIntervalText(result.nextInterval)}"
                    )
                }
            } finally {
                isAssessing.set(false)
            }
        }
    }

    private fun getIntervalText(interval: Int): String = when {
        interval < 1 -> "今天"
        interval == 1 -> "明天"
        interval < 7 -> "${interval}天后"
        interval < 30 -> "${interval / 7}周后"
        interval < 365 -> "${interval / 30}个月后"
        else -> "${interval / 365}年后"
    }

    fun toggleFavorite(card: Card) {
        viewModelScope.launch {
            repository.toggleFavorite(card.id)
            _uiState.value = _uiState.value.copy(
                currentCard = card.copy(isFavorite = !card.isFavorite)
            )
        }
    }
}
