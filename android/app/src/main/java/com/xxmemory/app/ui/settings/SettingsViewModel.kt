package com.xxmemory.app.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val dailyCardLimit: Int = 20,
    val autoPlayAudio: Boolean = false,
    val darkMode: Boolean = false,
    val syncEnabled: Boolean = false,
    val dailyReminder: Boolean = true,
    val shuffleCards: Boolean = false,
    val showDetailFirst: Boolean = false,
    val userName: String = "用户",
    val userEmail: String = "user@example.com"
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun toggleSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(syncEnabled = enabled)
    }

    fun toggleDailyReminder(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dailyReminder = enabled)
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPlayAudio = enabled)
    }

    fun toggleShuffle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(shuffleCards = enabled)
    }

    fun toggleShowDetailFirst(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showDetailFirst = enabled)
    }

    fun setDailyCardLimit(limit: Int) {
        _uiState.value = _uiState.value.copy(dailyCardLimit = limit)
    }
}