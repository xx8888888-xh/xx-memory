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
import com.xxmemory.app.domain.SchedulerUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val dailyCardLimit: Int = 20,
    val dailyCardLimitEnabled: Boolean = true,
    val autoPlayAudio: Boolean = false,
    val einkMode: Boolean = false,
    val dailyReminder: Boolean = true,
    val shuffleCards: Boolean = false,
    val difficultFirst: Boolean = false,
    val showDetailFirst: Boolean = false,
    val algorithmType: String = "SM-2",
    val reminderTimeSlots: List<String> = listOf("20:00"),
    val reviewMode: String = "flashcard",
    val baicizhanDeepMode: Boolean = true,
    val bbdcImmersiveMode: Boolean = false,
    val userName: String = "用户",
    val userEmail: String = "user@example.com",
    val studyMode: String = "free",
    val focusedTimeSlots: String = "08:00,12:00,20:00",
    val ttsAutoPlayQuestion: Boolean = false,
    val ttsAutoPlayAnswer: Boolean = true,
    val poetryRecitationEnabled: Boolean = true,
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
            dailyCardLimitEnabled = settingsManager.dailyCardLimitEnabled,
            autoPlayAudio = settingsManager.autoPlayAudio,
            einkMode = settingsManager.einkMode,
            dailyReminder = settingsManager.dailyReminder,
            shuffleCards = settingsManager.shuffleCards,
            difficultFirst = settingsManager.difficultFirst,
            showDetailFirst = settingsManager.showDetailFirst,
            algorithmType = settingsManager.algorithmType,
            reminderTimeSlots = parseSlots(settingsManager.reminderTimeSlots),
            reviewMode = settingsManager.reviewMode,
            baicizhanDeepMode = settingsManager.baicizhanDeepMode,
            bbdcImmersiveMode = settingsManager.bbdcImmersiveMode,
            userName = settingsManager.userName,
            userEmail = settingsManager.userEmail,
            studyMode = settingsManager.studyMode,
            focusedTimeSlots = settingsManager.focusedTimeSlots,
            ttsAutoPlayQuestion = settingsManager.ttsAutoPlayQuestion,
            ttsAutoPlayAnswer = settingsManager.ttsAutoPlayAnswer,
            poetryRecitationEnabled = settingsManager.poetryRecitationEnabled
        )
    }

    private fun parseSlots(slotsStr: String): List<String> {
        return slotsStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun serializeSlots(slots: List<String>): String {
        return slots.joinToString(",")
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
            NotificationScheduler.scheduleReminders(context)
        } else {
            NotificationScheduler.cancelAllReminders(context)
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

    fun toggleTtsAutoPlayQuestion(enabled: Boolean) {
        settingsManager.ttsAutoPlayQuestion = enabled
        _uiState.value = _uiState.value.copy(ttsAutoPlayQuestion = enabled)
    }

    fun toggleTtsAutoPlayAnswer(enabled: Boolean) {
        settingsManager.ttsAutoPlayAnswer = enabled
        _uiState.value = _uiState.value.copy(ttsAutoPlayAnswer = enabled)
    }

    fun togglePoetryRecitation(enabled: Boolean) {
        settingsManager.poetryRecitationEnabled = enabled
        _uiState.value = _uiState.value.copy(poetryRecitationEnabled = enabled)
    }

    fun toggleShuffle(enabled: Boolean) {
        settingsManager.shuffleCards = enabled
        _uiState.value = _uiState.value.copy(shuffleCards = enabled)
    }

    fun toggleDifficultFirst(enabled: Boolean) {
        settingsManager.difficultFirst = enabled
        _uiState.value = _uiState.value.copy(difficultFirst = enabled)
    }

    fun toggleShowDetailFirst(enabled: Boolean) {
        settingsManager.showDetailFirst = enabled
        _uiState.value = _uiState.value.copy(showDetailFirst = enabled)
    }

    fun setDailyCardLimit(limit: Int) {
        val coerced = limit.coerceIn(1, 200)
        settingsManager.dailyCardLimit = coerced
        _uiState.value = _uiState.value.copy(dailyCardLimit = coerced)
    }

    fun toggleDailyCardLimitEnabled(enabled: Boolean) {
        settingsManager.dailyCardLimitEnabled = enabled
        _uiState.value = _uiState.value.copy(dailyCardLimitEnabled = enabled)
    }

    fun setAlgorithmType(type: String) {
        settingsManager.algorithmType = type
        _uiState.value = _uiState.value.copy(algorithmType = type)
    }

    fun setReviewMode(mode: String) {
        settingsManager.reviewMode = mode
        _uiState.value = _uiState.value.copy(reviewMode = mode)
    }

    fun setStudyMode(mode: String) {
        settingsManager.studyMode = mode
        _uiState.value = _uiState.value.copy(studyMode = mode)
        if (settingsManager.dailyReminder) {
            NotificationScheduler.rescheduleReminders(XxMemoryApplication.instance)
        }
    }

    /**
     * 保存集中模式时间点字符串，仅在格式全部合法时返回 true 并持久化。
     */
    fun setFocusedTimeSlots(slots: String): Boolean {
        val parsed = SchedulerUtils.parseFocusedSlots(slots)
        val inputSlots = slots.split(",", "；", ";", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (inputSlots.isNotEmpty() && parsed.size != inputSlots.size) {
            return false
        }
        settingsManager.focusedTimeSlots = parsed.joinToString(",")
        _uiState.value = _uiState.value.copy(focusedTimeSlots = parsed.joinToString(","))
        if (settingsManager.dailyReminder && settingsManager.studyMode == "focused") {
            NotificationScheduler.rescheduleReminders(XxMemoryApplication.instance)
        }
        return true
    }

    fun toggleBaicizhanDeepMode(enabled: Boolean) {
        settingsManager.baicizhanDeepMode = enabled
        _uiState.value = _uiState.value.copy(baicizhanDeepMode = enabled)
    }

    fun toggleBbdcImmersiveMode(enabled: Boolean) {
        settingsManager.bbdcImmersiveMode = enabled
        _uiState.value = _uiState.value.copy(bbdcImmersiveMode = enabled)
    }

    fun addReminderSlot(hour: Int, minute: Int) {
        val slot = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        val current = _uiState.value.reminderTimeSlots.toMutableList()
        if (slot !in current) {
            current.add(slot)
            current.sort()
            settingsManager.reminderTimeSlots = serializeSlots(current)
            _uiState.value = _uiState.value.copy(reminderTimeSlots = current)
            if (settingsManager.dailyReminder) {
                NotificationScheduler.rescheduleReminders(XxMemoryApplication.instance)
            }
        }
    }

    fun removeReminderSlot(slot: String) {
        val current = _uiState.value.reminderTimeSlots.toMutableList()
        if (current.remove(slot)) {
            settingsManager.reminderTimeSlots = serializeSlots(current)
            _uiState.value = _uiState.value.copy(reminderTimeSlots = current)
            if (settingsManager.dailyReminder) {
                NotificationScheduler.rescheduleReminders(XxMemoryApplication.instance)
            }
        }
    }

    /**
     * 向集中模式时间点添加一个时间点，返回是否成功（格式合法且持久化）。
     */
    fun addFocusedSlot(hour: Int, minute: Int): Boolean {
        val slot = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        val current = SchedulerUtils.parseFocusedSlots(settingsManager.focusedTimeSlots).toMutableList()
        if (slot in current) return true
        current.add(slot)
        current.sort()
        return setFocusedTimeSlots(current.joinToString(","))
    }

    /**
     * 从集中模式时间点移除一个时间点，返回是否成功（格式合法且持久化）。
     */
    fun removeFocusedSlot(slot: String): Boolean {
        val current = SchedulerUtils.parseFocusedSlots(settingsManager.focusedTimeSlots).toMutableList()
        if (!current.remove(slot)) return true
        return setFocusedTimeSlots(current.joinToString(","))
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
