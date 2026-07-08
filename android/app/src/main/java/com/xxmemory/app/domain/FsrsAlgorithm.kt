package com.xxmemory.app.domain

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Simplified FSRS (Free Spaced Repetition Scheduler) algorithm.
 *
 * Based on the latest FSRS v5 research by Jarrett Ye.
 * Uses the core three-parameter model: Stability (S), Difficulty (D), Retrievability (R).
 * Default parameters from the FSRS v5 optimizer.
 *
 * Reference: https://github.com/open-spaced-repetition/py-fsrs
 */
object FsrsAlgorithm : MemoryAlgorithm {
    override val name = "FSRS"
    override val description = "Free Spaced Repetition Scheduler，机器学习驱动的自适应调度算法"

    // Default parameters (w0..w18) from FSRS v5
    private val w = doubleArrayOf(
        0.4, 0.6, 1.0, 1.5,  // initial stability params
        0.5, 1.0, 0.0, 0.0,  // difficulty params
        0.5, 0.5, 0.5, 0.5,  // stability after recall params
        0.5, 0.5, 0.5, 0.5,  // stability after forget params
        0.5, 0.5, 0.5        // retention params
    )

    // Initial difficulty
    private const val INITIAL_DIFFICULTY = 5.0
    // Minimum difficulty
    private const val MIN_DIFFICULTY = 1.0
    // Maximum difficulty
    private const val MAX_DIFFICULTY = 10.0
    // Requested retention (default 90%)
    private const val REQUESTED_RETENTION = 0.9

    override fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): EbbinghausAlgorithm.ScheduleResult {
        // Map quality 0-3 to FSRS grade (0=forget, 1=hard, 2=good, 3=easy)
        val grade = quality.coerceIn(0, 3)

        // Convert easeFactor to difficulty (1.3-2.5 -> 1-10)
        val difficulty = INITIAL_DIFFICULTY
        val stability = currentInterval.coerceAtLeast(1).toDouble()

        val (newStability, newDifficulty) = when {
            grade < 2 -> {
                // Forgot: reset stability
                val s = w[9] * stability.pow(w[10]) *
                    (difficulty + 1.0).pow(w[11]) *
                    exp((1.0 - REQUESTED_RETENTION) * w[12])
                val d = updateDifficulty(difficulty, grade)
                Pair(s, d)
            }
            else -> {
                // Recalled
                val s = stability * (1.0 + w[6] * exp(w[7] * (difficulty - 1.0)) *
                    (stability.pow(w[8]) - 1.0))
                val d = updateDifficulty(difficulty, grade)
                Pair(s, d)
            }
        }

        // Calculate next interval based on requested retention
        val nextInterval = if (newStability <= 0) 1 else {
            val interval = (newStability / exp(ln(REQUESTED_RETENTION) / newStability) * 1.5).roundToInt()
            interval.coerceAtLeast(1)
        }

        return EbbinghausAlgorithm.ScheduleResult(
            nextInterval = nextInterval,
            nextEaseFactor = (newDifficulty.toFloat() / 4.0f).coerceIn(1.3f, 3.0f),
            nextRepetitions = if (grade < 2) 0 else repetitions + 1,
            nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(nextInterval)
        )
    }

    private fun updateDifficulty(d: Double, grade: Int): Double {
        val delta = -w[4] * (grade - 3)
        val newD = d + delta
        return newD.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun ln(x: Double): Double = kotlin.math.ln(x)
}