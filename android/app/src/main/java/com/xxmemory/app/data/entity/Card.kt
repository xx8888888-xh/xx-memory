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

    /** SM-2 算法的 EF (Easiness Factor)，范围 1.3-2.5。FSRS 算法复用此字段存储难度值 */
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
    val imageUrl: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String = "",

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    /** 音标（单词类卡片） */
    @ColumnInfo(name = "phonetic")
    val phonetic: String = "",

    /** 例句（真实语境） */
    @ColumnInfo(name = "example")
    val example: String = "",

    /** 词组搭配 / 考点 */
    @ColumnInfo(name = "collocations")
    val collocations: String = "",

    /** 词根词缀 / 词源 */
    @ColumnInfo(name = "etymology")
    val etymology: String = "",

    /** 提示信息，对应百词斩的"提示" */
    @ColumnInfo(name = "hint")
    val hint: String = "",

    /** 押韵词 / 联想词，对应百词斩押韵模式 */
    @ColumnInfo(name = "rhyme")
    val rhyme: String = "",

    /** 派生词，对应不背单词派生词汇树 */
    @ColumnInfo(name = "derivatives")
    val derivatives: String = "",

    /** 已掌握 / 已斩：掌握后不再参与常规复习 */
    @ColumnInfo(name = "mastered")
    val mastered: Boolean = false
) {
    companion object {
        const val TYPE_QA = "qa"
        const val TYPE_FILL_BLANK = "fill_blank"
        const val TYPE_CODE = "code"
        const val TYPE_IMAGE = "image"
        const val TYPE_AUDIO = "audio"
    }
}