package com.xxmemory.app.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
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
    val einkMode: Boolean = false,
    val dailyReminder: Boolean = true,
    val shuffleCards: Boolean = false,
    val showDetailFirst: Boolean = false,
    val algorithmType: String = "SM-2",
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val userName: String = "用户",
    val userEmail: String = "user@example.com",
    val permissionRationale: String? = null
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
            einkMode = settingsManager.einkMode,
            dailyReminder = settingsManager.dailyReminder,
            shuffleCards = settingsManager.shuffleCards,
            showDetailFirst = settingsManager.showDetailFirst,
            algorithmType = settingsManager.algorithmType,
            reminderHour = settingsManager.reminderHour,
            reminderMinute = settingsManager.reminderMinute,
            userName = settingsManager.userName,
            userEmail = settingsManager.userEmail
        )
    }

    fun toggleEinkMode(enabled: Boolean) {
        settingsManager.einkMode = enabled
        _uiState.value = _uiState.value.copy(einkMode = enabled)
    }

    fun toggleDailyReminder(enabled: Boolean): Boolean {
        val context = XxMemoryApplication.instance
        if (enabled) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                _uiState.value = _uiState.value.copy(
                    dailyReminder = false,
                    permissionRationale = "notification"
                )
                settingsManager.dailyReminder = false
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    _uiState.value = _uiState.value.copy(
                        dailyReminder = false,
                        permissionRationale = "exact_alarm"
                    )
                    settingsManager.dailyReminder = false
                    return false
                }
            }
        }
        settingsManager.dailyReminder = enabled
        _uiState.value = _uiState.value.copy(dailyReminder = enabled)
        if (enabled) {
            NotificationScheduler.scheduleDailyReminder(context)
        } else {
            NotificationScheduler.cancelDailyReminder(context)
        }
        return true
    }

    fun openExactAlarmSettings() {
        val context = XxMemoryApplication.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        dismissPermissionRationale()
    }

    fun dismissPermissionRationale() {
        _uiState.value = _uiState.value.copy(permissionRationale = null)
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
        val clamped = limit.coerceIn(5, 100)
        settingsManager.dailyCardLimit = clamped
        _uiState.value = _uiState.value.copy(dailyCardLimit = clamped)
    }

    fun setAlgorithmType(type: String) {
        settingsManager.algorithmType = type
        _uiState.value = _uiState.value.copy(algorithmType = type)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val clampedHour = hour.coerceIn(0, 23)
        val clampedMinute = minute.coerceIn(0, 59)
        settingsManager.reminderHour = clampedHour
        settingsManager.reminderMinute = clampedMinute
        _uiState.value = _uiState.value.copy(
            reminderHour = clampedHour,
            reminderMinute = clampedMinute
        )
        if (settingsManager.dailyReminder) {
            NotificationScheduler.scheduleDailyReminder(XxMemoryApplication.instance)
        }
    }

    fun setUserName(name: String) {
        settingsManager.userName = name
        _uiState.value = _uiState.value.copy(userName = name)
    }

    fun setUserEmail(email: String) {
        settingsManager.userEmail = email
        _uiState.value = _uiState.value.copy(userEmail = email)
    }
}
