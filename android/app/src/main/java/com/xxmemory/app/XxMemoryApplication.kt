package com.xxmemory.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.SettingsManager
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.domain.NotificationScheduler
import com.xxmemory.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        importDefaultCardsIfNeeded()
    }

    private fun importDefaultCardsIfNeeded() {
        if (_settingsManager.defaultCardsImported) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = assets.open("default_cards.json").bufferedReader().use { it.readText() }
                val array = JsonParser.parseString(json).asJsonArray
                var count = 0
                for (element in array) {
                    val obj = element.asJsonObject
                    val card = mapJsonToCard(obj)
                    _database.cardDao().insertCard(card)
                    count++
                }
                _settingsManager.defaultCardsImported = true
                android.util.Log.i("XxMemory", "Imported $count default cards")
            } catch (e: Exception) {
                android.util.Log.e("XxMemory", "Failed to import default cards", e)
            }
        }
    }

    private fun mapJsonToCard(json: JsonObject): Card {
        return Card(
            question = json.get("question")?.asString ?: "",
            answer = json.get("answer")?.asString ?: "",
            detail = json.get("detail")?.asString ?: "",
            subject = json.get("subject")?.asString ?: "",
            cardType = json.get("cardType")?.asString ?: json.get("card_type")?.asString ?: "qa",
            tags = json.get("tags")?.asString ?: "",
            audioUrl = json.get("audioUrl")?.asString ?: json.get("audio_url")?.asString,
            imageUrl = json.get("imageUrl")?.asString ?: json.get("image_url")?.asString,
            isFavorite = json.get("isFavorite")?.asBoolean ?: json.get("is_favorite")?.asBoolean ?: false,
            phonetic = json.get("phonetic")?.asString ?: "",
            example = json.get("example")?.asString ?: "",
            collocations = json.get("collocations")?.asString ?: "",
            etymology = json.get("etymology")?.asString ?: "",
            hint = json.get("hint")?.asString ?: "",
            rhyme = json.get("rhyme")?.asString ?: "",
            derivatives = json.get("derivatives")?.asString ?: "",
            distractors = json.get("distractors")?.asString ?: json.get("options")?.asString ?: "",
            mastered = false,
            nextReviewDate = System.currentTimeMillis(),
            learningStage = 0
        )
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
