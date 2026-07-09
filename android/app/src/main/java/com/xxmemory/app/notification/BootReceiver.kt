package com.xxmemory.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.domain.NotificationScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? XxMemoryApplication ?: return
        if (app.settingsManager.dailyReminder) {
            NotificationScheduler.scheduleDailyReminder(context)
        }
    }
}
