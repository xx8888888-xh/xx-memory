package com.xxmemory.app.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxmemory.app.XxMemoryApplication
import com.xxmemory.app.data.AppDatabase
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.entity.ReviewLog
import com.xxmemory.app.data.repository.CardRepository
import com.xxmemory.app.domain.EbbinghausAlgorithm
import com.xxmemory.app.domain.SchedulerUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 支持的复习模式。
 * - FLASHCARD: 经典闪卡，翻面自评。
 * - BAICIZHAN: 看词选义，支持提示、押韵、斩熟词、拼写。
 * - BBDC: 不背单词式学习流（四选一 → 例句自评 → 独立回忆 → 常规复习）。
 */
enum class ReviewMode(val value: String) {
    FLASHCARD("flashcard"),
    BAICIZHAN("baicizhan"),
    BBDC("bbdc");

    companion object {
        fun from(value: String): ReviewMode = entries.find { it.value == value } ?: FLASHCARD
    }
}

/**
 * 复习步骤状态机。
 */
enum class ReviewStep {
    QUESTION,           // 闪卡/百词斩问题页
    RECALL,             // 不背单词：认识/不认识
    OPTIONS,            // 四选一验证释义
    EXAMPLE_REVIEW,     // 不背单词：例句自评（清晰 / 记错了）
    INDEPENDENT_RECALL, // 不背单词：独立回忆（无例句）
    SELF_ASSESSMENT,    // 自评：记对了 / 有点模糊 / 记不清
    DETAIL,             // 展示答案与详细解析
    SPELLING,           // 拼写测试（弹窗内）
    DICTATION,          // 默写：听音频写全文
    FILL_BLANK          // 填空：补全挖空
}

enum class SelfAssessment {
    CORRECT,    // 记对了 / 清晰
    FUZZY,      // 有点模糊
    FORGOT,     // 记不清
    WRONG       // 记错了
}

data class ReviewUiState(
    val cards: List<Card> = emptyList(),
    val currentIndex: Int = 0,
    val currentCard: Card? = null,
    val isFlipped: Boolean = false,
    val totalCount: Int = 0,
    val currentNumber: Int = 0,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val completedCount: Int = 0,
    val nextScheduleInfo: String? = null,

    // Mode-specific state
    val reviewMode: ReviewMode = ReviewMode.FLASHCARD,
    val step: ReviewStep = ReviewStep.QUESTION,
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val isCorrect: Boolean? = null,
    val showHint: Boolean = false,
    val showSpelling: Boolean = false,
    val spellingResult: SpellingResult? = null,

    // BBDC learning flow
    val selfAssessment: SelfAssessment? = null,
    val showAnswer: Boolean = false,
    val dictationInput: String = "",
    val dictationResult: SpellingResult? = null,
    val fillBlankInput: String = "",
    val fillBlankResult: SpellingResult? = null,
    val wrongAttempts: Int = 0  // BBDC 模式下当前卡片选错次数
)

sealed class SpellingResult {
    object Idle : SpellingResult()
    object Correct : SpellingResult()
    data class Wrong(val correctAnswer: String) : SpellingResult()
}

class ReviewViewModel : ViewModel() {
    private val repository: CardRepository
    private val settingsManager = XxMemoryApplication.instance.settingsManager

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val isAssessing = AtomicBoolean(false)
    private var allCardsForDistractors: List<Card> = emptyList()

    init {
        val db = AppDatabase.getInstance(XxMemoryApplication.instance)
        repository = CardRepository(db.cardDao(), db.reviewLogDao())
        loadDueCards()
    }

    fun loadDueCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val now = System.currentTimeMillis()
                val endOfDay = CardRepository.getEndOfDay(now)
                val cards = repository.getDueCardsList(endOfDay)

                if (cards.isEmpty()) {
                    _uiState.value = ReviewUiState(
                        isLoading = false,
                        isComplete = true,
                        reviewMode = ReviewMode.from(settingsManager.reviewMode)
                    )
                    return@launch
                }

