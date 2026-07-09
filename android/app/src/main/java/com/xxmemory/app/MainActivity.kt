package com.xxmemory.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xxmemory.app.domain.NotificationScheduler
import com.xxmemory.app.ui.navigation.AppNavigation
import com.xxmemory.app.ui.theme.XxMemoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = this
            val prefs = context.getSharedPreferences("xx_memory_settings", Context.MODE_PRIVATE)
            var einkMode by remember { mutableStateOf(prefs.getBoolean("eink_mode", false)) }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "eink_mode") {
                        einkMode = prefs.getBoolean("eink_mode", false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            // Runtime permission for notifications on Android 13+.
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(context, "通知权限被拒绝，将无法收到复习提醒", Toast.LENGTH_LONG).show()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                ensureDailyReminderScheduled(context)
            }

            SideEffect {
                val barColor = if (einkMode) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#FAF6F2")
                window.statusBarColor = barColor
                window.navigationBarColor = if (einkMode) android.graphics.Color.WHITE
                else android.graphics.Color.WHITE

                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }

            XxMemoryTheme(einkMode = einkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun ensureDailyReminderScheduled(context: Context) {
        val settings = (context.applicationContext as XxMemoryApplication).settingsManager
        if (!settings.dailyReminder) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "需要精确闹钟权限才能启用每日复习提醒", Toast.LENGTH_LONG).show()
                return
            }
        }
        NotificationScheduler.scheduleReminders(context)
    }
}
