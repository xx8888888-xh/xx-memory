package com.xxmemory.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xxmemory.app.domain.Scheduler

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.scheduleReviewReminder(context)
    }
}