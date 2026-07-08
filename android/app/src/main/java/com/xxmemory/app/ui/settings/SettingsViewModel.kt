package com.xxmemory.app.ui.settings

import androidx.lifecycle.ViewModel
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.SettingsManager
import com.xxmemory.app.domain.NotificationScheduler
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
    val algorithmType: String = "SM-2",
    val userName: String = "用户",
    val userEmail: String = "user@example.com"
)

class SettingsViewModel : ViewModel() {
    private val settingsManager: SettingsManager = XxMemoryApplication.instance.settingsManager

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            dailyCardLimit = settingsManager.dailyCardLimit,
            autoPlayAudio = settingsManager.autoPlayAudio,
            darkMode = settingsManager.darkMode,
            syncEnabled = settingsManager.syncEnabled,
            dailyReminder = settingsManager.dailyReminder,
            shuffleCards = settingsManager.shuffleCards,
            showDetailFirst = settingsManager.showDetailFirst,
            algorithmType = settingsManager.algorithmType,
            userName = settingsManager.userName,
            userEmail = settingsManager.userEmail
        )
    }

    fun toggleDarkMode(enabled: Boolean) {
        settingsManager.darkMode = enabled
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun toggleSync(enabled: Boolean) {
        settingsManager.syncEnabled = enabled
        _uiState.value = _uiState.value.copy(syncEnabled = enabled)
    }

    fun toggleDailyReminder(enabled: Boolean) {
        settingsManager.dailyReminder = enabled
        _uiState.value = _uiState.value.copy(dailyReminder = enabled)
        val context = XxMemoryApplication.instance
        if (enabled) {
            NotificationScheduler.scheduleDailyReminder(context)
        } else {
            NotificationScheduler.cancelDailyReminder(context)
        }
    }

    fun toggleAutoPlay(enabled: Boolean) {
        settingsManager.autoPlayAudio = enabled
        _uiState.value = _uiState.value.copy(autoPlayAudio = enabled)
    }

    fun toggleShuffle(enabled: Boolean) {
        settingsManager.shuffleCards = enabled
        _uiState.value = _uiState.value.copy(shuffleCards = enabled)
    }

    fun toggleShowDetailFirst(enabled: Boolean) {
        settingsManager.showDetailFirst = enabled
        _uiState.value = _uiState.value.copy(showDetailFirst = enabled)
    }

    fun setDailyCardLimit(limit: Int) {
        settingsManager.dailyCardLimit = limit
        _uiState.value = _uiState.value.copy(dailyCardLimit = limit)
    }

    fun setAlgorithmType(type: String) {
        settingsManager.algorithmType = type
        _uiState.value = _uiState.value.copy(algorithmType = type)
    }
}