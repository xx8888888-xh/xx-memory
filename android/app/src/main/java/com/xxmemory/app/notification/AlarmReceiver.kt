package com.xxmemory.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.xxmemory.app.domain.NotificationScheduler
import com.xxmemory.app.domain.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "xxmemory:alarm")
        wakeLock.acquire(10_000)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                try {
                    Scheduler.scheduleReviewReminder(context)
                    Scheduler.schedulePoetryReminder(context)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to schedule review/poetry reminder", e)
                }
                // Reschedule all reminder time slots so the chain is not broken.
                NotificationScheduler.rescheduleReminders(context)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
