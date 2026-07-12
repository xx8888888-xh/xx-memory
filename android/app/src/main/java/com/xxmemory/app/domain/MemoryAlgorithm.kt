package com.xxmemory.app.domain

import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Memory curve algorithm interface.
 * All algorithms implement this interface for interchangeable use.
 */
interface MemoryAlgorithm {
    data class ScheduleResult(
        val nextInterval: Int,
        val nextEaseFactor: Float,
        val nextRepetitions: Int,
        val nextReviewDate: Long,
        /** FSRS difficulty [1,10]; 0 means N/A (SM2/Ebbinghaus). */
        val nextDifficulty: Double = 0.0
    )

    fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): ScheduleResult

    val name: String
    val description: String
}

/**
 * SM-2 Algorithm - SuperMemo 2
 *
 * The classic spaced repetition algorithm developed by Piotr Woźniak in 1987.
 * Uses an Ease Factor (EF) to dynamically adjust review intervals based on user performance.
 * Quality ratings affect the EF: higher quality increases EF, lower quality decreases EF.
 *
 * Reference:
 * - Woźniak, P. A., & Gorzelanczyk, E. J. (1994). Optimization of spacing in
 *   practical learning and the biological basis of memory.
 * - https://super-memory.com/english/ol/sm2.htm
 * - https://en.wikipedia.org/wiki/SuperMemo#Description_of_SM-2_algorithm
 */
object SM2Algorithm : MemoryAlgorithm {
    override val name = "SM-2"
    override val description = "经典间隔重复算法，由 Piotr Woźniak 于1987年提出。使用难度因子(EF)动态调整复习间隔，是 Anki、Mnemosyne 等软件的基础算法。参考：https://super-memory.com/english/ol/sm2.htm"

    private const val INITIAL_EASE_FACTOR = 2.5f
    private const val MIN_EASE_FACTOR = 1.3f

    override fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): MemoryAlgorithm.ScheduleResult {
        val clampedQuality = quality.coerceIn(0, 3)
        val mappedQuality = mapQualityToSm2(clampedQuality)

        if (mappedQuality < 2) {
            return MemoryAlgorithm.ScheduleResult(
                nextInterval = 1,
                nextEaseFactor = easeFactor.coerceAtLeast(MIN_EASE_FACTOR),
                nextRepetitions = 0,
                nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(1)
            )
        }

        val newEaseFactor = calculateEaseFactor(easeFactor, mappedQuality)

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
                nextInterval = (currentInterval * newEaseFactor).roundToInt()
                nextRepetitions = repetitions + 1
            }
        }

        return MemoryAlgorithm.ScheduleResult(
            nextInterval = nextInterval.coerceAtLeast(1),
            nextEaseFactor = newEaseFactor,
            nextRepetitions = nextRepetitions,
            nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(nextInterval)
        )
    }

    /**
     * 将应用层的 4 级评分映射到 SM-2 的 0-5 质量分。
     * 0=忘记 对应 SM-2 0（完全不会），1=困难 对应 3（艰难回忆但正确），
     * 2=良好 对应 4（犹豫但正确），3=简单 对应 5（完美）。
     * 这样“困难”不会被视为错误，仅降低 EF 并继续推进间隔。
     */
    private fun mapQualityToSm2(quality: Int): Int = when (quality) {
        0 -> 0
        1 -> 3
        2 -> 4
        3 -> 5
        else -> 3
    }

    private fun calculateEaseFactor(currentEf: Float, quality: Int): Float {
        val newEf = currentEf + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        return newEf.coerceAtLeast(MIN_EASE_FACTOR)
    }
}

/**
 * Ebbinghaus Fixed Interval Algorithm
 *
 * Based on Hermann Ebbinghaus's forgetting curve research (1885).
 * Uses fixed, scientifically-proven intervals for optimal memory retention.
 * The intervals are derived from Ebbinghaus's experiments on memory decay.
 *
 * Intervals: 1 day, 2 days, 4 days, 7 days, 15 days, 30 days, 60 days, 180 days
 *
 * Reference:
 * - Ebbinghaus, H. (1885). Über das Gedächtnis [On Memory]
 * - https://en.wikipedia.org/wiki/Forgetting_curve
 * - https://en.wikipedia.org/wiki/Ebbinghaus_illusion#Memory_research
 */
