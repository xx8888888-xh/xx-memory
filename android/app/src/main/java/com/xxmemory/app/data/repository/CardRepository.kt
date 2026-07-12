package com.xxmemory.app.data.repository

import com.xxmemory.app.data.dao.CardDao
import com.xxmemory.app.data.dao.ReviewLogDao
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.entity.ReviewLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class CardRepository(
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao
) {

    fun getDueCards(today: Long): Flow<List<Card>> = cardDao.getDueCards(today)

    suspend fun getDueCardsList(today: Long): List<Card> = cardDao.getDueCardsList(today)

    fun getCardsBySubject(subject: String): Flow<List<Card>> = cardDao.getCardsBySubject(subject)

    fun getTotalCards(): Flow<Int> = cardDao.getTotalCards()

    fun getDueCount(today: Long): Flow<Int> = cardDao.getDueCount(today)

    fun getTodayReviewedCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        cardDao.getTodayReviewedCount(startOfDay, endOfDay)

    suspend fun getTodayReviewedCountSync(startOfDay: Long, endOfDay: Long): Int =
        cardDao.getTodayReviewedCountSync(startOfDay, endOfDay)

    suspend fun getTotalCardsSync(): Int = cardDao.getTotalCardsSync()

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id)

    suspend fun updateCard(card: Card) = cardDao.updateCard(card)

    suspend fun insertCard(card: Card): Long = cardDao.insertCard(card)

    suspend fun insertAllCards(cards: List<Card>): List<Long> = cardDao.insertAll(cards)

    suspend fun insertReviewLog(log: ReviewLog): Long = reviewLogDao.insert(log)

    fun getLogsForCard(cardId: Long): Flow<List<ReviewLog>> = reviewLogDao.getLogsForCard(cardId)

    fun getSubjects(): Flow<List<String>> = cardDao.getSubjects()

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards()

    suspend fun getAllCardsSync(): List<Card> = cardDao.getAllCardsSync()

    suspend fun getTodayReviewCount(startOfDay: Long, endOfDay: Long): Int =
        reviewLogDao.getTodayCount(startOfDay, endOfDay)

    suspend fun getWeekCount(startOfWeek: Long, endOfWeek: Long): Int =
        reviewLogDao.getWeekCount(startOfWeek, endOfWeek)

    suspend fun getCountForDay(dayStart: Long, dayEnd: Long): Int =
        reviewLogDao.getCountForDay(dayStart, dayEnd)

    suspend fun getCountAfter(afterDate: Long): Int =
        reviewLogDao.getCountAfter(afterDate)

    suspend fun getAllReviewLogsSync(): List<ReviewLog> =
        reviewLogDao.getAllLogsSync()

    suspend fun deleteCard(id: Long) = cardDao.deleteCard(id)

    fun getFavoriteCards(): Flow<List<Card>> = cardDao.getFavoriteCards()

    fun getCardsByTag(tag: String): Flow<List<Card>> = cardDao.getCardsByTag(tag)

    fun getAllTags(): Flow<List<String>> = cardDao.getAllTags().map { tagsList ->
        tagsList
            .flatMap { it.split(",").map { tag -> tag.trim() } }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    suspend fun toggleFavorite(cardId: Long) = cardDao.toggleFavorite(cardId)

    suspend fun getCardsForCalendar(start: Long, end: Long): List<Card> =
        cardDao.getCardsForCalendar(start, end)

    suspend fun getDueCountForDay(start: Long, end: Long): Int =
        cardDao.getDueCountForDay(start, end)

    suspend fun getDueCountBetween(start: Long, end: Long): Int =
        cardDao.getDueCountBetween(start, end)

    suspend fun getDueCountByTypeSync(today: Long, type: String): Int =
        cardDao.getDueCountByTypeSync(today, type)

    companion object {
        fun getStartOfDay(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfDay(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        fun getNextDayStart(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getStartOfWeek(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfWeek(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = getStartOfWeek(timestamp)
            cal.add(Calendar.DAY_OF_WEEK, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }
    }
}