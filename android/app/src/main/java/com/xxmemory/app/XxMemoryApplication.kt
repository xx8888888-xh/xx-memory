package com.xxmemory.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.SettingsManager
import com.xxmemory.app.domain.NotificationScheduler
import com.xxmemory.app.R

class XxMemoryApplication : Application() {

    private lateinit var _database: AppDatabase
    val database: AppDatabase get() = _database

    private lateinit var _settingsManager: SettingsManager
    val settingsManager: SettingsManager get() = _settingsManager

    override fun onCreate() {
        super.onCreate()
        _instance = this
        _database = AppDatabase.getInstance(this)
        _settingsManager = SettingsManager(this)
        createNotificationChannel()
        if (_settingsManager.dailyReminder) {
            NotificationScheduler.scheduleReminders(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = descriptionText
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "review_reminder_channel"
        private lateinit var _instance: XxMemoryApplication
        val instance: XxMemoryApplication get() = _instance
    }
}
