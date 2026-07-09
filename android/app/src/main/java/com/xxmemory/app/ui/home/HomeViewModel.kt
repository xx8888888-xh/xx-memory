package com.xxmemory.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val dueCards: List<Card> = emptyList(),
    val totalCards: Int = 0,
    val dueCount: Int = 0,
    val todayReviewed: Int = 0,
    val subjects: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val weekStats: List<DayStat> = emptyList(),
    val isLoading: Boolean = true
)

data class DayStat(
    val dayOfWeek: String,
    val dayNum: Int,
    val count: Int,
    val isToday: Boolean
)

class HomeViewModel : ViewModel() {
    private val repository: CardRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getInstance(XxMemoryApplication.instance)
        repository = CardRepository(db.cardDao(), db.reviewLogDao())
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val startOfDay = CardRepository.getStartOfDay(now)
            val endOfDay = CardRepository.getEndOfDay(now)

            val dueCards = repository.getDueCardsList(startOfDay)
            val totalCards = repository.getTotalCardsSync()
            val todayReviewed = repository.getTodayReviewCount(startOfDay, endOfDay)
            val subjects = repository.getSubjects().first()

            val weekStats = mutableListOf<DayStat>()
            val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")

            for (i in 6 downTo 0) {
                val dayCal = Calendar.getInstance()
                dayCal.timeInMillis = startOfDay
                dayCal.add(Calendar.DAY_OF_YEAR, -i)
                val dayStart = CardRepository.getStartOfDay(dayCal.timeInMillis)
                val dayEnd = CardRepository.getEndOfDay(dayCal.timeInMillis)
                val count = repository.getCountForDay(dayStart, dayEnd)
                val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
                weekStats.add(
                    DayStat(
                        dayOfWeek = dayNames[dayOfWeek - 1],
                        dayNum = dayCal.get(Calendar.DAY_OF_MONTH),
                        count = count,
                        isToday = i == 0
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                dueCards = dueCards,
                totalCards = totalCards,
                dueCount = dueCards.size,
                todayReviewed = todayReviewed,
                subjects = subjects,
                weekStats = weekStats,
                isLoading = false
            )
        }
    }

    fun selectSubject(subject: String?) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }

    fun getFilteredCards(): List<Card> {
        val state = _uiState.value
        return if (state.selectedSubject != null) {
            state.dueCards.filter { it.subject == state.selectedSubject }
        } else {
            state.dueCards
        }
    }
}
