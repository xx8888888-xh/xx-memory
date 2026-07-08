package com.xxmemory.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "question")
    val question: String,

    @ColumnInfo(name = "answer")
    val answer: String,

    @ColumnInfo(name = "detail")
    val detail: String = "",

    @ColumnInfo(name = "subject")
    val subject: String = "",

    @ColumnInfo(name = "difficulty")
    val difficulty: Float = 0f,

    @ColumnInfo(name = "interval")
    val interval: Int = 0,

    @ColumnInfo(name = "ease_factor")
    val easeFactor: Float = 2.5f,

    @ColumnInfo(name = "repetitions")
    val repetitions: Int = 0,

    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "card_type")
    val cardType: String = "qa",

    @ColumnInfo(name = "audio_url")
    val audioUrl: String? = null,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null
)