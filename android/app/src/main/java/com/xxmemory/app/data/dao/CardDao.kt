package com.xxmemory.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xxmemory.app.data.entity.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards WHERE next_review_date <= :today AND mastered = 0 ORDER BY next_review_date ASC")
    fun getDueCards(today: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE next_review_date <= :today AND mastered = 0 ORDER BY next_review_date ASC")
    suspend fun getDueCardsList(today: Long): List<Card>

    @Query("SELECT * FROM cards WHERE subject = :subject ORDER BY created_at DESC")
    fun getCardsBySubject(subject: String): Flow<List<Card>>

    @Query("SELECT COUNT(*) FROM cards")
    fun getTotalCards(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE next_review_date <= :today AND mastered = 0")
    fun getDueCount(today: Long): Flow<Int>

    /** All non-mastered cards, useful for generating distractor options. */
    @Query("SELECT * FROM cards WHERE mastered = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getDistractorCandidates(limit: Int): List<Card>

    @Query("SELECT COUNT(*) FROM review_logs WHERE review_date >= :startOfDay AND review_date < :endOfDay")
    fun getTodayReviewedCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_logs WHERE review_date >= :startOfDay AND review_date < :endOfDay")
    suspend fun getTodayReviewedCountSync(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getTotalCardsSync(): Int

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): Card?

    @Update
    suspend fun updateCard(card: Card)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: Card): Long

    @Query("SELECT DISTINCT subject FROM cards WHERE subject != ''")
    fun getSubjects(): Flow<List<String>>

    @Query("SELECT * FROM cards ORDER BY created_at DESC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards ORDER BY created_at DESC")
    suspend fun getAllCardsSync(): List<Card>

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: Long)

    @Query("SELECT * FROM cards WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE tags LIKE '%' || :tag || '%' ORDER BY created_at DESC")
    fun getCardsByTag(tag: String): Flow<List<Card>>

    @Query("SELECT tags FROM cards WHERE tags != ''")
    fun getAllTags(): Flow<List<String>>

    @Query("UPDATE cards SET is_favorite = NOT is_favorite WHERE id = :cardId")
    suspend fun toggleFavorite(cardId: Long)
}