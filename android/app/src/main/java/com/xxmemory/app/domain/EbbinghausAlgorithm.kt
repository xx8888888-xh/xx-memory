package com.xxmemory.app.domain

import java.util.Calendar

object EbbinghausAlgorithm {

    /**
     * SM-2 algorithm implementation.
     * @param quality 0-4 (0=forget, 1=hard, 2=good, 3=easy)
     * @param repetitions Current number of successful repetitions
     * @param easeFactor Current ease factor (minimum 1.3)
     * @param currentInterval Current interval in days
     * @return Triple of (nextInterval, nextEaseFactor, nextReviewDate)
     */
    fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): ScheduleResult {
        val clampedQuality = quality.coerceIn(0, 3)

        if (clampedQuality < 2) {
            // Failed recall - reset
            return ScheduleResult(
                nextInterval = 1,
                nextEaseFactor = easeFactor,
                nextRepetitions = 0,
                nextReviewDate = getNextDayTimestamp(1)
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

        return ScheduleResult(
            nextInterval = nextInterval.coerceAtLeast(1),
            nextEaseFactor = newEaseFactor,
            nextRepetitions = nextRepetitions,
            nextReviewDate = getNextDayTimestamp(nextInterval)
        )
    }

    private fun calculateEaseFactor(currentEf: Float, quality: Int): Float {
        val newEf = currentEf + (0.1f - (3 - quality) * (0.08f + (3 - quality) * 0.02f))
        return newEf.coerceAtLeast(1.3f)
    }

    private fun getNextDayTimestamp(daysFromNow: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysFromNow)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    data class ScheduleResult(
        val nextInterval: Int,
        val nextEaseFactor: Float,
        val nextRepetitions: Int,
        val nextReviewDate: Long
    )
}