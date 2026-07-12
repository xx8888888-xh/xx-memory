package com.xxmemory.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 单元测试：验证三种记忆算法的计算结果符合预期。
 * 这些算法不依赖 Android 框架，可在 JVM 上直接运行。
 */
class MemoryAlgorithmTest {

    @Test
    fun `SM2 new card with perfect quality schedules 1 day`() {
        val result = SM2Algorithm.calculate(
            quality = 3,
            repetitions = 0,
            easeFactor = 2.5f,
            currentInterval = 0
        )
        assertEquals(1, result.nextInterval)
        assertEquals(1, result.nextRepetitions)
        assertTrue(result.nextEaseFactor > 2.4f)
        assertTrue(result.nextReviewDate > System.currentTimeMillis())
    }

    @Test
    fun `SM2 second success schedules 4 days`() {
        val result = SM2Algorithm.calculate(
            quality = 3,
            repetitions = 1,
            easeFactor = 2.5f,
            currentInterval = 1
        )
        assertEquals(4, result.nextInterval)
        assertEquals(2, result.nextRepetitions)
    }

    @Test
    fun `SM2 subsequent success uses EF multiplier`() {
        val result = SM2Algorithm.calculate(
            quality = 3,
            repetitions = 2,
            easeFactor = 2.5f,
            currentInterval = 4
        )
        assertEquals(10, result.nextInterval) // 4 * 2.5 = 10
        assertEquals(3, result.nextRepetitions)
    }

    @Test
    fun `SM2 successful reviews produce monotonically growing intervals`() {
        var reps = 0
        var interval = 0
        var ef = 2.5f
        val intervals = mutableListOf<Int>()

        repeat(5) {
            val result = SM2Algorithm.calculate(
                quality = 3,
                repetitions = reps,
                easeFactor = ef,
                currentInterval = interval
            )
            intervals.add(result.nextInterval)
            reps = result.nextRepetitions
            interval = result.nextInterval
            ef = result.nextEaseFactor
        }

        for (i in 1 until intervals.size) {
            assertTrue(
                "Interval should grow: ${intervals[i - 1]} -> ${intervals[i]}",
                intervals[i] > intervals[i - 1]
            )
        }
    }

    @Test
    fun `SM2 failure resets repetitions and interval`() {
        val result = SM2Algorithm.calculate(
            quality = 0,
            repetitions = 5,
            easeFactor = 2.5f,
            currentInterval = 30
        )
        assertEquals(1, result.nextInterval)
        assertEquals(0, result.nextRepetitions)
    }

    @Test
    fun `SM2 hard quality lowers EF but still advances interval`() {
        val before = 2.5f
        val result = SM2Algorithm.calculate(
            quality = 1,
            repetitions = 2,
            easeFactor = before,
            currentInterval = 4
        )
        assertTrue("EF should drop after hard rating", result.nextEaseFactor < before)
        assertTrue("Interval should still advance on hard but correct recall", result.nextInterval > 4)
    }

    @Test
    fun `SM2 complete failure keeps EF at least at minimum`() {
        val before = 2.5f
        val result = SM2Algorithm.calculate(
            quality = 0,
            repetitions = 5,
            easeFactor = before,
            currentInterval = 30
        )
        assertTrue("EF should not drop below SM-2 minimum", result.nextEaseFactor >= 1.3f)
    }

    @Test
    fun `Ebbinghaus follows fixed sequence`() {
        val intervals = intArrayOf(1, 2, 4, 7, 15, 30, 60, 180)
        var reps = 0
        var current = 0
        for (expected in intervals) {
            val result = EbbinghausFixedAlgorithm.calculate(
                quality = 3,
                repetitions = reps,
                easeFactor = 2.5f,
                currentInterval = current
            )
            assertEquals(expected, result.nextInterval)
            reps = result.nextRepetitions
            current = result.nextInterval
        }
    }

    @Test
    fun `Ebbinghaus fixed sequence step by step`() {
        val expected = listOf(1, 2, 4, 7, 15, 30, 60, 180)
        var reps = 0
        var current = 0

        for ((index, days) in expected.withIndex()) {
            val result = EbbinghausFixedAlgorithm.calculate(
                quality = 3,
                repetitions = reps,
                easeFactor = 2.5f,
                currentInterval = current
            )
            assertEquals("Step $index should be $days days", days, result.nextInterval)
            reps = result.nextRepetitions
            current = result.nextInterval
        }

        assertEquals(expected.size, reps)
    }

    @Test
    fun `Ebbinghaus failure resets to first interval`() {
        val result = EbbinghausFixedAlgorithm.calculate(
            quality = 0,
            repetitions = 5,
            easeFactor = 2.5f,
            currentInterval = 30
        )
        assertEquals(1, result.nextInterval)
        assertEquals(0, result.nextRepetitions)
    }

