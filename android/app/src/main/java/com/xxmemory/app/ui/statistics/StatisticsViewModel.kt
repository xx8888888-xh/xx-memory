package com.xxmemory.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.XxMemoryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            val now = System.currentTimeMillis()
            val startOfDay = CardRepository.getStartOfDay(now)
            val endOfDay = CardRepository.getEndOfDay(now)
            val startOfWeek = CardRepository.getStartOfWeek(now)
            val endOfWeek = CardRepository.getEndOfWeek(now)

            val totalCards = repository.getTotalCardsSync()
            val todayReviewed = repository.getTodayReviewCount(startOfDay, endOfDay)
            val totalReviewed = repository.getCountAfter(0)

            // Calculate streak
            val streak = calculateStreak(startOfDay)

            // Weekly stats
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

            // Subject mastery - use real data from repository
            val subjectMastery = mutableListOf<SubjectMastery>()
            val allCards = repository.getAllCardsSync()
            val subjects = allCards.map { it.subject }.distinct().filter { it.isNotBlank() }
            for (subject in subjects) {
                val subjectCards = allCards.filter { it.subject == subject }
                val masteredCount = subjectCards.count { it.interval >= 21 } // 21+ days = mastered
                subjectMastery.add(
                    SubjectMastery(
                        subject = subject,
                        totalCards = subjectCards.size,
                        masteredCards = masteredCount
                    )
                )
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

    private suspend fun calculateStreak(todayStart: Long): Int {
        var streak = 0
        var checkDay = todayStart
        for (i in 0..365) {
            val dayEnd = CardRepository.getEndOfDay(checkDay)
            val count = repository.getCountForDay(checkDay, dayEnd)
            if (count > 0) {
                streak++
                checkDay -= 86400000L
            } else {
                break
            }
        }
        return streak
    }
}