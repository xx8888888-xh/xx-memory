package com.xxmemory.app.domain

import android.util.Log
import java.util.Calendar

object EbbinghausAlgorithm {

    fun getAlgorithm(type: String): MemoryAlgorithm {
        return when (type) {
            "艾宾浩斯固定" -> EbbinghausFixedAlgorithm
            "FSRS" -> FsrsAlgorithm
            else -> {
                Log.w("EbbinghausAlgorithm", "未知算法类型: $type, 降级为SM-2")
                SM2Algorithm
            }
        }
    }

    // SM-2 implementation (delegates to SM2Algorithm)
    fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): ScheduleResult {
        return SM2Algorithm.calculate(quality, repetitions, easeFactor, currentInterval)
    }

    internal fun getNextDayTimestamp(daysFromNow: Int): Long {
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