    @Test
    fun `FSRS new card produces positive interval`() {
        val result = FsrsAlgorithm.calculate(
            quality = 3,
            repetitions = 0,
            easeFactor = 2.5f,
            currentInterval = 0
        )
        assertTrue(result.nextInterval > 0)
        assertTrue(result.nextDifficulty in 1.0..10.0)
        assertTrue(result.nextReviewDate > System.currentTimeMillis())
    }

    @Test
    fun `FSRS recall increases interval`() {
        val first = FsrsAlgorithm.calculate(
            quality = 3,
            repetitions = 0,
            easeFactor = 2.5f,
            currentInterval = 0
        )
        val second = FsrsAlgorithm.calculate(
            quality = 3,
            repetitions = first.nextRepetitions,
            easeFactor = first.nextEaseFactor,
            currentInterval = first.nextInterval
        )
        assertTrue("Interval should grow on successful recall", second.nextInterval > first.nextInterval)
    }

    @Test
    fun `FSRS forget resets repetitions and shrinks interval`() {
        val success = FsrsAlgorithm.calculate(
            quality = 3,
            repetitions = 2,
            easeFactor = 2.5f,
            currentInterval = 10
        )
        val fail = FsrsAlgorithm.calculate(
            quality = 0,
            repetitions = success.nextRepetitions,
            easeFactor = success.nextEaseFactor,
            currentInterval = success.nextInterval
        )
        assertEquals(0, fail.nextRepetitions)
        assertTrue("Interval should shrink after forget", fail.nextInterval < success.nextInterval)
    }

    @Test
    fun `FSRS stability and difficulty change monotonically in expected directions`() {
        // Easy recall: interval/stability grow, difficulty drops
        val first = FsrsAlgorithm.calculate(
            quality = 3,
            repetitions = 0,
            easeFactor = 2.5f,
            currentInterval = 0
        )
        val easyRecall = FsrsAlgorithm.calculate(
            quality = 3,
            repetitions = first.nextRepetitions,
            easeFactor = first.nextEaseFactor,
            currentInterval = first.nextInterval
        )
        assertTrue("Interval should grow on easy recall", easyRecall.nextInterval > first.nextInterval)
        assertTrue(
            "Difficulty should drop or stay on easy recall",
            easyRecall.nextDifficulty <= first.nextDifficulty
        )

        // Hard recall: interval still grows (slower), difficulty rises
        val hardRecall = FsrsAlgorithm.calculate(
            quality = 1,
            repetitions = easyRecall.nextRepetitions,
            easeFactor = easyRecall.nextEaseFactor,
            currentInterval = easyRecall.nextInterval
        )
        assertTrue("Interval should grow on hard recall", hardRecall.nextInterval > easyRecall.nextInterval)
        assertTrue(
            "Difficulty should rise or stay on hard recall",
            hardRecall.nextDifficulty >= easyRecall.nextDifficulty
        )

        // Forget: interval shrinks, repetitions reset
        val forget = FsrsAlgorithm.calculate(
            quality = 0,
            repetitions = hardRecall.nextRepetitions,
            easeFactor = hardRecall.nextEaseFactor,
            currentInterval = hardRecall.nextInterval
        )
        assertEquals(0, forget.nextRepetitions)
        assertTrue("Interval should shrink after forget", forget.nextInterval < hardRecall.nextInterval)
    }

    @Test
    fun `FSRS intervals are always positive`() {
        val qualities = listOf(0, 1, 2, 3)
        val repetitions = listOf(0, 1, 2, 5)
        val intervals = listOf(0, 1, 4, 10, 30)

        for (q in qualities) {
            for (r in repetitions) {
                for (i in intervals) {
                    val result = FsrsAlgorithm.calculate(
                        quality = q,
                        repetitions = r,
                        easeFactor = 2.5f,
                        currentInterval = i
                    )
                    assertTrue(
                        "FSRS interval must be positive for q=$q r=$r i=$i",
                        result.nextInterval > 0
                    )
                }
            }
        }
    }

    @Test
    fun `Algorithm selector returns expected instances`() {
        assertEquals("SM-2", EbbinghausAlgorithm.getAlgorithm("SM-2").name)
        assertEquals("艾宾浩斯固定", EbbinghausAlgorithm.getAlgorithm("艾宾浩斯固定").name)
        assertEquals("FSRS", EbbinghausAlgorithm.getAlgorithm("FSRS").name)
        assertEquals("SM-2", EbbinghausAlgorithm.getAlgorithm("unknown").name)
    }
}