                // Load distractor candidates once for option generation.
                // Use all unmastered cards (including current due queue) so that
                // newly imported cards can still borrow plausible distractors.
                allCardsForDistractors = repository.getAllCardsSync()
                    .filter { !it.mastered }

                // Apply daily card limit
                val limit = if (settingsManager.dailyCardLimitEnabled) {
                    settingsManager.dailyCardLimit.coerceAtLeast(1)
                } else {
                    Int.MAX_VALUE
                }
                val limitedCards = cards.take(limit)

                // Apply shuffle
                val finalCards = if (settingsManager.shuffleCards) limitedCards.shuffled() else limitedCards

                val firstCard = finalCards.firstOrNull()
                val mode = ReviewMode.from(settingsManager.reviewMode)
                val initialStep = initialStepForMode(mode, firstCard)

                _uiState.value = ReviewUiState(
                    cards = finalCards,
                    currentIndex = 0,
                    currentCard = firstCard,
                    isFlipped = false,
                    totalCount = finalCards.size,
                    currentNumber = 1,
                    isLoading = false,
                    reviewMode = mode,
                    step = initialStep,
                    options = if (initialStep == ReviewStep.OPTIONS) generateOptions(firstCard) else emptyList()
                )
            } catch (e: Exception) {
                Log.e("ReviewViewModel", "loadDueCards failed", e)
                _uiState.value = ReviewUiState(
                    isLoading = false,
                    isComplete = true,
                    reviewMode = ReviewMode.from(settingsManager.reviewMode)
                )
            }
        }
    }

    private fun initialStepForMode(mode: ReviewMode, card: Card?): ReviewStep {
        card ?: return ReviewStep.QUESTION
        return when (card.cardType) {
            Card.TYPE_POETRY -> ReviewStep.DICTATION
            Card.TYPE_VOCABULARY -> initialStepForVocabulary(card)
            else -> when (mode) {
                ReviewMode.FLASHCARD -> ReviewStep.QUESTION
                ReviewMode.BAICIZHAN -> ReviewStep.QUESTION
                ReviewMode.BBDC -> initialStepForBbdcCard(card)
            }
        }
    }

    private fun initialStepForVocabulary(card: Card): ReviewStep = when (card.learningStage) {
        Card.STAGE_NEW -> ReviewStep.OPTIONS
        Card.STAGE_OPTIONS_PASSED -> ReviewStep.EXAMPLE_REVIEW
        Card.STAGE_EXAMPLE_PASSED -> ReviewStep.INDEPENDENT_RECALL
        Card.STAGE_LEARNED -> ReviewStep.SELF_ASSESSMENT
        Card.STAGE_SPELLING_PASSED -> ReviewStep.SELF_ASSESSMENT
        else -> ReviewStep.OPTIONS
    }

    private fun initialStepForBbdcCard(card: Card?): ReviewStep {
        card ?: return ReviewStep.RECALL
        return when (card.cardType) {
            Card.TYPE_DICTATION -> ReviewStep.DICTATION
            Card.TYPE_FILL_BLANK -> ReviewStep.FILL_BLANK
            else -> when (card.learningStage) {
                Card.STAGE_NEW -> ReviewStep.RECALL
                Card.STAGE_OPTIONS_PASSED -> ReviewStep.EXAMPLE_REVIEW
                Card.STAGE_EXAMPLE_PASSED -> ReviewStep.INDEPENDENT_RECALL
                Card.STAGE_LEARNED -> ReviewStep.SELF_ASSESSMENT
                else -> ReviewStep.RECALL
            }
        }
    }

    /**
     * 在复习过程中切换模式，保持当前卡片，重置到新模式初始步骤。
     */
    fun switchReviewMode(mode: ReviewMode) {
        val state = _uiState.value
        if (state.reviewMode == mode || state.isLoading || state.isComplete) return

        settingsManager.reviewMode = mode.value
        val card = state.currentCard
        val newStep = initialStepForMode(mode, card)
        _uiState.value = state.copy(
            reviewMode = mode,
            step = newStep,
            isFlipped = false,
            options = if (newStep == ReviewStep.OPTIONS) generateOptions(card) else emptyList(),
            selectedOption = null,
            isCorrect = null,
            showHint = false,
            showSpelling = false,
            spellingResult = SpellingResult.Idle,
            selfAssessment = null,
            showAnswer = false,
            nextScheduleInfo = null,
            dictationInput = "",
            dictationResult = null,
            fillBlankInput = "",
            fillBlankResult = null,
            wrongAttempts = 0
        )
    }

    private fun generateOptions(card: Card?): List<String> {
        val correct = card?.answer?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()

        // 优先使用 AI 在导入时生成的干扰项
        val presetDistractors = card.distractors
            .split(",", "，", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != correct }
            .distinct()
            .shuffled()
            .take(3)

        val distractors = if (presetDistractors.size >= 3) {
            presetDistractors
        } else {
            val poolDistractors = allCardsForDistractors
                .filter { it.id != card.id }
                .map { it.answer.trim() }
                .filter { it.isNotBlank() && it != correct && it !in presetDistractors }
                .distinct()
                .shuffled()
                .take(3 - presetDistractors.size)
            presetDistractors + poolDistractors
        }

        val options = (distractors + correct).shuffled()
        return if (options.size < 4) {
            val placeholders = listOf("选项 A", "选项 B", "选项 C", "选项 D")
                .filter { it != correct && it !in distractors }
                .take(3 - distractors.size)
            (distractors + placeholders + correct).shuffled()
        } else options
    }

    /** 经典闪卡：翻面显示答案。 */
    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = true, step = ReviewStep.DETAIL)
    }

    /** 显示提示（百词斩/不背单词）。 */
    fun showHint() {
        _uiState.value = _uiState.value.copy(showHint = true)
    }

    /** 不背单词：用户选择"认识" -> 进入选项验证。 */
    fun onKnowCard() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        _uiState.value = state.copy(
            step = ReviewStep.OPTIONS,
            options = generateOptions(card),
            showHint = false,
            isCorrect = null,
            selectedOption = null
        )
    }

    /** 不背单词：用户选择"不认识" -> 直接显示详情并标记为错误。 */
    fun onDontKnowCard() {
        _uiState.value = _uiState.value.copy(
            step = ReviewStep.DETAIL,
            isCorrect = false,
            showHint = true,
            selectedOption = null,
            selfAssessment = SelfAssessment.WRONG,
            showAnswer = true
        )
    }

    /** 百词斩：用户选择"认识" -> 进入选项验证。 */
    fun onBaicizhanKnow() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        _uiState.value = state.copy(
            step = ReviewStep.OPTIONS,
            options = generateOptions(card),
            showHint = false,
            isCorrect = null,
            selectedOption = null
        )
    }

    /** 百词斩：用户选择"不认识" -> 直接显示详情，不计为答题错误。 */
    fun onBaicizhanShowDetail() {
        _uiState.value = _uiState.value.copy(
            step = ReviewStep.DETAIL,
            isCorrect = null,
            showHint = true,
            selectedOption = null
        )
    }

    /** 用户在选项阶段选择答案（百词斩/不背单词）。 */
    fun selectOption(option: String) {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val correct = card.answer.trim()
        val selectedCorrect = option.trim() == correct
        val isVocabulary = state.reviewMode == ReviewMode.BBDC || card.cardType == Card.TYPE_VOCABULARY

        _uiState.value = if (selectedCorrect && isVocabulary) {
            // 单词类型选对后进入例句自评（有例句）或独立回忆（无例句），不立即计分。
            // 注意：不重置 wrongAttempts，保留到 assessBbdcStage 用于 quality 降级。
            val hasExample = card.example.isNotBlank()
            state.copy(
                selectedOption = option,
                isCorrect = true,
                step = if (hasExample) ReviewStep.EXAMPLE_REVIEW else ReviewStep.INDEPENDENT_RECALL,
                showHint = false,
                selfAssessment = null,
                showAnswer = false,
                currentCard = card.copy(learningStage = Card.STAGE_OPTIONS_PASSED)
            )
        } else if (!selectedCorrect && isVocabulary) {
            // 单词类型选错：保持 OPTIONS 步骤，增加错误计数，不进入 DETAIL，允许重试
            state.copy(
                selectedOption = option,
                isCorrect = false,
                step = ReviewStep.OPTIONS,
                wrongAttempts = state.wrongAttempts + 1,
                showHint = true
            )
        } else {
            state.copy(
                selectedOption = option,
                isCorrect = selectedCorrect,
                step = ReviewStep.DETAIL,
                showHint = !selectedCorrect || state.showHint,
                selfAssessment = if (selectedCorrect) SelfAssessment.CORRECT else SelfAssessment.WRONG,
                showAnswer = true
            )
        }
    }

    /** 不背单词 Stage 1：例句自评。 */
    fun assessExampleReview(clear: Boolean) {
        val state = _uiState.value
        val card = state.currentCard ?: return
        if (clear) {
            // 清晰：进入独立回忆阶段，stage 1 -> 2（尚未计分）
            _uiState.value = state.copy(
                step = ReviewStep.INDEPENDENT_RECALL,
                currentCard = card.copy(learningStage = Card.STAGE_EXAMPLE_PASSED),
                selfAssessment = null,
                showAnswer = false
            )
        } else {
            // 记错了：保持 stage 1，显示答案并允许提交
            _uiState.value = state.copy(
                step = ReviewStep.DETAIL,
                selfAssessment = SelfAssessment.WRONG,
                showAnswer = true
            )
        }
    }

    /** 不背单词 Stage 2/3：开始自评。 */
    fun selectSelfAssessment(assessment: SelfAssessment) {
        _uiState.value = _uiState.value.copy(
            selfAssessment = assessment,
            step = ReviewStep.DETAIL,
            showAnswer = true
        )
    }

    /** 查看答案后更改自评。 */
    fun changeSelfAssessment(assessment: SelfAssessment) {
        _uiState.value = _uiState.value.copy(selfAssessment = assessment)
    }

    /** 提交 BBDC 自评结果。 */
    fun submitBbdcAssessment() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val assessment = state.selfAssessment ?: SelfAssessment.FUZZY
        val quality = when (assessment) {
            SelfAssessment.CORRECT -> 3
            SelfAssessment.FUZZY -> 2
            SelfAssessment.FORGOT -> 1
            SelfAssessment.WRONG -> 0
        }
        val nextStage = when (card.learningStage) {
            Card.STAGE_NEW -> if (assessment == SelfAssessment.CORRECT) Card.STAGE_OPTIONS_PASSED else Card.STAGE_NEW
            Card.STAGE_OPTIONS_PASSED -> if (assessment == SelfAssessment.CORRECT) Card.STAGE_EXAMPLE_PASSED else Card.STAGE_OPTIONS_PASSED
            Card.STAGE_EXAMPLE_PASSED -> if (assessment == SelfAssessment.CORRECT) Card.STAGE_LEARNED else Card.STAGE_OPTIONS_PASSED
            Card.STAGE_LEARNED -> Card.STAGE_LEARNED
            Card.STAGE_SPELLING_PASSED -> Card.STAGE_LEARNED
            else -> card.learningStage
        }
        assessBbdcStage(quality = quality, nextStage = nextStage)
    }

    private fun assessBbdcStage(quality: Int, nextStage: Int) {
        if (!isAssessing.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val card = state.currentCard ?: return@launch
                val learningStartedAt = if (card.learningStartedAt == 0L) System.currentTimeMillis() else card.learningStartedAt

                // BBDC OPTIONS 阶段选错重试：有错误尝试时降级 quality（最多 2）
                val effectiveQuality = if (state.wrongAttempts >= 1) quality.coerceAtMost(2) else quality

                val algorithm = EbbinghausAlgorithm.getAlgorithm(settingsManager.algorithmType)
                val result = algorithm.calculate(
                    quality = effectiveQuality,
                    repetitions = card.repetitions,
                    easeFactor = card.easeFactor,
                    currentInterval = card.interval
                )

                val nextReviewDate = applyStudyMode(result.nextReviewDate)

                if (nextStage == Card.STAGE_LEARNED && card.learningStage < Card.STAGE_LEARNED && card.cardType == Card.TYPE_VOCABULARY) {
                    // 单词类型首次学会：进入拼写检查
                    _uiState.value = state.copy(
                        step = ReviewStep.SPELLING,
                        showSpelling = true,
                        spellingResult = SpellingResult.Idle
                    )
                    return@launch
                }

                val updatedCard = card.copy(
                    repetitions = result.nextRepetitions,
                    easeFactor = result.nextEaseFactor,
                    difficulty = if (result.nextDifficulty > 0) result.nextDifficulty.toFloat() else card.difficulty,
                    interval = result.nextInterval,
                    nextReviewDate = nextReviewDate,
                    learningStage = nextStage,
                    learningStartedAt = learningStartedAt
                )
                repository.updateCard(updatedCard)

                val log = ReviewLog(
                    cardId = card.id,
                    quality = effectiveQuality,
                    reviewDate = System.currentTimeMillis(),
                    nextInterval = result.nextInterval
                )
                repository.insertReviewLog(log)

                moveToNext("下次复习: ${getIntervalText(result.nextInterval)}")
            } finally {
                isAssessing.set(false)
            }
        }
    }

    /**
     * 使用 SRS 算法评估卡片并进入下一张（闪卡/百词斩通用）。
     * @param quality 0-3，对应忘记/困难/良好/简单。
     */
    fun assessCard(quality: Int) {
        if (!isAssessing.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val card = state.currentCard ?: return@launch

                val algorithm = EbbinghausAlgorithm.getAlgorithm(settingsManager.algorithmType)
                val result = algorithm.calculate(
                    quality = quality,
                    repetitions = card.repetitions,
                    easeFactor = card.easeFactor,
                    currentInterval = card.interval
                )

                val nextReviewDate = applyStudyMode(result.nextReviewDate)

                val updatedCard = card.copy(
                    repetitions = result.nextRepetitions,
                    easeFactor = result.nextEaseFactor,
                    difficulty = if (result.nextDifficulty > 0) result.nextDifficulty.toFloat() else card.difficulty,
                    interval = result.nextInterval,
                    nextReviewDate = nextReviewDate
                )
                repository.updateCard(updatedCard)

                val log = ReviewLog(
                    cardId = card.id,
                    quality = quality,
                    reviewDate = System.currentTimeMillis(),
                    nextInterval = result.nextInterval
                )
                repository.insertReviewLog(log)

                moveToNext("下次复习: ${getIntervalText(result.nextInterval)}")
            } finally {
                isAssessing.set(false)
            }
        }
    }

    /**
     * 根据当前模式与选择结果推断 quality 并评估。
     */
    fun assessCurrentFromSelection() {
        val state = _uiState.value
        val quality = when {
            state.isCorrect == true -> 3
            state.selectedOption != null -> 1  // 选错
            state.step == ReviewStep.DETAIL && state.showHint -> 0  // 主动放弃/不认识
            else -> 1
        }
        assessCard(quality)
    }

    /** 斩熟词，标记为掌握并跳过后续复习。 */
    fun markMastered() {
        if (!isAssessing.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val card = state.currentCard ?: return@launch
                val updatedCard = card.copy(mastered = true, nextReviewDate = Long.MAX_VALUE)
                repository.updateCard(updatedCard)
                repository.insertReviewLog(
                    ReviewLog(
                        cardId = card.id,
                        quality = 3,
                        reviewDate = System.currentTimeMillis(),
                        nextInterval = 3650
                    )
                )
                moveToNext("已斩，不再复习")
            } finally {
                isAssessing.set(false)
            }
        }
    }

    /** 移动到下一题。 */
    private fun moveToNext(scheduleInfo: String?) {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.cards.size) {
            _uiState.value = state.copy(
                isComplete = true,
                completedCount = state.completedCount + 1,
                currentCard = null,
                isFlipped = false,
                nextScheduleInfo = null,
                step = ReviewStep.QUESTION,
                options = emptyList(),
                selectedOption = null,
                isCorrect = null,
                showHint = false,
                showSpelling = false,
                spellingResult = SpellingResult.Idle,
                selfAssessment = null,
                showAnswer = false,
                dictationInput = "",
                dictationResult = null,
                fillBlankInput = "",
                fillBlankResult = null,
                wrongAttempts = 0
            )
        } else {
            val nextCard = state.cards[nextIndex]
            val mode = ReviewMode.from(settingsManager.reviewMode)
            val nextStep = initialStepForMode(mode, nextCard)
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                currentCard = nextCard,
                isFlipped = false,
                currentNumber = nextIndex + 1,
                completedCount = state.completedCount + 1,
                nextScheduleInfo = scheduleInfo,
                step = nextStep,
                options = if (nextStep == ReviewStep.OPTIONS) generateOptions(nextCard) else emptyList(),
                selectedOption = null,
                isCorrect = null,
                showHint = false,
                showSpelling = false,
                spellingResult = SpellingResult.Idle,
                selfAssessment = null,
                showAnswer = false,
                dictationInput = "",
                dictationResult = null,
                fillBlankInput = "",
                fillBlankResult = null,
                wrongAttempts = 0
            )
        }
    }

    private fun getIntervalText(interval: Int): String = when {
        interval < 1 -> "今天"
        interval == 1 -> "明天"
        interval < 7 -> "${interval}天后"
        interval < 30 -> "${interval / 7}周后"
        interval < 365 -> "${interval / 30}个月后"
        else -> "${interval / 365}年后"
    }

    /**
     * 预览当前卡片在指定 quality 下的下次复习间隔（天）。
     */
    fun previewSchedule(quality: Int): Int? {
        val card = _uiState.value.currentCard ?: return null
        val algorithm = EbbinghausAlgorithm.getAlgorithm(settingsManager.algorithmType)
        return try {
            algorithm.calculate(
                quality = quality.coerceIn(0, 3),
                repetitions = card.repetitions,
                easeFactor = card.easeFactor,
                currentInterval = card.interval
            ).nextInterval
        } catch (e: Exception) {
            Log.w("ReviewViewModel", "previewSchedule failed", e)
            null
        }
    }

    /** 推断当前状态直接评估会使用的 quality。 */
    fun inferCurrentQuality(): Int {
        val state = _uiState.value
        return when {
            state.isCorrect == true -> 3
            state.selectedOption != null -> 1
            state.step == ReviewStep.DETAIL && state.showHint -> 0
            else -> 1
        }
    }

    /** 开始拼写测试。 */
    fun startSpellingTest() {
        _uiState.value = _uiState.value.copy(
            showSpelling = true,
            spellingResult = SpellingResult.Idle
        )
    }

    /** 检查拼写输入。拼写对话框要求根据释义拼写 question，因此以 question 为正确答案。 */
    fun checkSpelling(input: String) {
        val card = _uiState.value.currentCard ?: return
        val correct = card.question.trim()
        val normalizedInput = input.trim()
        val isCorrect = normalizedInput.equals(correct, ignoreCase = true)
        _uiState.value = _uiState.value.copy(
            spellingResult = if (isCorrect) SpellingResult.Correct else SpellingResult.Wrong(correct)
        )
    }

    /** 完成拼写测试并评估。 */
    fun finishSpelling() {
        val wasCorrect = _uiState.value.spellingResult is SpellingResult.Correct
        val state = _uiState.value
        val card = state.currentCard ?: return
        if (wasCorrect) {
            _uiState.value = state.copy(
                showSpelling = false,
                currentCard = card.copy(learningStage = Card.STAGE_LEARNED)
            )
            assessBbdcStage(quality = 3, nextStage = Card.STAGE_LEARNED)
        } else {
            // 拼写错误，回到 OPTIONS 重学
            _uiState.value = state.copy(
                showSpelling = false,
                step = ReviewStep.OPTIONS,
                options = generateOptions(card),
                selectedOption = null,
                isCorrect = null,
                selfAssessment = null,
                showAnswer = false,
                currentCard = card.copy(learningStage = Card.STAGE_NEW),
                wrongAttempts = state.wrongAttempts + 1
            )
        }
    }

    fun cancelSpelling() {
        val state = _uiState.value
        val card = state.currentCard
        if (state.step == ReviewStep.SPELLING && card?.cardType == Card.TYPE_VOCABULARY) {
            // 单词类型拼写检查不可跳过，取消后回到自评步骤
            _uiState.value = state.copy(
                showSpelling = false,
                spellingResult = SpellingResult.Idle,
                step = ReviewStep.SELF_ASSESSMENT,
                showAnswer = true
            )
        } else {
            _uiState.value = state.copy(showSpelling = false, spellingResult = SpellingResult.Idle)
        }
    }

    fun toggleFavorite(card: Card) {
        viewModelScope.launch {
            repository.toggleFavorite(card.id)
            _uiState.value = _uiState.value.copy(
                currentCard = card.copy(isFavorite = !card.isFavorite)
            )
        }
    }

    // --- Dictation & Fill Blank ---

    fun updateDictationInput(input: String) {
        _uiState.value = _uiState.value.copy(dictationInput = input)
    }

    fun checkDictation() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val input = state.dictationInput.trim()
        val correct = card.answer.trim()
        val isCorrect = normalizeText(input) == normalizeText(correct)
        _uiState.value = state.copy(
            dictationResult = if (isCorrect) SpellingResult.Correct else SpellingResult.Wrong(correct)
        )
    }

    fun finishDictation() {
        val wasCorrect = _uiState.value.dictationResult is SpellingResult.Correct
        val quality = if (wasCorrect) 3 else 0
        val card = _uiState.value.currentCard ?: return
        val nextStage = if (card.learningStage < Card.STAGE_LEARNED && wasCorrect) {
            (card.learningStage + 1).coerceAtMost(Card.STAGE_LEARNED)
        } else card.learningStage
        assessBbdcStage(quality = quality, nextStage = nextStage)
    }

    /** 跳过古诗文朗诵阶段，直接进入默写。 */
    fun skipRecitation() {
        val card = _uiState.value.currentCard ?: return
        if (card.cardType != Card.TYPE_POETRY) return
        _uiState.value = _uiState.value.copy(
            step = ReviewStep.DICTATION
        )
    }

    fun updateFillBlankInput(input: String) {
        _uiState.value = _uiState.value.copy(fillBlankInput = input)
    }

    fun checkFillBlank() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val input = state.fillBlankInput.trim()
        val correct = card.answer.trim()
        val isCorrect = normalizeText(input) == normalizeText(correct)
        _uiState.value = state.copy(
            fillBlankResult = if (isCorrect) SpellingResult.Correct else SpellingResult.Wrong(correct)
        )
    }

    fun finishFillBlank() {
        val wasCorrect = _uiState.value.fillBlankResult is SpellingResult.Correct
        val quality = if (wasCorrect) 3 else 0
        val card = _uiState.value.currentCard ?: return
        val nextStage = if (card.learningStage < Card.STAGE_LEARNED && wasCorrect) {
            (card.learningStage + 1).coerceAtMost(Card.STAGE_LEARNED)
        } else card.learningStage
        assessBbdcStage(quality = quality, nextStage = nextStage)
    }

    private fun normalizeText(text: String): String {
        return text.replace("，", ",").replace("。", ".").replace(" ", "").lowercase()
    }

    private fun applyStudyMode(nextDayTimestamp: Long): Long {
        return if (settingsManager.studyMode == "focused") {
            val slots = SchedulerUtils.parseFocusedSlots(settingsManager.focusedTimeSlots)
            SchedulerUtils.adjustToFocusedSlot(nextDayTimestamp, slots)
        } else {
            nextDayTimestamp
        }
    }
}
