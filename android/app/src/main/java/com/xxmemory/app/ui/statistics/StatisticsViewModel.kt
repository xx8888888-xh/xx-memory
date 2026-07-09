package com.xxmemory.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StatisticsUiState(
    val streakDays: Int = 0,
    val totalReviewed: Int = 0,
    val totalCards: Int = 0,
    val todayReviewed: Int = 0,
    val weeklyStats: List<WeeklyDayStat> = emptyList(),
    val subjectMastery: List<SubjectMastery> = emptyList(),
    val isLoading: Boolean = true
)

data class WeeklyDayStat(
    val dayName: String,
    val count: Int
)

data class SubjectMastery(
    val subject: String,
    val totalCards: Int,
    val masteredCards: Int
)

class StatisticsViewModel : ViewModel() {
    private val repository: CardRepository

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getInstance(XxMemoryApplication.instance)
        repository = CardRepository(db.cardDao(), db.reviewLogDao())
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val startOfDay = CardRepository.getStartOfDay(now)
                val endOfDay = CardRepository.getNextDayStart(now)
                val startOfWeek = CardRepository.getStartOfWeek(now)

                val totalCards = repository.getTotalCardsSync()
                val todayReviewed = repository.getTodayReviewCount(startOfDay, endOfDay)
                val totalReviewed = repository.getCountAfter(0)

                val streak = calculateStreak(startOfDay)

                val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
                val weeklyStats = mutableListOf<WeeklyDayStat>()
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = startOfWeek
                for (i in 0..6) {
                    val dayStart = cal.timeInMillis
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    val dayEnd = cal.timeInMillis - 1
                    val count = repository.getCountForDay(dayStart, dayEnd)
                    weeklyStats.add(WeeklyDayStat(dayNames[i], count))
                }

                val allCards = repository.getAllCardsSync()
                val subjectMastery = allCards
                    .groupBy { it.subject }
                    .filter { it.key.isNotBlank() }
                    .map { (subject, cards) ->
                        val masteredCount = cards.count { it.interval >= 21 }
                        SubjectMastery(subject, cards.size, masteredCount)
                    }

                _uiState.value = StatisticsUiState(
                    streakDays = streak,
                    totalReviewed = totalReviewed,
                    totalCards = totalCards,
                    todayReviewed = todayReviewed,
                    weeklyStats = weeklyStats,
                    subjectMastery = subjectMastery,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun calculateStreak(todayStart: Long): Int {
        var streak = 0
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = todayStart
        for (i in 0..365) {
            val dayStart = CardRepository.getStartOfDay(cal.timeInMillis)
            val dayEnd = CardRepository.getNextDayStart(dayStart)
            val count = repository.getCountForDay(dayStart, dayEnd)
            if (count > 0) {
                streak++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }
}
