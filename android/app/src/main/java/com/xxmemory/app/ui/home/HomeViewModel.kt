package com.xxmemory.app.ui.home

import android.util.Log
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
    val nextSevenDays: List<DayDueStat> = emptyList(),
    val calendarMonthDays: List<CalendarDayStat> = emptyList(),
    val selectedCalendarDay: Long? = null,
    val selectedDayCards: List<Card> = emptyList(),
    val searchQuery: String = "",
    val editingCard: Card? = null,
    val isLoading: Boolean = true
)

data class DayDueStat(
    val dayOfWeek: String,
    val dayNum: Int,
    val dueCount: Int,
    val timestamp: Long,
    val isToday: Boolean
)

data class CalendarDayStat(
    val dayNum: Int,
    val dueCount: Int,
    val timestamp: Long,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
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
            try {
                val now = System.currentTimeMillis()
                val startOfDay = CardRepository.getStartOfDay(now)
                val endOfDay = CardRepository.getEndOfDay(now)

                val dueCards = repository.getDueCardsList(endOfDay)
                val totalCards = repository.getTotalCardsSync()
                val todayReviewed = repository.getTodayReviewCount(startOfDay, endOfDay)
                val subjects = repository.getSubjects().first()

                val settings = XxMemoryApplication.instance.settingsManager
                val limit = if (settings.dailyCardLimitEnabled) {
                    settings.dailyCardLimit.coerceAtLeast(1)
                } else {
                    Int.MAX_VALUE
                }

                val nextSevenDays = buildNextSevenDays(startOfDay)

                _uiState.value = _uiState.value.copy(
                    dueCards = dueCards,
                    totalCards = totalCards,
                    dueCount = if (settings.dailyCardLimitEnabled) dueCards.size.coerceAtMost(limit) else dueCards.size,
                    todayReviewed = todayReviewed,
                    subjects = subjects,
                    nextSevenDays = nextSevenDays,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "loadData failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun buildNextSevenDays(todayStart: Long): List<DayDueStat> {
        val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
        val result = mutableListOf<DayDueStat>()
        for (i in 0..6) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = todayStart
            cal.add(Calendar.DAY_OF_YEAR, i)
            val dayStart = CardRepository.getStartOfDay(cal.timeInMillis)
            val dayEnd = CardRepository.getNextDayStart(cal.timeInMillis)
            val count = repository.getDueCountBetween(dayStart, dayEnd)
            result.add(
                DayDueStat(
                    dayOfWeek = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1],
                    dayNum = cal.get(Calendar.DAY_OF_MONTH),
                    dueCount = count,
                    timestamp = dayStart,
                    isToday = i == 0
                )
            )
        }
        return result
    }

    fun loadCalendarMonth() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val todayStart = CardRepository.getStartOfDay(now)
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val currentMonth = cal.get(Calendar.MONTH)
                val monthStart = cal.timeInMillis

                val firstDayCal = cal.clone() as Calendar
                firstDayCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                if (firstDayCal.timeInMillis > monthStart) {
                    firstDayCal.add(Calendar.WEEK_OF_YEAR, -1)
                }
                val gridStart = CardRepository.getStartOfDay(firstDayCal.timeInMillis)

                val days = mutableListOf<CalendarDayStat>()
                val gridCal = Calendar.getInstance().apply { timeInMillis = gridStart }
                repeat(42) {
                    val dayStart = CardRepository.getStartOfDay(gridCal.timeInMillis)
                    val dayEnd = CardRepository.getNextDayStart(gridCal.timeInMillis)
                    val count = repository.getDueCountForDay(dayStart, dayEnd)
                    days.add(
                        CalendarDayStat(
                            dayNum = gridCal.get(Calendar.DAY_OF_MONTH),
                            dueCount = count,
                            timestamp = dayStart,
                            isToday = dayStart == todayStart,
                            isCurrentMonth = gridCal.get(Calendar.MONTH) == currentMonth
                        )
                    )
                    gridCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                _uiState.value = _uiState.value.copy(calendarMonthDays = days)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "loadCalendarMonth failed", e)
            }
        }
    }

    fun selectCalendarDay(dayStart: Long?) {
        if (dayStart == null) {
            _uiState.value = _uiState.value.copy(
                selectedCalendarDay = null,
                selectedDayCards = emptyList()
            )
            return
        }
        _uiState.value = _uiState.value.copy(selectedCalendarDay = dayStart)
        viewModelScope.launch {
            try {
                val dayEnd = CardRepository.getNextDayStart(dayStart)
                val cards = repository.getCardsForCalendar(dayStart, dayEnd)
                _uiState.value = _uiState.value.copy(selectedDayCards = cards)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "selectCalendarDay failed", e)
            }
        }
    }

    fun selectSubject(subject: String?) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredCards(): List<Card> {
        val state = _uiState.value
        var cards = if (state.selectedSubject != null) {
            state.dueCards.filter { it.subject == state.selectedSubject }
        } else {
            state.dueCards
        }
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim()
            cards = cards.filter {
                it.question.contains(q, ignoreCase = true) ||
                it.answer.contains(q, ignoreCase = true) ||
                it.subject.contains(q, ignoreCase = true) ||
                it.tags.contains(q, ignoreCase = true) ||
                it.detail.contains(q, ignoreCase = true) ||
                it.hint.contains(q, ignoreCase = true)
            }
        }
        return cards
    }

    fun updateCard(card: Card) {
        viewModelScope.launch {
            try {
                repository.updateCard(card)
                loadData()
                _uiState.value = _uiState.value.copy(editingCard = null)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "updateCard failed", e)
            }
        }
    }

    fun startEditCard(card: Card) {
        _uiState.value = _uiState.value.copy(editingCard = card)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(editingCard = null)
    }
}
