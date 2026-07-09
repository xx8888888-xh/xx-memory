package com.xxmemory.app.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.notification.AlarmReceiver
import java.util.Calendar

object NotificationScheduler {

    private const val ALARM_REQUEST_CODE_BASE = 8000

    /**
     * Schedule alarms at every configured reminder time slot.
     * Each alarm fires [AlarmReceiver] which checks for due cards and notifies.
     */
    fun scheduleReminders(context: Context): Boolean {
        val app = context.applicationContext as XxMemoryApplication
        val settings = app.settingsManager
        if (!settings.dailyReminder) return false

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return false
            }
        }

        val slots = parseTimeSlots(settings.reminderTimeSlots)
        if (slots.isEmpty()) return false

        slots.forEachIndexed { index, (hour, minute) ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("slot_index", index)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE_BASE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        return true
    }

    /**
     * Cancel all scheduled reminder alarms.
     */
    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val app = context.applicationContext as XxMemoryApplication
        val settings = app.settingsManager
        val slots = parseTimeSlots(settings.reminderTimeSlots)
        slots.forEachIndexed { index, _ ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE_BASE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Reschedule all reminders (call after time slots change).
     */
    fun rescheduleReminders(context: Context) {
        cancelAllReminders(context)
        scheduleReminders(context)
    }

    private fun parseTimeSlots(slotsStr: String): List<Pair<Int, Int>> {
        return slotsStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { slot ->
                val parts = slot.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: return@mapNotNull null
                    val m = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: return@mapNotNull null
                    Pair(h, m)
                } else null
            }
            .distinct()
    }
}
