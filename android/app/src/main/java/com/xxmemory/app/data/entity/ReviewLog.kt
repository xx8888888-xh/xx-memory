package com.xxmemory.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["card_id"])]
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "card_id")
    val cardId: Long,

    @ColumnInfo(name = "quality")
    val quality: Int,

    @ColumnInfo(name = "review_date")
    val reviewDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "next_interval")
    val nextInterval: Int = 0
)