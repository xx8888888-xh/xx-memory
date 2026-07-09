package com.xxmemory.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("xx_memory_settings", Context.MODE_PRIVATE)

    var dailyCardLimit: Int
        get() = prefs.getInt("daily_card_limit", 20)
        set(value) = prefs.edit().putInt("daily_card_limit", value).apply()

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

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(value) = prefs.edit().putInt("reminder_hour", value).apply()

    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(value) = prefs.edit().putInt("reminder_minute", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "用户") ?: "用户"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "user@example.com") ?: "user@example.com"
        set(value) = prefs.edit().putString("user_email", value).apply()
}