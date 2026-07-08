package com.xxmemory.app.domain

object SM2Algorithm : MemoryAlgorithm {
    override val name = "SM-2"
    override val description = "经典间隔重复算法，使用难度因子(EF)动态调整复习间隔"

    override fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): EbbinghausAlgorithm.ScheduleResult {
        val clampedQuality = quality.coerceIn(0, 3)

        if (clampedQuality < 2) {
            return EbbinghausAlgorithm.ScheduleResult(
                nextInterval = 1,
                nextEaseFactor = easeFactor,
                nextRepetitions = 0,
                nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(1)
            )
        }

        val newEaseFactor = calculateEaseFactor(easeFactor, clampedQuality)

        val nextInterval: Int
        val nextRepetitions: Int

        when (repetitions) {
            0 -> {
                nextInterval = 1
                nextRepetitions = 1
            }
            1 -> {
                nextInterval = 4
                nextRepetitions = 2
            }
            else -> {
                nextInterval = Math.round(currentInterval * newEaseFactor)
                nextRepetitions = repetitions + 1
            }
        }

        return EbbinghausAlgorithm.ScheduleResult(
            nextInterval = nextInterval.coerceAtLeast(1),
            nextEaseFactor = newEaseFactor,
            nextRepetitions = nextRepetitions,
            nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(nextInterval)
        )
    }

    private fun calculateEaseFactor(currentEf: Float, quality: Int): Float {
        val newEf = currentEf + (0.1f - (3 - quality) * (0.08f + (3 - quality) * 0.02f))
        return newEf.coerceAtLeast(1.3f)
    }
}