package com.xxmemory.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xxmemory.app.data.entity.ReviewLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLog): Long

    @Query("SELECT * FROM review_logs WHERE card_id = :cardId ORDER BY review_date DESC")
    fun getLogsForCard(cardId: Long): Flow<List<ReviewLog>>

    @Query("SELECT COUNT(*) FROM review_logs WHERE review_date >= :startOfDay AND review_date < :endOfDay")
    suspend fun getTodayCount(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE review_date >= :startOfWeek AND review_date < :endOfWeek")
    suspend fun getWeekCount(startOfWeek: Long, endOfWeek: Long): Int

    @Query("SELECT review_date FROM review_logs WHERE review_date >= :startOfWeek AND review_date < :endOfWeek GROUP BY review_date ORDER BY review_date")
    suspend fun getDailyReviewDates(startOfWeek: Long, endOfWeek: Long): List<Long>

    @Query("SELECT COUNT(*) FROM review_logs WHERE review_date >= :dayStart AND review_date < :dayEnd")
    suspend fun getCountForDay(dayStart: Long, dayEnd: Long): Int

    @Query("SELECT COUNT(DISTINCT review_date / 86400000) FROM review_logs WHERE review_date >= :startOfWeek AND review_date < :endOfWeek")
    suspend fun getStudyDaysInWeek(startOfWeek: Long, endOfWeek: Long): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE review_date >= :afterDate")
    suspend fun getCountAfter(afterDate: Long): Int
}