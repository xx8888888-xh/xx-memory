package com.xxmemory.app.ui.review

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xxmemory.app.data.entity.Card as CardEntity
import com.xxmemory.app.domain.AudioPlayer
import com.xxmemory.app.ui.theme.EinkFilterChip
import com.xxmemory.app.ui.theme.rememberEinkMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settings = remember { com.xxmemory.app.XxMemoryApplication.instance.settingsManager }
    val isEinkMode = rememberEinkMode()
    val isBaicizhanDeepMode = remember { settings.baicizhanDeepMode }
    val isBbdcImmersiveMode = remember { settings.bbdcImmersiveMode }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }
    val audioPlayer = remember { AudioPlayer(context) }
    DisposableEffect(tts, audioPlayer) {
        val result = tts.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.CHINESE
        }
        onDispose {
            tts.stop()
            tts.shutdown()
            audioPlayer.release()
        }
    }

    // Auto-play audio when a card question appears or the answer is revealed.
    LaunchedEffect(uiState.step, uiState.currentCard?.id) {
        val card = uiState.currentCard ?: return@LaunchedEffect
        when (uiState.step) {
            ReviewStep.DETAIL -> {
                if (settings.ttsAutoPlayAnswer) {
                    audioPlayer.play(card.audioUrl, card.answer)
                }
            }
            ReviewStep.QUESTION,
            ReviewStep.RECALL,
            ReviewStep.OPTIONS,
            ReviewStep.EXAMPLE_REVIEW,
            ReviewStep.INDEPENDENT_RECALL,
            ReviewStep.SELF_ASSESSMENT,
            ReviewStep.DICTATION,
            ReviewStep.FILL_BLANK -> {
                if (settings.ttsAutoPlayQuestion) {
                    audioPlayer.play(card.audioUrl, card.question)
                }
            }
            ReviewStep.SPELLING -> {}
        }
    }

    // Auto-play answer audio when dictation / fill-blank result is revealed.
    LaunchedEffect(uiState.dictationResult, uiState.fillBlankResult, uiState.currentCard?.id) {
        if (!settings.ttsAutoPlayAnswer) return@LaunchedEffect
        val card = uiState.currentCard ?: return@LaunchedEffect
        if (uiState.dictationResult != null || uiState.fillBlankResult != null) {
            audioPlayer.play(card.audioUrl, card.answer)
        }
    }

    if (uiState.showSpelling) {
        SpellingDialog(
            card = uiState.currentCard,
            spellingResult = uiState.spellingResult,
            isEinkMode = isEinkMode,
            onCheck = { viewModel.checkSpelling(it) },
            onFinish = { viewModel.finishSpelling() },
            onCancel = { viewModel.cancelSpelling() }
        )
    }

    if (uiState.isLoading) {
        LoadingState(isEinkMode = isEinkMode)
        return
    }

    if (uiState.isComplete && uiState.cards.isEmpty()) {
        EmptyReviewState(onReload = { viewModel.loadDueCards() })
        return
    }

    val isImmersive = uiState.reviewMode == ReviewMode.BBDC && isBbdcImmersiveMode
    // 百词斩深度模式减少图片依赖，仅在详情页展示图片。
    val showImageInQuestion = !(uiState.reviewMode == ReviewMode.BAICIZHAN && isBaicizhanDeepMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Progress bar
        ReviewProgressBar(progress = uiState.progress, isEinkMode = isEinkMode)
        Spacer(modifier = Modifier.height(12.dp))

        // Mode switcher
        if (!isImmersive) {
            ModeSwitcher(
                currentMode = uiState.reviewMode,
                onModeChange = { viewModel.switchReviewMode(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.currentNumber} / ${uiState.totalCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isImmersive) {
                ModeLabel(mode = uiState.reviewMode, isEinkMode = isEinkMode)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isComplete) {
            ReviewCompleteState(
                completedCount = uiState.completedCount,
                onReload = { viewModel.loadDueCards() }
            )
        } else {
            val card = uiState.currentCard ?: return

            val currentCardId = card.id
            val flashcardPreviews = remember(currentCardId, settings.algorithmType) {
                (0..3).map { viewModel.previewSchedule(it) }
            }
            val selectionPreview = remember(
                currentCardId,
                uiState.step,
                uiState.isCorrect,
                uiState.selectedOption,
                uiState.showHint,
                settings.algorithmType
            ) {
                viewModel.previewSchedule(viewModel.inferCurrentQuality())
            }

            if (!isImmersive) {
                CardMetaRow(
                    card = card,
                    isEinkMode = isEinkMode,
                    onToggleFavorite = { viewModel.toggleFavorite(card) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main content area with swipe gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(uiState.currentCard?.id, uiState.reviewMode) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            when {
                                dragAmount < -120f -> viewModel.toggleFavorite(card)
                                dragAmount > 120f -> viewModel.markMastered()
                            }
                        }
                    }
            ) {
                when (uiState.step) {
                    ReviewStep.RECALL -> if (isImmersive) {
                        BbdcImmersiveRecallCard(
                            card = card,
                            isEinkMode = isEinkMode,
                            tts = tts,
                            ttsReady = ttsReady,
                            onKnow = { viewModel.onKnowCard() },
                            onDontKnow = { viewModel.onDontKnowCard() }
                        )
                    } else {
                        BbdcRecallCard(
                            card = card,
                            isEinkMode = isEinkMode,
                            tts = tts,
                            ttsReady = ttsReady,
                            onKnow = { viewModel.onKnowCard() },
                            onDontKnow = { viewModel.onDontKnowCard() }
                        )
                    }

                    ReviewStep.QUESTION -> QuestionCard(
                        card = card,
                        isEinkMode = isEinkMode,
                        showImage = showImageInQuestion,
                        tts = tts,
                        ttsReady = ttsReady,
                        onReveal = {
                            when (uiState.reviewMode) {
                                ReviewMode.BAICIZHAN -> viewModel.onBaicizhanShowDetail()
                                else -> viewModel.flipCard()
                            }
                        }
                    )

                    ReviewStep.OPTIONS -> OptionsCard(
                        card = card,
                        options = uiState.options,
                        selectedOption = uiState.selectedOption,
                        isEinkMode = isEinkMode,
                        showImage = showImageInQuestion,
                        tts = tts,
                        ttsReady = ttsReady,
                        onSelect = { viewModel.selectOption(it) }
                    )

                    ReviewStep.EXAMPLE_REVIEW,
                    ReviewStep.INDEPENDENT_RECALL,
                    ReviewStep.SELF_ASSESSMENT -> BbdcLearningCard(
                        card = card,
                        step = uiState.step,
                        isEinkMode = isEinkMode,
                        tts = tts,
                        ttsReady = ttsReady,
                        onExampleClear = { viewModel.assessExampleReview(clear = true) },
                        onExampleWrong = { viewModel.assessExampleReview(clear = false) },
                        onSelfAssessment = { viewModel.selectSelfAssessment(it) }
                    )

                    ReviewStep.DETAIL -> DetailCard(
                        card = card,
                        isCorrect = uiState.isCorrect,
                        showHint = uiState.showHint,
                        isEinkMode = isEinkMode,
                        isDeepMode = isBaicizhanDeepMode || uiState.reviewMode == ReviewMode.BBDC,
                        reviewMode = uiState.reviewMode,
                        tts = tts,
                        ttsReady = ttsReady
                    )

                    ReviewStep.DICTATION -> DictationCard(
                        card = card,
                        input = uiState.dictationInput,
                        result = uiState.dictationResult,
                        isEinkMode = isEinkMode,
                        tts = tts,
                        ttsReady = ttsReady,
                        onInputChange = { viewModel.updateDictationInput(it) },
                        onPlayAudio = { audioPlayer.play(card.audioUrl, card.question) },
                        onCheck = { viewModel.checkDictation() },
                        onFinish = { viewModel.finishDictation() }
                    )

                    ReviewStep.FILL_BLANK -> FillBlankCard(
                        card = card,
                        input = uiState.fillBlankInput,
                        result = uiState.fillBlankResult,
                        isEinkMode = isEinkMode,
                        tts = tts,
                        ttsReady = ttsReady,
                        onInputChange = { viewModel.updateFillBlankInput(it) },
                        onCheck = { viewModel.checkFillBlank() },
                        onFinish = { viewModel.finishFillBlank() }
                    )

                    ReviewStep.SPELLING -> {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom action area
            BottomActionArea(
                uiState = uiState,
                isEinkMode = isEinkMode,
                isImmersive = isImmersive,
                flashcardPreviews = flashcardPreviews,
                selectionPreview = selectionPreview,
                onReveal = { viewModel.flipCard() },
                onShowHint = { viewModel.showHint() },
                onStartSpelling = { viewModel.startSpellingTest() },
                onAssess = { quality -> viewModel.assessCard(quality) },
                onAssessSelection = { viewModel.assessCurrentFromSelection() },
                onMarkMastered = { viewModel.markMastered() },
                onBaicizhanKnow = { viewModel.onBaicizhanKnow() },
                onBaicizhanShowDetail = { viewModel.onBaicizhanShowDetail() },
                onChangeSelfAssessment = { viewModel.changeSelfAssessment(it) },
                onSubmitBbdcAssessment = { viewModel.submitBbdcAssessment() }
            )

            uiState.nextScheduleInfo?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun SpeakButton(
    text: String,
    tts: TextToSpeech,
    ttsReady: Boolean,
    isEinkMode: Boolean
) {
    if (!ttsReady || text.isBlank()) return
    IconButton(
        onClick = { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
    ) {
        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = "朗读",
            tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun scheduleSubtext(interval: Int?): String = interval?.let {
    when {
        it < 1 -> "今天"
        it == 1 -> "明天"
        it < 7 -> "${it}天后"
        it < 30 -> "${it / 7}周后"
        it < 365 -> "${it / 30}个月后"
        else -> "${it / 365}年后"
    }
} ?: ""

@Composable
internal fun TypeLabel(cardType: String, isEinkMode: Boolean) {
    val label = when (cardType) {
        CardEntity.TYPE_FILL_BLANK -> "填空"
        CardEntity.TYPE_CODE -> "代码"
        CardEntity.TYPE_IMAGE -> "图片"
        CardEntity.TYPE_AUDIO -> "音频"
        else -> null
    } ?: return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CodeBlock(text: String) {
    // Defensive: some import paths may store literal \n instead of actual newlines.
    val displayText = text.replace("\\n", "\n")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun AudioButton(
    audioUrl: String?,
    isEinkMode: Boolean
) {
    if (audioUrl.isNullOrBlank()) return
    val context = LocalContext.current
    IconButton(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(audioUrl)))
            } catch (_: Exception) {
                // Ignore if no player available
            }
        }
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "播放音频",
            tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ReviewProgressBar(progress: Float, isEinkMode: Boolean) {
    if (isEinkMode) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
    } else {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ModeSwitcher(
    currentMode: ReviewMode,
    onModeChange: (ReviewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            ReviewMode.FLASHCARD to "闪卡",
            ReviewMode.BAICIZHAN to "百词斩",
            ReviewMode.BBDC to "不背"
        ).forEach { (mode, label) ->
            val selected = currentMode == mode
            EinkFilterChip(
                selected = selected,
                onClick = { onModeChange(mode) },
                label = label,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeLabel(mode: ReviewMode, isEinkMode: Boolean) {
    val label = when (mode) {
        ReviewMode.FLASHCARD -> "闪卡模式"
        ReviewMode.BAICIZHAN -> "百词斩模式"
        ReviewMode.BBDC -> "不背单词模式"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CardMetaRow(
    card: CardEntity,
    isEinkMode: Boolean,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (card.subject.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = card.subject,
                        color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (card.tags.isNotBlank()) {
                Text(
                    text = card.tags.split(",").take(3).joinToString(" · ") { it.trim() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (card.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (card.isFavorite) "已收藏" else "收藏",
                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuestionCard(
    card: CardEntity,
    isEinkMode: Boolean,
    showImage: Boolean,
    tts: TextToSpeech,
    ttsReady: Boolean,
    onReveal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onReveal() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeLabel(cardType = card.cardType, isEinkMode = isEinkMode)
                    Row {
                        AudioButton(
                            audioUrl = card.audioUrl,
                            isEinkMode = isEinkMode
                        )
                        SpeakButton(
                            text = card.question,
                            tts = tts,
                            ttsReady = ttsReady,
                            isEinkMode = isEinkMode
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (showImage && !card.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = card.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                if (card.cardType == CardEntity.TYPE_CODE) {
                    CodeBlock(text = card.question)
                } else {
                    Text(
                        text = card.question,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                if (card.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = card.phonetic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "点击看答案",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BbdcRecallCard(
    card: CardEntity,
    isEinkMode: Boolean,
    tts: TextToSpeech,
    ttsReady: Boolean,
    onKnow: () -> Unit,
    onDontKnow: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeLabel(cardType = card.cardType, isEinkMode = isEinkMode)
                        Row {
                            AudioButton(
                                audioUrl = card.audioUrl,
                                isEinkMode = isEinkMode
                            )
                            SpeakButton(
                                text = card.question,
                                tts = tts,
                                ttsReady = ttsReady,
                                isEinkMode = isEinkMode
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!card.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = card.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    if (card.cardType == CardEntity.TYPE_CODE) {
                        CodeBlock(text = card.question)
                    } else {
                        Text(
                            text = card.question,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (card.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = card.phonetic,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDontKnow,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.error
                )
            ) {
                Text("不认识", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Button(
                onClick = onKnow,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("认识", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun BbdcImmersiveRecallCard(
    card: CardEntity,
    isEinkMode: Boolean,
    tts: TextToSpeech,
    ttsReady: Boolean,
    onKnow: () -> Unit,
    onDontKnow: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeLabel(cardType = card.cardType, isEinkMode = isEinkMode)
            Row {
                AudioButton(
                    audioUrl = card.audioUrl,
                    isEinkMode = isEinkMode
                )
                SpeakButton(
                    text = card.question,
                    tts = tts,
                    ttsReady = ttsReady,
                    isEinkMode = isEinkMode
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!card.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (card.cardType == CardEntity.TYPE_CODE) {
                CodeBlock(text = card.question)
            } else {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            if (card.phonetic.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = card.phonetic,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDontKnow,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.error
                )
            ) {
                Text("不认识", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
            Button(
                onClick = onKnow,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("认识", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun OptionsCard(
    card: CardEntity,
    options: List<String>,
    selectedOption: String?,
    isEinkMode: Boolean,
    showImage: Boolean,
    tts: TextToSpeech,
    ttsReady: Boolean,
    onSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeLabel(cardType = card.cardType, isEinkMode = isEinkMode)
                    Row {
                        AudioButton(
                            audioUrl = card.audioUrl,
                            isEinkMode = isEinkMode
                        )
                        SpeakButton(
                            text = card.question,
                            tts = tts,
                            ttsReady = ttsReady,
                            isEinkMode = isEinkMode
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (showImage && !card.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = card.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (card.cardType == CardEntity.TYPE_CODE) {
                    CodeBlock(text = card.question)
                } else {
                    Text(
                        text = card.question,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                if (card.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请选择正确答案",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    val isSelected = selectedOption == option
                    val isAnswerCorrect = option.trim() == card.answer.trim()
                    val borderColor = when {
                        isSelected && isAnswerCorrect ->
                            if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        isSelected && !isAnswerCorrect ->
                            if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val bgColor = when {
                        isSelected && isAnswerCorrect ->
                            if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        isSelected && !isAnswerCorrect ->
                            if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.background
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(
                                width = 1.5.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(option) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(borderColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = if (isAnswerCorrect) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = borderColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailCard(
    card: CardEntity,
    isCorrect: Boolean?,
    showHint: Boolean,
    isEinkMode: Boolean,
    isDeepMode: Boolean,
    reviewMode: ReviewMode,
    tts: TextToSpeech,
    ttsReady: Boolean
) {
    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // Result indicator
            isCorrect?.let { correct ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (correct) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (correct) {
                            if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        } else {
                            if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (correct) "回答正确" else "回答错误",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (correct) {
                            if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        } else {
                            if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Question + answer header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.question,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (card.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (ttsReady) {
                            tts.speak(card.answer, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "朗读",
                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )

            // Answer
            Text(
                text = card.answer,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            // BBDC: prioritize real-context example sentence right after answer.
            if (reviewMode == ReviewMode.BBDC && card.example.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.SpeakerNotes,
                    title = "真实语境例句",
                    content = card.example,
                    isEinkMode = isEinkMode
                )
            }

            // Hint (Baicizhan deep mode / when user asked for help)
            if (showHint && card.hint.isNotBlank() && isDeepMode) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.Lightbulb,
                    title = "提示",
                    content = card.hint,
                    isEinkMode = isEinkMode
                )
            }

            // Detail
            if (card.detail.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = card.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Example sentence (for non-BBDC modes)
            if (reviewMode != ReviewMode.BBDC && card.example.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.SpeakerNotes,
                    title = "例句",
                    content = card.example,
                    isEinkMode = isEinkMode
                )
            }

            // Collocations
            if (card.collocations.isNotBlank() && isDeepMode) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.School,
                    title = "词组搭配",
                    content = card.collocations,
                    isEinkMode = isEinkMode
                )
            }

            // Etymology
            if (card.etymology.isNotBlank() && isDeepMode) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.HelpOutline,
                    title = "词根词缀",
                    content = card.etymology,
                    isEinkMode = isEinkMode
                )
            }

            // Rhyme hint
            if (card.rhyme.isNotBlank() && isDeepMode) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.Edit,
                    title = "押韵 / 联想",
                    content = card.rhyme,
                    isEinkMode = isEinkMode
                )
            }

            // Derivatives
            if (card.derivatives.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(
                    icon = Icons.Filled.AutoStories,
                    title = "派生词",
                    content = card.derivatives,
                    isEinkMode = isEinkMode
                )
            }

            // Image (always shown on detail page to reinforce memory)
            if (!card.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    isEinkMode: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isEinkMode) 0.5f else 0.6f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BottomActionArea(
    uiState: ReviewUiState,
    isEinkMode: Boolean,
    isImmersive: Boolean,
    flashcardPreviews: List<Int?>,
    selectionPreview: Int?,
    onReveal: () -> Unit,
    onShowHint: () -> Unit,
    onStartSpelling: () -> Unit,
    onAssess: (Int) -> Unit,
    onAssessSelection: () -> Unit,
    onMarkMastered: () -> Unit,
    onBaicizhanKnow: () -> Unit,
    onBaicizhanShowDetail: () -> Unit,
    onChangeSelfAssessment: (SelfAssessment) -> Unit,
    onSubmitBbdcAssessment: () -> Unit
) {
    when (uiState.step) {
        ReviewStep.QUESTION -> {
            if (uiState.reviewMode == ReviewMode.BAICIZHAN) {
                // 百词斩：认识 -> 选项验证；不认识 -> 详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBaicizhanShowDetail,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("不认识", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Button(
                        onClick = onBaicizhanKnow,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("认识", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            } else {
                // 闪卡：看答案（仅翻面，不评分）
                Button(
                    onClick = onReveal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("看答案", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }

        ReviewStep.OPTIONS -> {
            if (uiState.reviewMode == ReviewMode.BAICIZHAN && !uiState.showHint) {
                OutlinedButton(
                    onClick = onShowHint,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("提示一下")
                }
            }
        }

        ReviewStep.DETAIL -> {
            when (uiState.reviewMode) {
                ReviewMode.FLASHCARD -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssessmentButton(
                                text = "忘记",
                                subtext = scheduleSubtext(flashcardPreviews.getOrNull(0)),
                                isEinkMode = isEinkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onAssess(0) }
                            )
                            AssessmentButton(
                                text = "困难",
                                subtext = scheduleSubtext(flashcardPreviews.getOrNull(1)),
                                isEinkMode = isEinkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onAssess(1) }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssessmentButton(
                                text = "良好",
                                subtext = scheduleSubtext(flashcardPreviews.getOrNull(2)),
                                isEinkMode = isEinkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onAssess(2) }
                            )
                            AssessmentButton(
                                text = "简单",
                                subtext = scheduleSubtext(flashcardPreviews.getOrNull(3)),
                                isEinkMode = isEinkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onAssess(3) }
                            )
                        }
                    }
                }

                ReviewMode.BAICIZHAN -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionIconButton(
                                icon = Icons.Filled.Create,
                                label = "拼写测试",
                                onClick = onStartSpelling,
                                isEinkMode = isEinkMode,
                                modifier = Modifier.weight(1f)
                            )
                            ActionIconButton(
                                icon = Icons.Filled.Check,
                                label = "斩熟词",
                                onClick = onMarkMastered,
                                isEinkMode = isEinkMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        AssessmentButton(
                            text = "继续学习",
                            subtext = scheduleSubtext(selectionPreview),
                            isEinkMode = isEinkMode,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onAssessSelection() }
                        )
                    }
                }

                ReviewMode.BBDC -> {
                    val showSelfAssessment = uiState.selfAssessment != null || uiState.isCorrect == false
                    if (showSelfAssessment) {
                        val currentAssessment = uiState.selfAssessment ?: SelfAssessment.WRONG
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BbdcSelfAssessmentChip(
                                    text = "记对了",
                                    selected = currentAssessment == SelfAssessment.CORRECT,
                                    isEinkMode = isEinkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onChangeSelfAssessment(SelfAssessment.CORRECT) }
                                )
                                BbdcSelfAssessmentChip(
                                    text = "有点模糊",
                                    selected = currentAssessment == SelfAssessment.FUZZY,
                                    isEinkMode = isEinkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onChangeSelfAssessment(SelfAssessment.FUZZY) }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BbdcSelfAssessmentChip(
                                    text = "记不清",
                                    selected = currentAssessment == SelfAssessment.FORGOT,
                                    isEinkMode = isEinkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onChangeSelfAssessment(SelfAssessment.FORGOT) }
                                )
                                BbdcSelfAssessmentChip(
                                    text = "记错了",
                                    selected = currentAssessment == SelfAssessment.WRONG,
                                    isEinkMode = isEinkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onChangeSelfAssessment(SelfAssessment.WRONG) }
                                )
                            }
                            AssessmentButton(
                                text = "提交",
                                subtext = scheduleSubtext(selectionPreview),
                                isEinkMode = isEinkMode,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (uiState.selfAssessment == null) {
                                        onChangeSelfAssessment(SelfAssessment.WRONG)
                                    }
                                    onSubmitBbdcAssessment()
                                }
                            )
                        }
                    } else {
                        if (isImmersive) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionIconButton(
                                    icon = Icons.Filled.Create,
                                    label = "随手拼",
                                    onClick = onStartSpelling,
                                    isEinkMode = isEinkMode,
                                    modifier = Modifier.weight(1f)
                                )
                                AssessmentButton(
                                    text = "下一个",
                                    subtext = scheduleSubtext(selectionPreview),
                                    isEinkMode = isEinkMode,
                                    modifier = Modifier.weight(2f),
                                    onClick = { onAssessSelection() }
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionIconButton(
                                        icon = Icons.Filled.Create,
                                        label = "随手拼",
                                        onClick = onStartSpelling,
                                        isEinkMode = isEinkMode,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AssessmentButton(
                                        text = "下一个",
                                        subtext = scheduleSubtext(selectionPreview),
                                        isEinkMode = isEinkMode,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onAssessSelection() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        else -> {}
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isEinkMode: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BbdcSelfAssessmentChip(
    text: String,
    selected: Boolean,
    isEinkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
    val containerColor = if (selected) {
        if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) contentColor else MaterialTheme.colorScheme.outline

    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AssessmentButton(
    text: String,
    subtext: String,
    isEinkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isEinkMode) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.DarkGray
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtext.isNotBlank()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtext.isNotBlank()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellingDialog(
    card: CardEntity?,
    spellingResult: SpellingResult?,
    isEinkMode: Boolean,
    onCheck: (String) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val result = spellingResult ?: SpellingResult.Idle

    AlertDialog(
        onDismissRequest = { if (result !is SpellingResult.Correct) onCancel() },
        title = { Text("拼写测试", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "请根据释义拼写：${card?.question ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (card?.phonetic?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("输入答案") },
                    enabled = result !is SpellingResult.Correct
                )
                when (result) {
                    is SpellingResult.Correct -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("拼写正确！", color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary)
                        }
                    }
                    is SpellingResult.Wrong -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "正确答案是：${result.correctAnswer}",
                                color = if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (result) {
                is SpellingResult.Correct -> {
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("继续")
                    }
                }
                is SpellingResult.Wrong -> {
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("继续")
                    }
                }
                else -> {
                    Button(
                        onClick = { onCheck(input) },
                        enabled = input.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("检查")
                    }
                }
            }
        },
        dismissButton = {
            if (result !is SpellingResult.Correct) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
private fun LoadingState(isEinkMode: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isEinkMode) {
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyReviewState(onReload: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "今日复习已完成",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "暂时没有需要复习的卡片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onReload,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.School, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("再来一组")
            }
        }
    }
}

@Composable
private fun ReviewCompleteState(
    completedCount: Int,
    onReload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "复习完成！",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本次完成 $completedCount 张卡片",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onReload,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.School, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("再来一组")
            }
        }
    }
}
