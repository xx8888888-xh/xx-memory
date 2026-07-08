package com.xxmemory.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.xxmemory.app.data.AppDatabase

class XxMemoryApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "复习提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "艾宾浩斯遗忘曲线复习提醒"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "review_reminder_channel"
        lateinit var instance: XxMemoryApplication
            private set
    }
}