object EbbinghausFixedAlgorithm : MemoryAlgorithm {
    override val name = "艾宾浩斯固定"
    override val description = "基于 Hermann Ebbinghaus 1885年遗忘曲线研究的固定间隔复习法。采用科学验证的间隔序列：1天→2天→4天→7天→15天→30天→60天→180天，逐步巩固记忆。参考：https://en.wikipedia.org/wiki/Forgetting_curve"

    private val fixedIntervals = intArrayOf(1, 2, 4, 7, 15, 30, 60, 180)

    override fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): MemoryAlgorithm.ScheduleResult {
        val clampedQuality = quality.coerceIn(0, 3)

        if (clampedQuality == 0) {
            return MemoryAlgorithm.ScheduleResult(
                nextInterval = fixedIntervals[0],
                nextEaseFactor = easeFactor,
                nextRepetitions = 0,
                nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(fixedIntervals[0])
            )
        }

        val nextIndex = repetitions.coerceAtMost(fixedIntervals.size - 1)
        val nextInterval = fixedIntervals[nextIndex]

        return MemoryAlgorithm.ScheduleResult(
            nextInterval = nextInterval,
            nextEaseFactor = easeFactor,
            nextRepetitions = repetitions + 1,
            nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(nextInterval)
        )
    }
}

/**
 * FSRS v4 Algorithm - Free Spaced Repetition Scheduler
 *
 * A modern spaced repetition algorithm developed by Jarrett Ye (open-spaced-repetition).
 * Uses a three-component model: Stability (S), Difficulty (D), and Retrievability (R).
 *
 * Core concepts:
 * - Stability (S): Memory strength, measured in days (interval when retrievability = 90%)
 * - Difficulty (D): How hard the item is, range [1, 10]
 * - Retrievability (R): Probability of recalling the item at time t, range [0, 1]
 *
 * Key formulas (FSRS v4):
 * - Retrievability: R(t, S) = (1 + t / (9S))^(-1)
 * - Interval: I = S * 9 * (1 / desiredRetention - 1)
 * - Initial stability: S0(G) = w[G-1] for G in {1,2,3,4}
 * - Initial difficulty: D0(G) = w[4] + w[5] * (G - 3)
 * - Difficulty update: D_new = D + w[6] * (G - 3), clamped to [1, 10]
 * - Stability after recall: S_new = S * (1 + exp(w[7]) * (11 - D) * S^(-w[8]) * (exp((1-R)*w[9]) - 1))
 * - Stability after forget: S_new = w[10] * D^(-w[11]) * ((S + 1)^w[12] - 1)
 *
 * Reference:
 * - https://github.com/open-spaced-repetition/fsrs4anki/wiki/FSRS4Anki-Algorithm
 * - https://github.com/open-spaced-repetition/py-fsrs
 * - Jarrett Ye. (2023). FSRS: A Modern Spaced Repetition Scheduler
 */
object FsrsAlgorithm : MemoryAlgorithm {
    override val name = "FSRS"
    override val description = "Free Spaced Repetition Scheduler，由 Jarrett Ye 开发的现代间隔重复算法。采用机器学习驱动的三参数模型（稳定性S、难度D、可提取性R），比SM-2更准确地预测记忆保持率。默认保持率90%。参考：https://github.com/open-spaced-repetition/fsrs4anki/wiki"

    private const val DESIRED_RETENTION = 0.9

    private const val MIN_DIFFICULTY = 1.0
    private const val MAX_DIFFICULTY = 10.0

    private val w = doubleArrayOf(
        0.4, 0.6, 1.0, 1.5,
        4.5, 0.5, -0.5,
        0.5, 0.3, 1.2,
        0.2, 1.5, 0.5
    )

