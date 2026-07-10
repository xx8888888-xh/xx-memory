package com.xxmemory.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("xx_memory_settings", Context.MODE_PRIVATE)

    var dailyCardLimit: Int
        get() = prefs.getInt("daily_card_limit", 20)
        set(value) = prefs.edit().putInt("daily_card_limit", value).apply()

    var dailyCardLimitEnabled: Boolean
        get() = prefs.getBoolean("daily_card_limit_enabled", true)
        set(value) = prefs.edit().putBoolean("daily_card_limit_enabled", value).apply()

    var autoPlayAudio: Boolean
        get() = prefs.getBoolean("auto_play_audio", false)
        set(value) = prefs.edit().putBoolean("auto_play_audio", value).apply()

    var einkMode: Boolean
        get() = prefs.getBoolean("eink_mode", false)
        set(value) = prefs.edit().putBoolean("eink_mode", value).apply()

    var dailyReminder: Boolean
        get() = prefs.getBoolean("daily_reminder", true)
        set(value) = prefs.edit().putBoolean("daily_reminder", value).apply()

    var shuffleCards: Boolean
        get() = prefs.getBoolean("shuffle_cards", false)
        set(value) = prefs.edit().putBoolean("shuffle_cards", value).apply()

    var showDetailFirst: Boolean
        get() = prefs.getBoolean("show_detail_first", false)
        set(value) = prefs.edit().putBoolean("show_detail_first", value).apply()

    var algorithmType: String
        get() = prefs.getString("algorithm_type", "SM-2") ?: "SM-2"
        set(value) = prefs.edit().putString("algorithm_type", value).apply()

    /**
     * Comma-separated reminder time slots in "HH:mm" format, e.g. "08:00,12:00,20:00".
     * Reminders fire at each slot when there are due cards.
     */
    var reminderTimeSlots: String
        get() = prefs.getString("reminder_time_slots", "20:00") ?: "20:00"
        set(value) = prefs.edit().putString("reminder_time_slots", value).apply()

    /**
     * Review mode. Supported values:
     * - "flashcard": 经典闪卡（翻面自评）
     * - "baicizhan": 百词斩（看词选义 + 斩 + 拼写）
     * - "bbdc": 不背单词（认识/不认识 + 选义 + 例句）
     * 已不再支持混合模式，自由切换请在复习界面顶部切换器完成。
     */
    var reviewMode: String
        get() = (prefs.getString("review_mode", "flashcard") ?: "flashcard").let {
            if (it == "mixed") "flashcard" else it
        }
        set(value) = prefs.edit().putString("review_mode", value).apply()

    /**
     * 百词斩深度模式：开启后详情页展示词根词缀、词组搭配、押韵等完整内容。
     * 关闭时为轻快模式，仅显示核心释义和例句。
     */
    var baicizhanDeepMode: Boolean
        get() = prefs.getBoolean("baicizhan_deep_mode", true)
        set(value) = prefs.edit().putBoolean("baicizhan_deep_mode", value).apply()

    /**
     * 不背单词沉浸刷词模式：开启后复习界面全屏极简，自动朗读并隐藏进度等干扰元素。
     */
    var bbdcImmersiveMode: Boolean
        get() = prefs.getBoolean("bbdc_immersive_mode", false)
        set(value) = prefs.edit().putBoolean("bbdc_immersive_mode", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "用户") ?: "用户"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "user@example.com") ?: "user@example.com"
        set(value) = prefs.edit().putString("user_email", value).apply()

    /**
     * 复习时间安排模式。
     * - "free"：自由模式，由算法自主安排。
     * - "focused"：集中模式，算法结果迁就到用户指定时间点。
     */
    var studyMode: String
        get() = prefs.getString("study_mode", "free") ?: "free"
        set(value) = prefs.edit().putString("study_mode", value).apply()

    /**
     * 集中模式下的用户指定时间点，逗号分隔 HH:mm，例如 "08:00,12:00,20:00"。
     */
    var focusedTimeSlots: String
        get() = prefs.getString("focused_time_slots", "08:00,12:00,20:00") ?: "08:00,12:00,20:00"
        set(value) = prefs.edit().putString("focused_time_slots", value).apply()

    /** 进入卡片时是否自动朗读问题（或播放音频）。 */
    var ttsAutoPlayQuestion: Boolean
        get() = prefs.getBoolean("tts_auto_play_question", false)
        set(value) = prefs.edit().putBoolean("tts_auto_play_question", value).apply()

    /** 揭晓答案后是否自动朗读答案（或播放音频）。 */
    var ttsAutoPlayAnswer: Boolean
        get() = prefs.getBoolean("tts_auto_play_answer", true)
        set(value) = prefs.edit().putBoolean("tts_auto_play_answer", value).apply()
}