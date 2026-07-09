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
    fun `SM2 new card with perfect quality schedules 4 days`() {
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
    fun `SM2 hard quality lowers EF`() {
        val before = 2.5f
        val result = SM2Algorithm.calculate(
            quality = 1,
            repetitions = 2,
            easeFactor = before,
            currentInterval = 4
        )
        assertTrue("EF should drop after hard rating", result.nextEaseFactor < before)
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
    fun `Algorithm selector returns expected instances`() {
        assertEquals("SM-2", EbbinghausAlgorithm.getAlgorithm("SM-2").name)
        assertEquals("艾宾浩斯固定", EbbinghausAlgorithm.getAlgorithm("艾宾浩斯固定").name)
        assertEquals("FSRS", EbbinghausAlgorithm.getAlgorithm("FSRS").name)
        assertEquals("SM-2", EbbinghausAlgorithm.getAlgorithm("unknown").name)
    }
}
