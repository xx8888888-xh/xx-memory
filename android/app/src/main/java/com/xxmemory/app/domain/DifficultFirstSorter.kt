package com.xxmemory.app.domain

import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.entity.ReviewLog

/**
 * 困难优先智能排序器。
 *
 * 认知科学依据：
 * - 间隔重复学习中，先复习困难项目能利用“测试效应”在认知资源最充沛时处理高负荷内容。
 * - 根据历史正确率、卡片内在难度和近期失败记录综合打分，动态调整复习顺序。
 *
 * 排序策略：
 * 1. 历史正确率越低 → 越优先。
 * 2. 卡片内在难度越高（FSRS difficulty / SM-2 easeFactor 推导）→ 越优先。
 * 3. 近期（7天内）失败次数越多 → 越优先。
 * 4. 全新卡片（无复习历史）默认中等优先级，避免被无限推后。
 */
object DifficultFirstSorter {

    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * 对到期卡片列表按困难程度降序排序。
     *
     * @param cards 待复习卡片（已通过到期时间筛选）
     * @param logs 全部复习日志，用于计算历史正确率和近期失败
     * @param algorithmType 当前记忆算法，决定如何解读卡片难度
     * @param now 当前时间戳，用于判断“近期”失败
     */
    fun sort(
        cards: List<Card>,
        logs: List<ReviewLog>,
        algorithmType: String,
        now: Long = System.currentTimeMillis()
    ): List<Card> {
        if (cards.size <= 1) return cards

        val logsByCard = logs.groupBy { it.cardId }
        val recentWindowStart = now - 7 * ONE_DAY_MS

        return cards.sortedByDescending { card ->
            difficultyScore(card, logsByCard[card.id].orEmpty(), algorithmType, recentWindowStart)
        }
    }

    /**
     * 计算单张卡片的困难分，范围约 [0, 1]，越高表示越困难、越应该优先复习。
     */
    fun difficultyScore(
        card: Card,
        logs: List<ReviewLog>,
        algorithmType: String,
        recentWindowStart: Long
    ): Double {
        val historyScore = historyFailureScore(logs)
        val intrinsicScore = intrinsicDifficultyScore(card, algorithmType)
        val recentFailures = recentFailureScore(logs, recentWindowStart)

        // 权重：历史表现 50%，内在难度 35%，近期失败 15%
        return historyScore * 0.5 +
                intrinsicScore * 0.35 +
                recentFailures * 0.15
    }

    /**
     * 根据复习历史计算失败比例。无历史记录时返回 0.5（中等优先级）。
     */
    private fun historyFailureScore(logs: List<ReviewLog>): Double {
        if (logs.isEmpty()) return 0.5
        val failures = logs.count { it.quality < 2 }
        return failures.toDouble() / logs.size
    }

    /**
     * 计算近期失败加成。每发生一次 quality < 2 的复习加 0.12，封顶 0.6。
     */
    private fun recentFailureScore(logs: List<ReviewLog>, recentWindowStart: Long): Double {
        val recentFailures = logs.count { it.reviewDate >= recentWindowStart && it.quality < 2 }
        return (recentFailures * 0.12).coerceAtMost(0.6)
    }

    /**
     * 计算卡片内在难度分，归一化到 [0, 1]。
     * - FSRS：difficulty 字段本身就在 [1, 10]，直接归一化。
     * - SM-2 / 艾宾浩斯：通过 easeFactor 反推，EF 越低越难。
     * - 全新卡片：返回 0.5。
     */
    private fun intrinsicDifficultyScore(card: Card, algorithmType: String): Double {
        val isFsrs = algorithmType == "FSRS"

        return if (isFsrs && card.difficulty > 0) {
            (card.difficulty / 10.0).coerceIn(0.0, 1.0)
        } else if (card.repetitions > 0) {
            // SM-2: EF 范围约 [1.3, 2.5]，越低越难
            val ef = card.easeFactor.coerceIn(1.3f, 2.5f)
            ((2.5f - ef) / 1.2f).toDouble().coerceIn(0.0, 1.0)
        } else {
            0.5
        }
    }
}
