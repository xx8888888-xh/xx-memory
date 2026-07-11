package com.xxmemory.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.xxmemory.app.domain.NotificationScheduler
import com.xxmemory.app.domain.Scheduler
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "xxmemory:alarm")
        wakeLock.acquire(10_000)
        try {
            try {
                runBlocking {
                    Scheduler.scheduleReviewReminder(context)
                    Scheduler.schedulePoetryReminder(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Reschedule all reminder time slots so the chain is not broken.
            NotificationScheduler.rescheduleReminders(context)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
            pendingResult.finish()
        }
    }
}
