package com.xxmemory.app.domain

import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.entity.ReviewLog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 单元测试：验证困难优先排序器能正确识别并优先排列困难卡片。
 */
class DifficultFirstSorterTest {

    @Test
    fun `empty or single list returns unchanged`() {
        val now = 1000000L
        assertEquals(emptyList<Card>(), DifficultFirstSorter.sort(emptyList(), emptyList(), "SM-2", now))

        val single = newCard(id = 1, question = "a")
        assertEquals(listOf(single), DifficultFirstSorter.sort(listOf(single), emptyList(), "SM-2", now))
    }

    @Test
    fun `FSRS higher difficulty cards are sorted first`() {
        val now = System.currentTimeMillis()
        val easy = newCard(id = 1, difficulty = 2f, repetitions = 3)
        val hard = newCard(id = 2, difficulty = 8f, repetitions = 3)

        val sorted = DifficultFirstSorter.sort(listOf(easy, hard), emptyList(), "FSRS", now)

        assertEquals(hard.id, sorted[0].id)
        assertEquals(easy.id, sorted[1].id)
    }

    @Test
    fun `SM2 lower ease factor means harder and sorted first`() {
        val now = System.currentTimeMillis()
        val easy = newCard(id = 1, easeFactor = 2.5f, repetitions = 3)
        val hard = newCard(id = 2, easeFactor = 1.3f, repetitions = 3)

        val sorted = DifficultFirstSorter.sort(listOf(easy, hard), emptyList(), "SM-2", now)

        assertEquals(hard.id, sorted[0].id)
        assertEquals(easy.id, sorted[1].id)
    }

    @Test
    fun `cards with more historical failures are sorted first`() {
        val now = System.currentTimeMillis()
        val goodCard = newCard(id = 1)
        val badCard = newCard(id = 2)

        val logs = listOf(
            ReviewLog(cardId = goodCard.id, quality = 3, reviewDate = now - 1000),
            ReviewLog(cardId = goodCard.id, quality = 3, reviewDate = now - 2000),
            ReviewLog(cardId = badCard.id, quality = 0, reviewDate = now - 1000),
            ReviewLog(cardId = badCard.id, quality = 1, reviewDate = now - 2000)
        )

        val sorted = DifficultFirstSorter.sort(listOf(goodCard, badCard), logs, "SM-2", now)

        assertEquals(badCard.id, sorted[0].id)
        assertEquals(goodCard.id, sorted[1].id)
    }

    @Test
    fun `recent failures boost priority beyond historical average`() {
        val now = System.currentTimeMillis()
        val oldFailureCard = newCard(id = 1)
        val recentFailureCard = newCard(id = 2)

        val logs = listOf(
            // Old failure card: one failure long ago
            ReviewLog(cardId = oldFailureCard.id, quality = 0, reviewDate = now - 30L * 24 * 60 * 60 * 1000),
            // Recent failure card: one failure within window
            ReviewLog(cardId = recentFailureCard.id, quality = 0, reviewDate = now - 1L * 24 * 60 * 60 * 1000)
        )

        val sorted = DifficultFirstSorter.sort(listOf(oldFailureCard, recentFailureCard), logs, "SM-2", now)

        assertEquals(recentFailureCard.id, sorted[0].id)
        assertEquals(oldFailureCard.id, sorted[1].id)
    }

    @Test
    fun `new cards without history get medium priority between easy and hard cards`() {
        val now = System.currentTimeMillis()
        val newCard = newCard(id = 1, repetitions = 0, easeFactor = 2.5f)
        val masteredEasy = newCard(id = 2, repetitions = 5, easeFactor = 2.5f)
        val struggling = newCard(id = 3, repetitions = 5, easeFactor = 1.3f)

        val logs = listOf(
            ReviewLog(cardId = masteredEasy.id, quality = 3, reviewDate = now - 1000),
            ReviewLog(cardId = struggling.id, quality = 0, reviewDate = now - 1000)
        )

        val sorted = DifficultFirstSorter.sort(listOf(newCard, masteredEasy, struggling), logs, "SM-2", now)

        // Hardest first, newest in the middle, easiest last
        assertEquals(struggling.id, sorted[0].id)
        assertEquals(newCard.id, sorted[1].id)
        assertEquals(masteredEasy.id, sorted[2].id)
    }

    private fun newCard(
        id: Long,
        question: String = "Q$id",
        difficulty: Float = 0f,
        easeFactor: Float = 2.5f,
        repetitions: Int = 0
    ): Card {
        return Card(
            id = id,
            question = question,
            answer = "A$id",
            difficulty = difficulty,
            easeFactor = easeFactor,
            repetitions = repetitions
        )
    }
}
