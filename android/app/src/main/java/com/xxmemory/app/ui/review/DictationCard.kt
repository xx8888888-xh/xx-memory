package com.xxmemory.app.ui.review

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xxmemory.app.data.entity.Card as CardEntity

@Composable
internal fun DictationCard(
    card: CardEntity,
    input: String,
    result: SpellingResult?,
    isEinkMode: Boolean,
    tts: TextToSpeech,
    ttsReady: Boolean,
    poetryRecitationEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onPlayAudio: () -> Unit,
    onCheck: () -> Unit,
    onFinish: () -> Unit,
    onSkipRecitation: (() -> Unit)? = null
) {
    val isPoetry = card.cardType == CardEntity.TYPE_POETRY
    // 非古诗默写卡直接进入输入；古诗文在朗诵关闭时也直接输入
    var showInput by remember(card.id) { mutableStateOf(!isPoetry || !poetryRecitationEnabled) }

    LaunchedEffect(card.id) {
        if (isPoetry && !showInput && ttsReady && poetryRecitationEnabled) {
            // 不再自动 TTS 朗诵；仅当用户上传了音频时才自动播放
            val url = card.audioUrl
            if (!url.isNullOrBlank()) {
                onPlayAudio()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEinkMode) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                TypeLabel(cardType = card.cardType, isEinkMode = isEinkMode)
                Spacer(modifier = Modifier.height(16.dp))

                if (isPoetry && !showInput) {
                    // 古诗文朗诵阶段
                    Text(
                        text = "古诗文默写",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "先听一遍朗诵，再开始默写",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (card.audioUrl?.isNotBlank() == true) {
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "听朗诵",
                                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击听朗诵",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Button(
                        onClick = { showInput = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("开始默写", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            showInput = true
                            onSkipRecitation?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("跳过朗诵")
                    }
                } else {
                    Text(
                        text = "听音频，写出正确内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isEinkMode) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                    ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "播放音频",
                                tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击播放",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                if (!isPoetry || showInput) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isPoetry && card.audioUrl?.isNotBlank() == true) {
                            OutlinedButton(
                                onClick = onPlayAudio,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("重新播放")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = input,
                            onValueChange = onInputChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("输入答案") },
                            enabled = result !is SpellingResult.Correct,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                                )
                            }
                        )

                        when (result) {
                            is SpellingResult.Correct -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "回答正确",
                                        color = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            is SpellingResult.Wrong -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "正确答案：${result.correctAnswer}",
                                        color = if (isEinkMode) Color.Gray else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (result) {
                            is SpellingResult.Correct, is SpellingResult.Wrong -> {
                                Button(
                                    onClick = onFinish,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("继续", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = onCheck,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = input.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEinkMode) Color.DarkGray else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("检查", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
