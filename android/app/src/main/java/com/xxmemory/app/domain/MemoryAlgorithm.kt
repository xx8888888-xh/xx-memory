package com.xxmemory.app.domain

/**
 * Memory curve algorithm interface.
 * All algorithms implement this interface for interchangeable use.
 */
interface MemoryAlgorithm {
    fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): EbbinghausAlgorithm.ScheduleResult

    val name: String
    val description: String
}