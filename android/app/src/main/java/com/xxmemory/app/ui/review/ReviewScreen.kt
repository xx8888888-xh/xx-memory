package com.xxmemory.app.ui.review

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import java.util.Locale

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settings = remember { com.xxmemory.app.XxMemoryApplication.instance.settingsManager }
    val isEinkMode = settings.einkMode

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(tts) {
        tts.language = Locale.CHINESE
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val mediaPlayer = remember { MediaPlayer() }
    DisposableEffect(mediaPlayer) {
        onDispose {
            mediaPlayer.release()
        }
    }

    // Auto-play TTS when the card is flipped and the setting is enabled.
    LaunchedEffect(uiState.isFlipped, uiState.currentCard?.id) {
        if (uiState.isFlipped && settings.autoPlayAudio && ttsReady) {
            val text = uiState.currentCard?.answer?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    if (uiState.isLoading) {
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
        return
    }

    if (uiState.isComplete && uiState.cards.isEmpty()) {
        EmptyReviewState(onReload = { viewModel.loadDueCards() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
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
                        .fillMaxWidth(uiState.progress)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        } else {
            LinearProgressIndicator(
                progress = uiState.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${uiState.currentNumber} / ${uiState.totalCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isComplete) {
            ReviewCompleteState(
                completedCount = uiState.completedCount,
                onReload = { viewModel.loadDueCards() }
            )
        } else {
            val card = uiState.currentCard ?: return

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (card.subject.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isEinkMode) 0f else 0.1f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = card.subject,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier)
                }
                IconButton(
                    onClick = { viewModel.toggleFavorite(card) }
                ) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (card.isFavorite) "已收藏" else "收藏",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (card.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "标签: ${card.tags}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { viewModel.flipCard() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isEinkMode) 0.dp else 4.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!uiState.isFlipped) {
                            Text(
                                text = "点击翻转",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = card.question,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = card.question,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CardContent(
                                card = card,
                                mediaPlayer = mediaPlayer,
                                isEinkMode = isEinkMode
                            )
                            if (card.detail.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = card.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isFlipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .clickable {
                                val text = card.answer
                                if (ttsReady) {
                                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SpeakerNotes,
                            contentDescription = "朗读",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (uiState.isFlipped) {
                Text(
                    text = "你对这张卡片的掌握程度如何？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssessmentButton(
                        text = "忘记",
                        color = MaterialTheme.colorScheme.error,
                        isEinkMode = isEinkMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(0) }
                    )
                    AssessmentButton(
                        text = "困难",
                        color = MaterialTheme.colorScheme.secondary,
                        isEinkMode = isEinkMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(1) }
                    )
                    AssessmentButton(
                        text = "良好",
                        color = MaterialTheme.colorScheme.tertiary,
                        isEinkMode = isEinkMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(2) }
                    )
                    AssessmentButton(
                        text = "简单",
                        color = MaterialTheme.colorScheme.primary,
                        isEinkMode = isEinkMode,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.assessCard(3) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CardContent(
    card: CardEntity,
    mediaPlayer: MediaPlayer,
    isEinkMode: Boolean
) {
    val context = LocalContext.current

    when (card.cardType) {
        CardEntity.TYPE_FILL_BLANK -> {
            Text(
                text = card.answer.replace("___", "_________"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        CardEntity.TYPE_CODE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF1E1E1E),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isEinkMode) MaterialTheme.colorScheme.onSurface else Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        CardEntity.TYPE_IMAGE -> {
            val imageUrl = card.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = card.question,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "图片内容",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        CardEntity.TYPE_AUDIO -> {
            val audioUrl = card.audioUrl
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (!audioUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            try {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(context, android.net.Uri.parse(audioUrl))
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                            } catch (e: Exception) {
                                android.util.Log.e("ReviewScreen", "播放音频失败: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SpeakerNotes,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("播放音频")
                    }
                }
            }
        }
        else -> {
            Text(
                text = card.answer,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AssessmentButton(
    text: String,
    color: Color,
    isEinkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isEinkMode) Color.Black else color.copy(alpha = 0.15f)
    val contentColor = if (isEinkMode) Color.White else color
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyReviewState(onReload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "没有待复习的卡片",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "去导入页面添加新的卡片吧",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReload,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("刷新")
        }
    }
}

@Composable
private fun ReviewCompleteState(completedCount: Int, onReload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "太棒了！",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "已完成 $completedCount 张卡片的复习",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onReload,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("查看是否有新的卡片")
        }
    }
}
