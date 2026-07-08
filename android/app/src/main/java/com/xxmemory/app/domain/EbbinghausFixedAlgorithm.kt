package com.xxmemory.app.domain

/**
 * Ebbinghaus fixed interval algorithm.
 * Based on the classic forgetting curve research:
 * 1st review: 1 day, 2nd: 2 days, 3rd: 4 days, 4th: 7 days,
 * 5th: 15 days, 6th: 30 days, 7th: 60 days, 8th: 120 days
 */
object EbbinghausFixedAlgorithm : MemoryAlgorithm {
    override val name = "艾宾浩斯固定"
    override val description = "基于艾宾浩斯遗忘曲线的固定复习间隔"

    private val fixedIntervals = intArrayOf(1, 2, 4, 7, 15, 30, 60, 120, 180, 365)

    override fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): EbbinghausAlgorithm.ScheduleResult {
        val clampedQuality = quality.coerceIn(0, 3)

        if (clampedQuality < 2) {
            // Failed - reset to first interval
            return EbbinghausAlgorithm.ScheduleResult(
                nextInterval = fixedIntervals[0],
                nextEaseFactor = easeFactor,
                nextRepetitions = 0,
                nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(fixedIntervals[0])
            )
        }

        val nextIndex = repetitions.coerceAtMost(fixedIntervals.size - 1)
        val nextInterval = fixedIntervals[nextIndex]

        return EbbinghausAlgorithm.ScheduleResult(
            nextInterval = nextInterval,
            nextEaseFactor = easeFactor,
            nextRepetitions = repetitions + 1,
            nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(nextInterval)
        )
    }
}