    override fun calculate(
        quality: Int,
        repetitions: Int,
        easeFactor: Float,
        currentInterval: Int
    ): MemoryAlgorithm.ScheduleResult {
        val grade = quality.coerceIn(0, 3) + 1

        val isNew = repetitions == 0 || currentInterval == 0

        val currentDifficulty = if (isNew) {
            initialDifficulty(grade)
        } else {
            easeFactorToDifficulty(easeFactor)
        }

        val currentStability = if (isNew) {
            initialStability(grade)
        } else {
            currentInterval.coerceAtLeast(1).toDouble()
        }

        val retrievability = if (isNew) {
            1.0
        } else {
            retrievability(currentInterval.toDouble(), currentStability)
        }

        val newDifficulty = nextDifficulty(currentDifficulty, grade)

        val newStability = when {
            grade <= 1 -> nextStabilityAfterForget(currentStability, currentDifficulty)
            else -> nextStabilityAfterRecall(currentStability, currentDifficulty, retrievability, grade)
        }

        val nextInterval = nextInterval(newStability, DESIRED_RETENTION)

        val newRepetitions = if (grade <= 1) 0 else repetitions + 1
        val newEaseFactor = difficultyToEaseFactor(newDifficulty)

        return MemoryAlgorithm.ScheduleResult(
            nextInterval = nextInterval,
            nextEaseFactor = newEaseFactor,
            nextRepetitions = newRepetitions,
            nextReviewDate = EbbinghausAlgorithm.getNextDayTimestamp(nextInterval),
            nextDifficulty = newDifficulty
        )
    }

    private fun initialStability(grade: Int): Double {
        val g = grade.coerceIn(1, 4)
        return w[g - 1]
    }

    private fun initialDifficulty(grade: Int): Double {
        val d = w[4] + w[5] * (grade - 3)
        return d.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun retrievability(elapsedDays: Double, stability: Double): Double {
        if (stability <= 0) return 0.0
        return 1.0 / (1.0 + elapsedDays / (9.0 * stability))
    }

    private fun nextDifficulty(d: Double, grade: Int): Double {
        val newD = d + w[6] * (grade - 3)
        return newD.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun nextStabilityAfterRecall(
        stability: Double,
        difficulty: Double,
        retrievability: Double,
        grade: Int
    ): Double {
        val baseFactor = 1.0 + exp(w[7]) * (11.0 - difficulty) *
                Math.pow(stability, -w[8]) *
                (exp((1.0 - retrievability) * w[9]) - 1.0)

        val gradeFactor = when (grade) {
            2 -> 0.8
            4 -> 1.3
            else -> 1.0
        }

        return (stability * baseFactor * gradeFactor).coerceAtLeast(0.1)
    }

    private fun nextStabilityAfterForget(stability: Double, difficulty: Double): Double {
        val s = w[10] * Math.pow(difficulty, -w[11]) *
                (Math.pow(stability + 1.0, w[12]) - 1.0)
        return s.coerceAtLeast(0.1)
    }

    private fun nextInterval(stability: Double, desiredRetention: Double): Int {
        if (stability <= 0) return 1
        val interval = (stability * 9.0 * (1.0 / desiredRetention - 1.0)).roundToInt()
        return interval.coerceAtLeast(1)
    }

    private fun easeFactorToDifficulty(ef: Float): Double {
        val efClamped = ef.coerceIn(1.3f, 3.0f)
        return ((efClamped - 1.3f) / (3.0f - 1.3f) * (MAX_DIFFICULTY - MIN_DIFFICULTY) + MIN_DIFFICULTY).toDouble()
    }

    private fun difficultyToEaseFactor(d: Double): Float {
        val dClamped = d.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        return ((dClamped - MIN_DIFFICULTY) / (MAX_DIFFICULTY - MIN_DIFFICULTY) * (3.0f - 1.3f) + 1.3f).toFloat()
    }
}
